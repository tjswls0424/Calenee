package org.jin.calenee.login

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import org.jin.calenee.databinding.ActivityLoginActivtyBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginActivtyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginActivtyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.googleLoginBtn.setOnClickListener {
            Log.d("btn_test", "google")
        }

        binding.caleneeLoginBtn.setOnClickListener {
            Log.d("btn_test", "calenee")
        }




    }
}