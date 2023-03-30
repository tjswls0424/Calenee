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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import org.jin.calenee.*
import org.jin.calenee.MainActivity.Companion.slideLeft
import org.jin.calenee.MainActivity.Companion.slideRight
import org.jin.calenee.data.SyncData
import org.jin.calenee.data.firestore.CoupleInfoSync
import org.jin.calenee.data.firestore.UserSync
import org.jin.calenee.databinding.ActivityCaleneeLoginBinding

private const val TAG = "CaleneeLoginActivity:"
class CaleneeLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCaleneeLoginBinding

    private val signInJob = Job()
    private var email: String = ""
    private var pw: String = ""

    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val firestore by lazy {
        Firebase.firestore
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

    private suspend fun syncUserData() {
        withContext(Dispatchers.Main) {
            val deferredUser = async(Dispatchers.IO) {
                val userDoc = Firebase.firestore.collection("user")
                    .document(email)
                    .get()
                    .await()

                return@async if (userDoc.exists()) {
                    userDoc.toObject(UserSync::class.java) as UserSync
                } else {
                    UserSync()
                }
            }

            // userPrefs의 couple_chat_id를 알아야 함
            val deferredCoupleInfo = async(Dispatchers.IO) {
                deferredUser.await().coupleChatID
                val coupleInfoDoc = Firebase.firestore.collection("coupleInfo")
                    .document(deferredUser.await().coupleChatID)
                    .get()
                    .await()

                return@async if (coupleInfoDoc.exists()) {
                    coupleInfoDoc.toObject(CoupleInfoSync::class.java) as CoupleInfoSync
                } else {
                    CoupleInfoSync()
                }
            }

            with(SyncData()) {
                syncUserData(deferredUser.await(), email)
                syncCoupleInfoData(deferredCoupleInfo.await())
                handleLoadingState(false)
            }
        }
    }

    private fun signIn(email: String, pw: String) {
        handleLoadingState(true)

        firebaseAuth.signInWithEmailAndPassword(email, pw)
            .addOnSuccessListener {
                App.userPrefs.apply {
                    setString("login_status", "true")
                    setString("current_email", email)
//                    setString("current_name", this.getString("${email}_name", email))
                }

                val intent = Intent(this@CaleneeLoginActivity, ConnectionActivity::class.java)

                firestore.collection("user")
                    .document(firebaseAuth.currentUser?.email.toString())
                    .get()
                    .addOnSuccessListener { doc ->
                        CoroutineScope(Dispatchers.Main + signInJob).launch {
                            doc.data?.let { data ->
                                Log.d("${TAG}login-doc1", data.toString())
                                if (data["coupleConnectionFlag"] == true) {
                                    val deferred = async {
                                        return@async if (data["profileInputFlag"] == true) {
                                            syncUserData()

                                            Intent(
                                                this@CaleneeLoginActivity,
                                                MainActivity::class.java
                                            )
                                        } else {
                                            Intent(
                                                this@CaleneeLoginActivity,
                                                ConnectionInputActivity::class.java
                                            )
                                        }
                                    }

                                    startActivity(deferred.await())
                                    finish()
                                } else {
                                    Log.d("${TAG}login-doc2", "coupleConnectionFlag is not true")
                                    startActivity(intent)
                                }
                            } ?: kotlin.run {
                                launch(Dispatchers.Main) {
                                    Log.d("${TAG}login-doc3", "doc is null")
                                    signInJob.cancel()

                                    startActivity(intent)
                                    finish()
                                }
                            }
                        }
                    }
                    .addOnFailureListener {
                        handleLoadingState(false)

                        Log.d("${TAG}login_test/normal-err", it.stackTraceToString())
                        Snackbar.make(
                            binding.root,
                            "이메일 또는 패스워드가 올바르지 않습니다.",
                            Snackbar.LENGTH_SHORT
                        )
                            .show()
                    }
            }
            .addOnFailureListener {
                Log.d("${TAG}login_test/signIn-err", it.stackTraceToString())
            }
    }

    // while loading
    private fun handleLoadingState(flag: Boolean) = with(binding) {
        if (flag) {
            progressBar.isVisible = true
            loadingScreen.isVisible = true
            Log.d("${TAG}login_test", "loading")
        } else {
            progressBar.isGone = true
            loadingScreen.isGone = true
            Log.d("${TAG}login_test", "loading stop")
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