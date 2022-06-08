package org.jin.calenee

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import org.jin.calenee.databinding.ActivityMainBinding
import org.jin.calenee.login.LoginActivity
import org.jin.calenee.login.LoginActivity.Companion.viewModel

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        supportFragmentManager.beginTransaction()
            .add(binding.mainFrameLayout.id, HomeFragment()).commit()

        listener()
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
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(binding.mainFrameLayout.id, fragment)
            .commit()
    }

    override fun onBackPressed() {}
}