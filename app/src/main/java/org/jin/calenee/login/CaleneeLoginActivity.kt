package org.jin.calenee.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import org.jin.calenee.App
import org.jin.calenee.MainActivity
import org.jin.calenee.MainActivity.Companion.slideLeft
import org.jin.calenee.MainActivity.Companion.slideRight
import org.jin.calenee.R
import org.jin.calenee.databinding.ActivityCaleneeLoginBinding

class CaleneeLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCaleneeLoginBinding

    private var email: String = ""
    private var pw: String = ""

    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCaleneeLoginBinding.inflate(layoutInflater)

        setContentView(binding.root)

        listener()
    }

    private fun listener() {
        editTextListener(binding.inputEmailEt)
        editTextListener(binding.inputPwEt)

        binding.loginBtn.setOnClickListener {
            if (checkInputCondition()) {
                signIn(email, pw)
                Log.d("login_test", "email: $email, pw: $pw")
            }
        }

        binding.resetPwBtn.setOnClickListener {
            // 다음 activity에서 이름, 이메일 입력 후 DB에 일치하는 정보 있는지 확인
            // 일치하면 send email for reset PW
            Intent(this, ResetPwActivity::class.java).also {
                startActivity(it)
                slideRight()
            }
        }
    }

    private fun editTextListener(editText: TextInputEditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(text: Editable?) {
                when (editText.id) {
                    R.id.input_email_et -> {
                        email = "$text"
                        binding.inputEmailLayout.error = null
                    }
                    R.id.input_pw_et -> {
                        pw = "$text"
                        binding.inputPwLayout.error = null
                    }
                }
            }
        })
    }

    private fun checkInputCondition(): Boolean {
        return when {
            email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.inputEmailLayout.apply {
                    error = "이메일을 올바르게 입력해주세요"
                    requestFocus()
                }
                false
            }

            pw.isEmpty() || pw.length < 6 -> {
                binding.inputPwLayout.apply {
                    error = "비밀번호를 6자 이상 입력해주세요"
                    requestFocus()
                }
                false
            }

            else -> true
        }
    }

    private fun signIn(email: String, pw: String) {
        handleLoadingState(true)

        firebaseAuth.signInWithEmailAndPassword(email, pw)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    App.userPrefs.apply {
                        setString("current_email", email)
                        setString("current_name", this.getString("${email}_name", email))
                        setString("login_status", "true")
                    }

                    handleLoadingState(false)

                    Log.d("login_test/normal", "sign in success")
                    Intent(this@CaleneeLoginActivity, MainActivity::class.java).also {
                        startActivity(it)
                        finish()
                    }
                } else {
                    // error
                    handleLoadingState(false)

                    Log.d("login_test/normal-err", task.exception?.message.toString())
                    Snackbar.make(binding.root, "이메일 또는 패스워드가 올바르지 않습니다.", Snackbar.LENGTH_SHORT)
                        .show()
                }
            }
    }

    // while loading
    private fun handleLoadingState(flag: Boolean) = with(binding) {
        if (flag) {
            progressBar.isVisible = true
            loadingScreen.isVisible = true
            Log.d("login_test", "loading")
        } else {
            progressBar.isGone = true
            loadingScreen.isGone = true
            Log.d("login_test", "loading stop")
        }
    }

    override fun finish() {
        super.finish()
        slideRight()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        slideLeft()
    }
}