package org.jin.calenee.login

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import org.jin.calenee.App
import org.jin.calenee.MainActivity.Companion.slideLeft
import org.jin.calenee.R
import org.jin.calenee.databinding.ActivityLoginActivtyBinding
import org.jin.calenee.databinding.ActivitySignUpBinding

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var loginbinding: ActivityLoginActivtyBinding
    private lateinit var englishKoreanTranslator: Translator

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
        loginbinding = ActivityLoginActivtyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listener()
        setTranslation()
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

                email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email)
                    .matches() -> {
                    inputEmailLayout.apply {
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

                pwCheck.isEmpty() -> {
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
                englishKoreanTranslator.translate(task.exception?.message.toString())
                    .addOnSuccessListener { translatedText ->
                        if (task.isSuccessful) {
                            // add user info to db
                            App.userPrefs.apply {
                                setString("${email}_name", name)
                                setString("${email}_pw", pw)
                            }

                            Log.d("login_test1", "sign in success, now login with this email")
                            Toast.makeText(this, "회원가입이 완료되었습니다", Toast.LENGTH_SHORT).show()
                            finish()
                        } else if (!task.exception?.message.isNullOrEmpty()) {
                            Log.d("login_test2", "exception1: sign up [${task.exception?.message}]")
                            Log.d("login_test3", translatedText)

                            Snackbar.make(
                                binding.root,
                                translatedText,
                                Snackbar.LENGTH_SHORT
                            ).show()
                        } else {
                            // account already exists
                            Snackbar.make(binding.root, "이미 생성된 계정입니다.", Snackbar.LENGTH_SHORT)
                                .show()
                            finish()
                        }
                    }
                    .addOnFailureListener {
                        Log.d("login_test1", "fail to translation: ${it.message}")
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
                        name = "$text"
                        binding.inputNameLayout.error = null
                    }
                    R.id.input_email_et -> {
                        email = "$text"
                        binding.inputEmailLayout.error = null
                    }
                    R.id.input_pw_et -> {
                        pw = "$text"
                        binding.inputPwLayout.error = null
                    }
                    R.id.input_pw_check_et -> {
                        pwCheck = "$text"
                        binding.inputPwCheckLayout.error = null
                    }
                }
            }
        })
    }

    private fun setTranslation() {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.KOREAN)
            .build()

        englishKoreanTranslator = Translation.getClient(options)

        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        englishKoreanTranslator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                // ready to start translating
                Log.d("trans_test", "success to download model")

            }
            .addOnFailureListener {
                // Model couldn’t be downloaded or other internal error.
                Log.d("trans_test", "fail to download model")
            }
    }

    override fun finish() {
        super.finish()
        slideLeft()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        slideLeft()
    }
}