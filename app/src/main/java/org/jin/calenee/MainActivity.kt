package org.jin.calenee

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import org.jin.calenee.databinding.ActivityMainBinding
import org.jin.calenee.login.LoginActivity
import org.jin.calenee.login.LoginViewModel

class MainActivity : AppCompatActivity() {
    companion object {
        internal val viewModel: LoginViewModel = LoginViewModel()

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)


        supportFragmentManager.beginTransaction()
            .add(binding.mainFrameLayout.id, CalendarFragment()).commit()

        listener()
    }

    private fun listener() {
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.calendarMenu -> {
                    Log.d("menu_test", "add calendar menu")
                }

                R.id.logoutMenu -> {
                    viewModel.signOut()
                    Snackbar.make(binding.root, "로그아웃 되었습니다.", Snackbar.LENGTH_SHORT).show()
                    Log.d("menu_test", "logout")

                    Intent(this, LoginActivity::class.java).apply {
                        startActivity(this)
                        slideLeft()
                        finish()
                    }
                }

                else -> {}
            }

            true
        }

        binding.mainBottomNavigationView.setOnItemSelectedListener { item ->
            replaceFragment(
                when(item.itemId) {
                    R.id.calendar -> CalendarFragment()
                    R.id.memo -> MemoFragment()
                    else -> TodoFragment()
                }
            )

            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(binding.mainFrameLayout.id, fragment).commit()
    }
}