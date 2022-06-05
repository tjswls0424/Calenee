package org.jin.calenee

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.lifecycle.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.jin.calenee.MainActivity.Companion.slideLeft
import org.jin.calenee.databinding.ActivityConnectionBinding
import org.jin.calenee.login.LoginActivity

class ConnectionActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityConnectionBinding.inflate(layoutInflater)
    }

    private val inviteCodeViewModel by lazy {
        ViewModelProvider(this).get(InviteCodeViewModel::class.java)
    }

    private var partnerInviteCode = ""

    // 분 수정 (1 -> 10)
    private val mCountDown: CountDownTimer = object : CountDownTimer(1000 * 60 * 10, 1000 * 60) {
        override fun onTick(millisUntilFinished: Long) {
            //update the UI with the new count
            "0${(millisUntilFinished.toFloat() / 60000.0f).toInt()}".also {
                binding.timerMin.text = it
            }
            sCountDown.start()
        }

        override fun onFinish() {
            //countdown finish
            inviteCodeViewModel.updateExpirationFlag(true)
        }
    }

    val sCountDown: CountDownTimer = object : CountDownTimer(1000 * 60, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            //update the UI with the new count
            if ((millisUntilFinished.toFloat() / 1000.0f) / 10.0f >= 1.0f) {
                "${(millisUntilFinished.toFloat() / 1000.0f).toInt()}".also {
                    binding.timerSec.text = it
                }
            } else {
                "0${(millisUntilFinished.toFloat() / 1000.0f).toInt()}".also {
                    binding.timerSec.text = it
                }
            }
        }

        override fun onFinish() {
            //countdown finish
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setMyInviteCode()

        setContentView(binding.root)

        listener()
        observeData()
    }

    private fun listener() {
        binding.logoutBtn.setOnClickListener {
            val googleSignInOptions: GoogleSignInOptions by lazy {
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
            }

            val googleSignInClient by lazy {
                GoogleSignIn.getClient(this, googleSignInOptions)
            }

            googleSignInClient.signOut().addOnCompleteListener {
                LoginActivity.viewModel.signOut()
            }

            // set SP
            App.userPrefs.apply {
                setString("current_email", "")
                setString("current_name", "")
                setString("login_status", "false")
            }

            Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()

            Intent(this, LoginActivity::class.java).apply {
                startActivity(this)
                slideLeft()
                finish()
            }
        }

        binding.refreshInviteCodeBtn.setOnClickListener {
            setMyInviteCode()
        }

        var previousLength = 0
        var backspace: Boolean
        binding.inputCodeEt.apply {
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    previousLength = "$text".length
                }

                override fun onTextChanged(
                    p0: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                }

                override fun afterTextChanged(text: Editable?) {
                    backspace = previousLength > "$text".length

                    if (!backspace && "$text".length == 4) {
                        text?.append(" ")
                    }

                    partnerInviteCode = "$text"

                    Log.d("text_test/inviteCode", partnerInviteCode)
                }
            })

            setOnKeyListener { view, keyCode, keyEvent ->
                if (keyEvent.action == KeyEvent.ACTION_DOWN &&
                    keyEvent.keyCode == KeyEvent.KEYCODE_DEL
                ) {
                    if (binding.inputCodeEt.text.length == 5) {
                        binding.inputCodeEt.setText(partnerInviteCode.dropLast(1))
                        binding.inputCodeEt.setSelection(binding.inputCodeEt.text.length)

                        Log.d("text_test/del1", "delete1 code length: ${partnerInviteCode.length}")
                    }
                }

                return@setOnKeyListener false
            }

            accessibilityDelegate = object : View.AccessibilityDelegate() {
                override fun sendAccessibilityEvent(host: View?, eventType: Int) {
                    super.sendAccessibilityEvent(host, eventType)

                    if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
                        binding.inputCodeEt.setSelection(binding.inputCodeEt.text.length)
                    }
                }
            }
        }


    }

    private fun observeData() {
        inviteCodeViewModel.myInviteCode.observe(this) {
            binding.inviteCodeTv.text = it
        }

        inviteCodeViewModel.expirationFlag.observe(this) {
            if (it) Snackbar.make(
                binding.root,
                "내 초대코드가 만료되었습니다. 하단의 재발급 버튼을 눌러 초대코드를 재발급 해주세요.",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun setMyInviteCode() {
        inviteCodeViewModel.updateExpirationFlag(false)
        var tmpMyInviteCode = ""

        for (i in 0..7) {
            if (tmpMyInviteCode.length == 4) tmpMyInviteCode += " "
            tmpMyInviteCode += (0..9).random()
        }

        inviteCodeViewModel.updateMyInviteCode(tmpMyInviteCode)
        mCountDown.start()
    }

    override fun onBackPressed() {}

}

class InviteCodeViewModel : ViewModel() {
    private val _myInviteCode = MutableLiveData<String>()
    val myInviteCode: LiveData<String> get() = _myInviteCode

    private val _expirationFlag = MutableLiveData<Boolean>()
    val expirationFlag: LiveData<Boolean> get() = _expirationFlag

    init {
        this._myInviteCode.value = ""
        this._expirationFlag.value = false
    }

    fun updateMyInviteCode(inviteCode: String) = viewModelScope.launch {
        _myInviteCode.value = inviteCode
        _myInviteCode.postValue(inviteCode)
    }

    fun updateExpirationFlag(flag: Boolean) = viewModelScope.launch {
        _expirationFlag.value = flag
        _expirationFlag.postValue(flag)
    }
}
