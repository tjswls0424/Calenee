package org.jin.calenee

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.jin.calenee.chat.ChattingActivity
import org.jin.calenee.databinding.ActivityMainBinding
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

    private lateinit var binding: ActivityMainBinding

    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
    private val firestore by lazy {
        Firebase.firestore
    }


    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        supportFragmentManager.beginTransaction()
            .add(binding.mainFrameLayout.id, HomeFragment()).commit()

        // initialize couple document ID
        // todo: 추후 login activity로 이동 (앱 재설치시 커플 연결 때 저장했던 ID local DB에 재저장)
        firestore.collection("user").document(App.userPrefs.getString("current_email")).get()
            .addOnSuccessListener {
                App.userPrefs.setString("couple_chat_id", it["coupleChatID"].toString())
                Log.d("pref_test", App.userPrefs.getString("couple_chat_id"))
            }

        listener()
        checkNetworkStatus()
    }

    private fun listener() {
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.logoutMenu -> {
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
                        setString("current_email", "")
                        setString("current_name", "")
                        setString("login_status", "false")
                    }

                    Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()

                    Intent(this, LoginActivity::class.java).apply {
                        startActivity(this)
                        slideLeft()
                        finish()
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
    }

    private fun replaceFragment(fragment: Fragment) {
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
                Toast.makeText(this, "현재 인터넷이 연결되어 있지 않습니다. 인터넷을 연결해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {}
}