package org.jin.calenee.login

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Job
import org.jin.calenee.MainActivity
import org.jin.calenee.MainActivity.Companion.slideLeft
import org.jin.calenee.MainActivity.Companion.slideRight
import org.jin.calenee.R
import org.jin.calenee.databinding.ActivityLoginActivtyBinding

class LoginActivity : AppCompatActivity() {

    companion object {
        internal val viewModel: LoginViewModel = LoginViewModel()
    }

    private lateinit var binding: ActivityLoginActivtyBinding

    private lateinit var fetchJob: Job
    private var tokenId: String? = null

    private val googleSignInOptions: GoogleSignInOptions by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }

    private val googleSignInClient by lazy {
        GoogleSignIn.getClient(this, googleSignInOptions)
    }

    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val loginLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    task.getResult(ApiException::class.java)?.let { account ->
                        tokenId = account.idToken
                        viewModel.saveToken(tokenId ?: throw java.lang.Exception())
                    } ?: throw Exception()
                } catch (e: Exception) {
                    e.printStackTrace()
                    handleErrorState() // error state
                }
            } else {
                handleErrorState() // error state
            }
        }

    override fun onStart() {
        super.onStart()

        Log.d("login_test", "token: $tokenId")
        // db에서 login status 체크
//        loginLauncher.launch(googleSignIn.signInIntent)
        observeData()
        initViews()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginActivtyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fetchJob = viewModel.fetchData(tokenId)

        observeData()
        initViews()
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
            loginLauncher.launch(googleSignInClient.signInIntent)
        }

        binding.signUpBtn.setOnClickListener {
            Intent(this, SignUpActivity::class.java).apply {
                startActivity(this)
                slideRight()
            }
        }
    }

    private fun initViews() {
        tokenId?.let {
            // 로그인 된 상태
            Log.d("login_test", "login status: true")
            Intent(this@LoginActivity, MainActivity::class.java).apply {
                startActivity(this)
                slideRight()
                finish()
            }
        } ?: kotlin.run {
            Log.d("login_test", "login status: false")
        }
    }

    private fun observeData() = viewModel.loginStateLiveData.observe(this) {
        when (it) {
            is LoginState.UnInitialized -> initViews()
            is LoginState.Loading -> handleLoadingState()
            is LoginState.Login -> handleLoginState(it)
            is LoginState.Success -> {
                handleSuccessState(it)
                initViews()
            }
            is LoginState.Error -> handleLoadingState()
        }
    }

    // while loading
    private fun handleLoadingState() = with(binding) {
        progressBar.isVisible = true
        loadingScreen.isVisible = true
        Log.d("login_test", "loading")
    }

    // google auth login 상태인 경우
    private fun handleLoginState(state: LoginState.Login) = with(binding) {
        progressBar.isVisible = true
        loadingScreen.isVisible = true

        val credential = GoogleAuthProvider.getCredential(state.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this@LoginActivity) { task ->
                if (task.isSuccessful) {
                    viewModel.setUserInfo(firebaseAuth.currentUser)
                } else {
                    // fail to login
                    Log.d("login_test", "login state: false")
                    viewModel.setUserInfo(null)
                }
            }
    }

    // google auth login state: success
    private fun handleSuccessState(state: LoginState.Success) = with(binding) {
        progressBar.isGone = true
        loadingScreen.isGone = true

        when (state) {
            is LoginState.Success.Registered -> {
                // google auth 등록된 상태
                // Success.Registered 상태로 변경
                handleRegisteredState(state)
            }

            is LoginState.Success.NotRegistered -> {
                // google auth 미등록 상태
                Log.d("login_test", "google auth is not registered")
            }
        }
    }

    // google auth login registered state
    private fun handleRegisteredState(state: LoginState.Success.Registered) {
        Log.d("login_test", "user: ${state.userName}")
    }

    // state: error
    private fun handleErrorState() {
        Log.d("login_test", "login error")
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        // get user info (signed in) from google
        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)

        firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Intent(this@LoginActivity, MainActivity::class.java).apply {
                    startActivity(this)
                    slideLeft()
                    finish()
                }
            } else {
                Snackbar.make(binding.root, "구글 로그인에 실패했습니다.", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1000) {
            if (resultCode == Activity.RESULT_OK) {
                val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data!!)

                if (result != null) {
                    if (result.isSuccess) {
                        // send user email info to Firebase server
                        firebaseAuthWithGoogle(result.signInAccount)
                    }
                } else {
                    Log.d("login_test", "fail to login from firebase server")
                }
            }
        }
    }

    override fun onBackPressed() {}
}