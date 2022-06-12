package org.jin.calenee

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.jin.calenee.databinding.ActivityConnectionInputBinding

class ConnectionInputActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityConnectionInputBinding.inflate(layoutInflater)
    }

    private var gender: String = "female"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        listener()
    }

    private fun listener() {
        binding.genderRadioGroup.setOnCheckedChangeListener { radioGroup, id ->
            when (id) {
                R.id.female_radio_btn -> gender = "female"
                R.id.male_radio_btn -> gender = "male"
            }
        }

    }
}