package org.jin.calenee

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.jin.calenee.databinding.ActivityConnectionInputBinding

class ConnectionInputActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityConnectionInputBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)
    }
}