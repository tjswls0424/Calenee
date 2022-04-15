package org.jin.calenee.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import org.jin.calenee.MainActivity
import org.jin.calenee.MainActivity.Companion.slideLeft
import org.jin.calenee.MainActivity.Companion.slideRight
import org.jin.calenee.databinding.ActivityLoginActivtyBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginActivtyBinding
    private lateinit var mAuth: FirebaseAuth

    override fun onStart() {
        super.onStart()

        val currentUser = mAuth.currentUser.let {
//            Intent(this, MainActivity::class.java).apply {
//                Log.d("login_test", "login success: ${it?.email}")
//                startActivity(this)
//                finish()
//            }
        }

        Log.d("login_test", "auto login failed")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginActivtyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()


        listener()


    }

    private fun listener() {
        binding.caleneeLoginBtn.setOnClickListener {
            Intent(this, CaleneeLoginActivity::class.java).apply {
                startActivity(this)
                slideRight()
            }
            Log.d("btn_test", "calenee")

        }

        binding.googleLoginBtn.setOnClickListener {

        }
    }

    // 회원가입 완료 버튼 클릭시 호출
    private fun createAccount(email: String, pw: String) {
        // sign up
        // email만 따로 중복확인 할 수 있는지 확인
        // 안되면 확인버튼 클릭시 email, pw가 동시에 db에서 확인되기 때문에 문제 발생 가능성 O
        mAuth.createUserWithEmailAndPassword(email, pw)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("login_test", "sign in success, now login with this email")
                        Snackbar.make(binding.root, "회원가입을 완료했습니다.", Snackbar.LENGTH_SHORT).show()

                    } else if (!task.exception?.message.isNullOrEmpty()) {
                        // error or connection server fail
                        Log.d("login_test", "exception - sign up")
                        Snackbar.make(binding.root, "회원가입에 실패했습니다. 잠시 후 재시도 해주세요", Snackbar.LENGTH_SHORT).show()
                    } else {
                        // account already exists
                        // signIn() 호출
                        // snackbar message or popup alert 실행
                        Snackbar.make(binding.root, "이미 생성된 계정입니다.", Snackbar.LENGTH_SHORT).show()
                    }
                }
    }

    private fun signIn(email: String, pw: String) {
        mAuth.signInWithEmailAndPassword(email, pw)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Intent(this@LoginActivity, MainActivity::class.java).apply {
                            startActivity(this)

                            // + progress bar (loading)
                            // + animation 살며시 or 천천히 나타나는거 찾아보기
                            // + none을 duration을 늘리던가
                            slideRight()
                            finish()
                        }
                    } else {
                        // error
                        Snackbar.make(binding.root, "이메일 또는 패스워드가 올바르지 않습니다.", Snackbar.LENGTH_SHORT).show()
                    }
                }
    }
}