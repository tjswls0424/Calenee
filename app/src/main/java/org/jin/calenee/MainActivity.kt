package org.jin.calenee

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.jin.calenee.chat.ChattingActivity
import org.jin.calenee.databinding.ActivityMainBinding
import org.jin.calenee.home.CoupleInfoViewModel
import org.jin.calenee.home.EditCoupleInfoActivity
import org.jin.calenee.home.HomeFragment
import org.jin.calenee.login.LoginActivity
import org.jin.calenee.login.LoginActivity.Companion.viewModel
import org.jin.calenee.util.NetworkStatusHelper

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

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        coupleInfoObserver()
        listener()
        initFragment()
        checkNetworkStatus()
    }

    private fun initFragment() {
        HomeFragment().apply {
            supportFragmentManager.beginTransaction()
                .add(binding.mainFrameLayout.id, this).commit()
        }
    }

    private fun listener() {
        // top right menu
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.edit_couple_info -> {
                    val coupleInfo = coupleInfoViewModel.getCoupleInfo().apply {
                        position =
                            if (coupleInfoViewModel.nickname1.value == App.userPrefs.getString("current_nickname")) 1 else 2
                    }

                    Intent(this, EditCoupleInfoActivity::class.java).apply {
                        putExtra("coupleInfo", coupleInfo)
                        startActivity(this)
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
            with(coupleInfoViewModel) {
                updateNickname(value?.get("user1Nickname").toString(), 1)
                updateBirthday(value?.get("user1Birthday").toString(), 1)
                updateMessage(value?.get("user1Message").toString(), 1)
                updateNickname(value?.get("user2Nickname").toString(), 2)
                updateBirthday(value?.get("user2Birthday").toString(), 2)
                updateMessage(value?.get("user2Message").toString(), 2)
                updateDays(value?.get("firstMetDate").toString())
            }

            // refresh fragment
            replaceFragment(HomeFragment())
            Log.d("home_test", "refresh fragment")

            val position = when {
                (value?.get("user1Email") == firebaseAuth.currentUser?.email) -> 1
                (value?.get("user2Email") == firebaseAuth.currentUser?.email) -> 2
                else -> 0
            }

            // save to SP
            with(App.userPrefs) {
                when (position) {
                    1 -> {
                        updateUserNickname(value?.get("user1Nickname").toString(), true)
                        updateUserNickname(value?.get("user2Nickname").toString(), false)

                        updateUserBirthday(value?.get("user1Birthday").toString(), true)
                        updateUserBirthday(value?.get("user2Birthday").toString(), false)
                    }

                    2 -> {
                        updateUserNickname(value?.get("user2Nickname").toString(), true)
                        updateUserNickname(value?.get("user1Nickname").toString(), false)

                        updateUserBirthday(value?.get("user2Birthday").toString(), true)
                        updateUserBirthday(value?.get("user1Birthday").toString(), false)
                    }
                }

                updateFirstMetDate(value?.get("firstMetDate").toString())
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

    private fun logout() {
        val googleSignInOptions: GoogleSignInOptions by lazy {
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        }

        val googleSignInClient by lazy {
            GoogleSignIn.getClient(this, googleSignInOptions)
        }

        googleSignInClient.signOut().addOnCompleteListener {
            viewModel.signOut()
        }

        // set SP
        App.userPrefs.apply {
            setString("login_status", "false")
            setString("current_email", "")
            setString("current_nickname", "")
            setString("current_birthday", "")
            setString("current_partner_email", "")
            setString("current_partner_nickname", "")
            setString("current_nickname_birthday", "")
        }

        Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()

        Intent(this, LoginActivity::class.java).apply {
            startActivity(this)
            slideLeft()
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_top_menu, menu)
        return true
    }

    override fun onBackPressed() {}
}