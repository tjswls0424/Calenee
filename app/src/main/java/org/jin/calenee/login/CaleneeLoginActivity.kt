package org.jin.calenee.login

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.jin.calenee.MainActivity.Companion.slideLeft
import org.jin.calenee.MainActivity.Companion.slideRight
import org.jin.calenee.MainActivity.Companion.slideUp
import org.jin.calenee.R
import org.jin.calenee.databinding.ActivityCaleneeLoginBinding

class CaleneeLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCaleneeLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCaleneeLoginBinding.inflate(layoutInflater)

        setContentView(binding.root)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        slideRight()
    }
}