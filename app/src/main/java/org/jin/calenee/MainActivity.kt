package org.jin.calenee

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import org.jin.calenee.databinding.ActivityMainBinding

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)


        supportFragmentManager.beginTransaction()
            .add(binding.mainFrameLayout.id, CalendarFragment()).commit()

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