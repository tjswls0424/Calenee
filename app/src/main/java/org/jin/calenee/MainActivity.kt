package org.jin.calenee

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.jin.calenee.chat.ChattingActivity
import org.jin.calenee.databinding.ActivityMainBinding
import org.jin.calenee.home.CoupleInfoViewModel
import org.jin.calenee.home.EditCoupleInfoActivity
import org.jin.calenee.home.HomeFragment
import org.jin.calenee.util.NetworkStatusHelper

const val NOTIFICATION = 10

class MainActivity : AppCompatActivity() {
    companion object {
        fun Activity.slideLeft() {
            overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_exit)
        }

        fun Activity.slideRight() {
            overridePendingTransition(R.anim.slide_right_enter, R.anim.slide_right_exit)
        }

        fun Activity.slideUp() {
            overridePendingTransition(R.anim.slide_up_enter, R.anim.slide_up_exit)
        }

        fun Activity.slideDown() {
            overridePendingTransition(R.anim.slide_down_enter, R.anim.slide_down_exit)
        }
    }

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val coupleInfoViewModel by lazy {
        ViewModelProvider(this)[CoupleInfoViewModel::class.java]
    }

    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
    private val firestore by lazy {
        Firebase.firestore
    }

    private val coupleInfoDoc by lazy {
        firestore.collection("coupleInfo").document(App.userPrefs.getString("couple_chat_id"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        checkPermission()
        coupleInfoObserver()
        listener()
        initFragment()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkNetworkStatus()
        }
}

    private fun checkPermission() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (!notificationManager.areNotificationsEnabled()
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            val permission = listOf(Manifest.permission.POST_NOTIFICATIONS)
            val denied = permission.count {
                ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    it
                ) == PackageManager.PERMISSION_DENIED
            }
            if (denied > 0) {
                requestPermissions(permission.toTypedArray(), NOTIFICATION)
            }
        } else {
            println("Main: notification are not enabled")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode) {
            NOTIFICATION -> {
                // Manifest.permission.POST_NOTIFICATIONS
                if (grantResults.first() == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this@MainActivity, "알림 권한을 허용하지 않을 경우 채팅 서비스가 원활하지 않을 수 있습니다.\n" +
                            "[설정] > [권한] 에서 권한을 허용해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initFragment(): Fragment {
        return HomeFragment().apply {
            supportFragmentManager.beginTransaction()
                .add(binding.mainFrameLayout.id, this).commit()
        }
    }

    private fun listener() {
        // top right menu
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.edit_couple_info -> {
                    lifecycleScope.launch {
                        val coupleInfo = coupleInfoViewModel.getCoupleInfo().apply {
                            position = getMyPosition()
                        }

                        Intent(this@MainActivity, EditCoupleInfoActivity::class.java).apply {
                            putExtra("coupleInfo", coupleInfo)
                            startActivity(this)
                        }
                    }
                }
            }

            true
        }

        binding.mainBottomNavigationView.setOnItemSelectedListener { item ->
            replaceFragment(
                when (item.itemId) {
                    R.id.main_home -> HomeFragment()
                    R.id.diary -> DiaryFragment()
                    R.id.calendar -> CalendarFragment()
                    else -> MenuFragment()
                }
            )

            true
        }

        binding.chatBtn.setOnClickListener {
            Intent(this@MainActivity, ChattingActivity::class.java).also {
                startActivity(it)
                slideUp()
            }
        }

        // Firestore - collection for coupleInfo listener
        coupleInfoDoc.addSnapshotListener { value, error ->
//            value?.toObject(CoupleInfoSync::class.java).run {
//                coupleInfoViewModel.run {
//                    updateNickname(user1)
//                }
//            }
//            with (value?.toObject(CoupleInfoSync::class.java)) {
//                updateNickname(value?.get("user1Nickname").toString(), 1)
//
//            }

            // update coupleInfoViewModel
            with(coupleInfoViewModel) {
                updateNickname(value?.get("user1Nickname").toString(), 1, value?.get("user1Email").toString())
                updateBirthday(value?.get("user1Birthday").toString(), 1)
                updateMessage(value?.get("user1Message").toString(), 1)
                updateNickname(value?.get("user2Nickname").toString(), 2, value?.get("user2Email").toString())
                updateBirthday(value?.get("user2Birthday").toString(), 2)
                updateMessage(value?.get("user2Message").toString(), 2)
                updateDays(value?.get("firstMetDate").toString())
            }

            // refresh fragment
            kotlin.runCatching {
                replaceFragment(HomeFragment())
            }.onFailure {
                it.printStackTrace()
            }

            // (SP) update nickname, birthday for 2 users
            lifecycleScope.launch {
                with(App.userPrefs) {
                    val isMyPosition = getMyPosition() == 1
                    when (getString("current_position").toInt()) {
                        1 -> {
                            updateUserNickname(value?.get("user1Nickname").toString(), isMyPosition)
                            updateUserBirthday(value?.get("user1Birthday").toString(), isMyPosition)

                            updateUserNickname(value?.get("user2Nickname").toString(), !isMyPosition)
                            updateUserBirthday(value?.get("user2Birthday").toString(), !isMyPosition)
                        }

                        2 -> {
                            updateUserNickname(value?.get("user2Nickname").toString(), !isMyPosition)
                            updateUserBirthday(value?.get("user2Birthday").toString(), !isMyPosition)

                            updateUserNickname(value?.get("user1Nickname").toString(), isMyPosition)
                            updateUserBirthday(value?.get("user1Birthday").toString(), isMyPosition)
                        }
                    }

                    updateFirstMetDate(value?.get("firstMetDate").toString())
                }
            }
        }
    }

    private fun coupleInfoObserver() {
        coupleInfoViewModel.nickname1.observe(this) {
            Log.d("observer_test/1", it)
        }
        coupleInfoViewModel.nickname2.observe(this) {
            Log.d("observer_test/2", it)
        }
        coupleInfoViewModel.days.observe(this) {
            Log.d("observer_test/days", it)
        }
    }

    private suspend fun getMyPosition(): Int {
        return kotlin.runCatching {
            flowOf(App.userPrefs.getString("current_position").toInt())
        }.recoverCatching {
            callbackFlow {
                firestore.collection("user")
                    .document(firebaseAuth.currentUser?.email.toString())
                    .get().addOnSuccessListener { doc ->
                        App.userPrefs.setString("current_position", doc["position"].toString())
                        trySend(doc["position"].toString().toInt())
                    }

                awaitClose()
            }
        }.fold(
            onSuccess = { it.first() },
            onFailure = { -1 }
        )
    }

    private fun replaceFragment(fragment: Fragment, bundle: Bundle? = null) {
        fragment.arguments = bundle
        supportFragmentManager.beginTransaction().replace(binding.mainFrameLayout.id, fragment)
            .commit()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun checkNetworkStatus() {
        NetworkStatusHelper(applicationContext).observe(this) { isConnected ->
            if (isConnected) {
                binding.loadingScreenView.visibility = View.INVISIBLE
            } else {
                binding.loadingScreenView.visibility = View.VISIBLE
                Toast.makeText(this, "현재 인터넷이 연결되어 있지 않습니다. 인터넷을 연결해주세요.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_top_menu, menu)
        return true
    }

    override fun onBackPressed() {}
}