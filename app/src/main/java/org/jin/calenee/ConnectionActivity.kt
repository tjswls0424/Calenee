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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import org.jin.calenee.MainActivity.Companion.slideLeft
import org.jin.calenee.database.model.CoupleConnection
import org.jin.calenee.databinding.ActivityConnectionBinding
import org.jin.calenee.login.LoginActivity
import java.util.*

class ConnectionActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityConnectionBinding.inflate(layoutInflater)
    }

    private val inviteCodeViewModel by lazy {
        ViewModelProvider(this).get(InviteCodeViewModel::class.java)
    }

    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val fireStore by lazy {
        Firebase.firestore
    }

    private val myConnectionData by lazy {
        CoupleConnection(
            inviteCodeViewModel.myInviteCode.value.toString(),
            firebaseAuth.currentUser?.email.toString(),
            "",
            codeExpirationFlag = false,
            connectionFlag = false
        )
    }

    private val connectionData by lazy {
        hashMapOf(
            "ownerInviteCode" to myConnectionData.ownerInviteCode,
            "ownerEmail" to myConnectionData.ownerEmail,
            "partnerEmail" to myConnectionData.partnerEmail,
            "codeExpirationFlag" to myConnectionData.codeExpirationFlag,
            "connectionFlag" to myConnectionData.connectionFlag,
            "addedDate" to FieldValue.serverTimestamp()
        )
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

        // initialize my connection data
        WriteData().addData()
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

            WriteData().deleteData()
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
                ) {}

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

        binding.connectBtn.setOnClickListener {
            if (partnerInviteCode.isEmpty()) {
                Snackbar.make(binding.root, "상대방의 초대코드를 입력해주세요.", Snackbar.LENGTH_SHORT).show()
            } else if (partnerInviteCode.length < 9) {
                Snackbar.make(binding.root, "초대코드를 올바르게 입력해주세요.", Snackbar.LENGTH_SHORT).show()
            } else if (inviteCodeViewModel.myInviteCode.value == partnerInviteCode) {
                Snackbar.make(binding.root, "자신의 초대코드는 입력할 수 없습니다.", Snackbar.LENGTH_SHORT).show()
            } else {
                // 유효한 코드인지 DB 검색
                WriteData().isValidInviteCode(partnerInviteCode)
            }
        }
    }

    private fun observeData() {
        inviteCodeViewModel.myInviteCode.observe(this) {
            WriteData().updateInviteCode(it)
            binding.inviteCodeTv.text = it
        }

        binding.shareCodeBtn.setOnClickListener {}

        inviteCodeViewModel.expirationFlag.observe(this) {
            WriteData().updateExpirationFlag(true)

            if (it) Snackbar.make(
                binding.root,
                "내 초대코드가 만료되었습니다. 하단의 재발급 버튼을 눌러 초대코드를 재발급 해주세요.",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun setMyInviteCode() {
        mCountDown.start()
        inviteCodeViewModel.updateExpirationFlag(false)
        var tmpMyInviteCode = ""

        for (i in 0..7) {
            if (tmpMyInviteCode.length == 4) tmpMyInviteCode += " "
            tmpMyInviteCode += (0..9).random()
        }

        inviteCodeViewModel.updateMyInviteCode(tmpMyInviteCode)
    }

    override fun onBackPressed() {}

    inner class WriteData {
        fun addData() {
            fireStore.collection("connection")
                .document(firebaseAuth.currentUser?.email.toString())
                .set(connectionData)
        }

        fun updateInviteCode(newInviteCode: String) {
            val tmpMap = mapOf(
                "ownerInviteCode" to newInviteCode,
                "codeExpirationFlag" to false,
                "addedDate" to FieldValue.serverTimestamp()
            )

            fireStore.collection("connection")
                .document(firebaseAuth.currentUser?.email.toString())
                .update(tmpMap)
        }

        fun updateExpirationFlag(flag: Boolean) {
            fireStore.collection("connection")
                .document(firebaseAuth.currentUser?.email.toString())
                .update("codeExpirationFlag", flag)
        }

        // if connection btn clicked
        fun isValidInviteCode(partnerInviteCode: String) {
            fireStore.collection("connection")
                .get()
                .addOnSuccessListener { result ->
                    var validFlag = false
                    for (doc in result) {
                        // todo: 반복문 동안 loading screen 구현
                        if (doc["ownerInviteCode"] == partnerInviteCode && doc["codeExpirationFlag"] == false) {
                            validFlag = true


                            /*
                            * SnapShot listener 사용
                            * 해당 이메일 (입력한 초대 코드 주인)의 document에서
                            * partnerEmail이 내 이메일과 일치 + connectionFlag == true 일 경우
                            * 내 이메일, 상대 이메일 저장 후 다음 화면으로 이동
                            * */

//                            val tmpPartnerEmail = doc["ownerEmail"].toString()

                            Log.d("db_test/get", "owner: ${doc["ownerEmail"]}, code: ${doc["ownerInviteCode"]}")

                            break

                            // time stamp (invite code expiration 때 사용)
//                                val tmp: Timestamp = doc["addedDate"] as Timestamp
//                                val cal = Calendar.getInstance().apply {
//                                    time = tmp.toDate()
//                                }
                        }
                    }

                    if (validFlag) {
                        Snackbar.make(binding.root, "상대방에게 연결을 요청했습니다.\n상대방이 요청에 응할 때까지 기다려주세요.", Snackbar.LENGTH_SHORT).show()
                    } else {
                        Snackbar.make(binding.root, "유효하지 않은 초대코드 입니다.", Snackbar.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.d("db_test", "error getting documents: $e")
                }
        }

        fun deleteData() {
            fireStore.collection("connection")
                .document(firebaseAuth.currentUser?.email.toString())
                .delete()
        }

    }
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