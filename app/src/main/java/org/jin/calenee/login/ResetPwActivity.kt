package org.jin.calenee.login

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.animation.TranslateAnimation
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import org.jin.calenee.MainActivity.Companion.slideDown
import org.jin.calenee.MainActivity.Companion.slideLeft
import org.jin.calenee.R
import org.jin.calenee.databinding.ActivityResetPwBinding

class ResetPwActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResetPwBinding

    private var name = ""
    private var email = ""
    private var newPw = ""
    private var newPwCheck = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityResetPwBinding.inflate(layoutInflater)

        setContentView(binding.root)
        listener()
    }

    private fun listener() {
        editTextListener(binding.inputNameEt)
        editTextListener(binding.inputEmailEt)
        editTextListener(binding.inputNewPwEt)
        editTextListener(binding.inputNewPwCheckEt)

        binding.confirmBtn.setOnClickListener {
            if (checkInputCondition()) {
                Snackbar.make(binding.root, "name: $name, email: $email", Snackbar.LENGTH_SHORT)
                    .show()

                // name, email - DB에서 비교
                // if checkUserInfoExists() -> true
                binding.inputNameLayout.isEnabled = false
                binding.inputEmailLayout.isEnabled = false

                TranslateAnimation(0f, 0f, binding.linearlayout2.width.toFloat(), 0f).apply {
                    duration = 600
                    fillAfter = true

                    binding.linearlayout2.animation = this
                    binding.linearlayout2.isVisible = true
                }
            }

            if (checkInputConditionForPw()) {
                Snackbar.make(
                    binding.root,
                    "new pw: $newPw, new pw check: $newPwCheck",
                    Snackbar.LENGTH_SHORT
                )
                    .show()
            }
        }
    }

    private fun checkInputCondition(): Boolean {
        return when {
            name.isEmpty() -> {
                binding.inputNameLayout.apply {
                    error = "이름을 입력해주세요"
                    requestFocus()
                }
                false
            }

            email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email)
                .matches() -> {
                binding.inputEmailLayout.apply {
                    error = "이메일을 올바르게 입력해주세요"
                    requestFocus()
                }
                false
            }

            else -> true
        }
    }

    private fun checkInputConditionForPw(): Boolean {
        return when {
            newPw.isEmpty() || newPw.length < 6 -> {
                binding.inputNewPwLayout.apply {
                    error = "비밀번호를 6자리 이상 입력해주세요"
                    requestFocus()
                }
                false
            }

            newPwCheck.isEmpty() -> {
                binding.inputNewPwCheckLayout.apply {
                    error = "비밀번호 확인을 위해 입력한 비밀번호를 다시 입력해주세요"
                    requestFocus()
                }
                false
            }

            newPw != newPwCheck -> {
                binding.inputNewPwCheckLayout.apply {
                    error = "비밀번호가 일치하지 않습니다"
                    requestFocus()
                }
                false
            }

            else -> true
        }
    }

    private fun editTextListener(editText: TextInputEditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(text: Editable?) {
                when (editText.id) {
                    R.id.input_name_et -> {
                        name = "$text"
                        binding.inputNameLayout.error = null
                    }
                    R.id.input_email_et -> {
                        email = "$text"
                        binding.inputEmailLayout.error = null
                    }
                    R.id.input_new_pw_et -> {
                        newPw = "$text"
                        binding.inputNewPwLayout.error = null
                    }
                    R.id.input_new_pw_check_et -> {
                        newPwCheck = "$text"
                        binding.inputNewPwCheckLayout.error = null
                    }
                }
            }
        })
    }

    override fun onBackPressed() {
        super.onBackPressed()
        slideLeft()
    }
}