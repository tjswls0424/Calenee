package org.jin.calenee.login

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import org.jin.calenee.MainActivity.Companion.slideLeft
import org.jin.calenee.R
import org.jin.calenee.databinding.ActivitySignUpBinding

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private var email: String = ""
    private var name: String = ""
    private var pw: String = ""
    private var pwCheck: String = ""

    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listener()
    }

    private fun listener() = with(binding) {
        editTextListener(inputEmailEt)
        editTextListener(inputNameEt)
        editTextListener(inputPwEt)
        editTextListener(inputPwCheckEt)

        signUpBtn.setOnClickListener {
            if (checkInputCondition()) {
                createAccount(email, pw)
                Log.d("text_test", "$name, $email, $pw , $pwCheck")
            }
        }
    }

    private fun checkInputCondition(): Boolean {
        with(binding) {
            return when {
                name.isEmpty() -> {
                    inputNameLayout.apply {
                        error = "이름을 입력해주세요"
                        requestFocus()
                    }
                    false
                }

                email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    inputEmailLayout.apply{
                        error = "이메일을 올바르게 입력해주세요"
                        requestFocus()
                    }
                    false
                }

                pw.isEmpty() || pw.length < 6 -> {
                    inputPwLayout.apply {
                        error = "비밀번호를 6자리 이상 입력해주세요"
                        requestFocus()
                    }
                    false
                }

                pwCheck.isEmpty()-> {
                    inputPwCheckLayout.apply {
                        error = "비밀번호 확인을 위해 입력한 비밀번호를 다시 입력해주세요"
                        requestFocus()
                    }
                    false
                }

                pw != pwCheck -> {
                    inputPwCheckLayout.apply {
                        error = "비밀번호가 일치하지 않습니다"
                        requestFocus()
                    }
                    false
                }

                else -> true
            }
        }
    }

    // sign up
    private fun createAccount(email: String, pw: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, pw)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("login_test", "sign in success, now login with this email")
                    Snackbar.make(binding.root, "회원가입을 완료했습니다.", Snackbar.LENGTH_SHORT).show()
                    slideLeft()
                } else if (!task.exception?.message.isNullOrEmpty()) {
                    Log.d("login_test", "exception1: sign up [${task.exception?.message}]")

                    val msg = task.exception?.localizedMessage ?: "회원가입에 실패했습니다. 잠시 후 재시도 해주세요"

                    // task msg 한국어 설정

                    Snackbar.make(
                        binding.root,
                        msg,
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    // account already exists
                    // signIn() 호출
                    // snackbar message or popup alert 실행
                    Snackbar.make(binding.root, "이미 생성된 계정입니다.", Snackbar.LENGTH_SHORT).show()
                    finish()
                }
            }
    }

    private fun editTextListener(editText: TextInputEditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(text: Editable?) {
                when (editText.id) {
                    R.id.input_name_et -> {
                        name = "${text?.trim()}"
                        binding.inputNameLayout.error = null
                    }
                    R.id.input_email_et -> {
                        email = "${text?.trim()}"
                        binding.inputEmailLayout.error = null
                    }
                    R.id.input_pw_et -> {
                        pw = "${text?.trim()}"
                        binding.inputPwLayout.error = null
                    }
                    R.id.input_pw_check_et -> {
                        pwCheck = "${text?.trim()}"
                        binding.inputPwCheckLayout.error = null
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