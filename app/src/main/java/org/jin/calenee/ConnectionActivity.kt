package org.jin.calenee

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.appcompat.app.AlertDialog
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
import org.jin.calenee.MainActivity.Companion.slideRight
import org.jin.calenee.database.firestore.Couple
import org.jin.calenee.database.firestore.CoupleConnection
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

    private val firestore by lazy {
        Firebase.firestore
    }

    private var connectionDialog: Dialog? = null

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

    private var partnerInviteCodeInput = ""

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

        observeData()
        listener()

        // initialize my connection data
        WriteData().addMyData()
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

            WriteData().deleteMyData()
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

        binding.copyCodeBtn.setOnClickListener {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            ClipData.newPlainText("text", inviteCodeViewModel.myInviteCode.value).also {
                clipboardManager.setPrimaryClip(it)
            }

            Snackbar.make(binding.root, "코드가 복사되었습니다.", Snackbar.LENGTH_SHORT).show()
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

                    partnerInviteCodeInput = "$text"

                    Log.d("text_test/inviteCode", partnerInviteCodeInput)
                }
            })

            setOnKeyListener { _, _, keyEvent ->
                if (keyEvent.action == KeyEvent.ACTION_DOWN &&
                    keyEvent.keyCode == KeyEvent.KEYCODE_DEL
                ) {
                    if (binding.inputCodeEt.text.length == 5) {
                        binding.inputCodeEt.setText(partnerInviteCodeInput.dropLast(1))
                        binding.inputCodeEt.setSelection(binding.inputCodeEt.text.length)

                        Log.d(
                            "text_test/del1",
                            "delete1 code length: ${partnerInviteCodeInput.length}"
                        )
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
            if (partnerInviteCodeInput.isEmpty()) {
                Snackbar.make(binding.root, "상대방의 초대코드를 입력해주세요.", Snackbar.LENGTH_SHORT).show()
            } else if (partnerInviteCodeInput.length < 9) {
                Snackbar.make(binding.root, "초대코드를 올바르게 입력해주세요.", Snackbar.LENGTH_SHORT).show()
            } else if (inviteCodeViewModel.myInviteCode.value == partnerInviteCodeInput) {
                Snackbar.make(binding.root, "자신의 초대코드는 입력할 수 없습니다.", Snackbar.LENGTH_SHORT).show()
            } else {
                // 유효한 코드인지 DB 검색
                WriteData().isValidInviteCode(partnerInviteCodeInput)
            }
        }
    }

    private fun observeData() {
        inviteCodeViewModel.myInviteCode.observe(this) {
            WriteData().updateMyInviteCode(it)
            binding.inviteCodeTv.text = it
        }

        inviteCodeViewModel.ownerEmail.observe(this) {
            Log.d("db_test/partner-email", "partner email: $it")
            // it(owner)의 row로 가서 partnerEmail에 내 email 업데이트
            if (it.isNotEmpty()) {
                WriteData().updatePartnerEmail(it, firebaseAuth.currentUser?.email.toString())
            }
        }

        inviteCodeViewModel.expirationFlag.observe(this) {
            WriteData().updateExpirationFlag(true)

            if (it) Snackbar.make(
                binding.root,
                "내 초대코드가 만료되었습니다. 하단의 재발급 버튼을 눌러 초대코드를 재발급 해주세요.",
                Snackbar.LENGTH_SHORT
            ).show()
        }

        // owner's point of view
        firestore.collection("connection").document(firebaseAuth.currentUser?.email.toString())
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot != null && snapshot.exists()) {
                    snapshot.data?.get("partnerEmail").also {
                        if (it != "") {
                            inviteCodeViewModel.setPartnerEmail(it.toString())
                            connectionDialog = showRequestConnectionDialog()
                        }
                    }
                }
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

    private fun showRequestConnectionDialog(): AlertDialog {
        val myEmail = firebaseAuth.currentUser?.email.toString()

        return AlertDialog.Builder(
            this,
            android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth
        ).apply {
            setTitle("커플 연결 요청")
            setMessage("상대방이 연결을 요청 했습니다.")
            setCancelable(false)
            setPositiveButton("수락") { _, _ ->
                Intent(this@ConnectionActivity, ConnectionInputActivity::class.java).also {
                    WriteData().updateConnectionFlag(true)

                    // for chat DB (path)
                    val coupleChatID = getRandomString(20)

                    // add couple data
                    Couple(
                        myEmail,
                        inviteCodeViewModel.partnerEmail.value.toString(),
                        "",
                        connectionFlag = true
                    ).also { couple ->
                        val coupleDataMap = hashMapOf(
                            "user1Email" to couple.user1Email,
                            "user2Email" to couple.user2Email,
                            "firstMetDate" to couple.firstMetDate,
                            "connectionFlag" to couple.connectionFlag,
                            "coupleChatID" to coupleChatID
                        )

                        val docId = couple.user1Email + "_" + couple.user2Email
                        firestore.collection("couple").document(docId).set(coupleDataMap)

                        hashMapOf(
                            "partnerEmail" to couple.user2Email,
                            "coupleConnectionFlag" to true,
                            "profileInputFlag" to false,
                            "profileImageFlag" to false,
                            "coupleChatID" to coupleChatID
                        ).also {
                            firestore.collection("user")
                                .document(myEmail)
                                .set(it)
                        }
                    }

                    if (connectionDialog!!.isShowing) connectionDialog!!.dismiss()

                    App.userPrefs.apply {
                        setString("${myEmail}_couple_connection_flag", "true")
                        setString("${myEmail}_couple_input_flag", "false")
                        setString("couple_chat_id", coupleChatID)
                    }

                    startActivity(it)
                    slideRight()
                    finish()
                }
            }
            setNegativeButton("거절") { _, _ ->
                // owner의 partner email 초기화
                inviteCodeViewModel.setPartnerEmail("")
                WriteData().updatePartnerEmail(myEmail, "")
            }
        }.show()
    }

    private fun getRandomString(length: Int): String {
        val charset = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..length)
            .map { charset.random() }
            .joinToString("")
    }

    override fun onBackPressed() {}

    inner class WriteData {
        fun addMyData() {
            firestore.collection("connection")
                .document(firebaseAuth.currentUser?.email.toString())
                .set(connectionData)
        }

        // refresh my invite code
        fun updateMyInviteCode(newInviteCode: String) {
            val tmpMap = mapOf(
                "ownerInviteCode" to newInviteCode,
                "partnerEmail" to "",
                "codeExpirationFlag" to false,
                "addedDate" to FieldValue.serverTimestamp()
            )

            firestore.collection("connection")
                .document(firebaseAuth.currentUser?.email.toString())
                .update(tmpMap)
        }

        fun updateExpirationFlag(flag: Boolean) {
            firestore.collection("connection")
                .document(firebaseAuth.currentUser?.email.toString())
                .update("codeExpirationFlag", flag)
        }

        // if connection btn clicked
        fun isValidInviteCode(partnerInviteCode: String) {
            firestore.collection("connection")
                .get()
                .addOnSuccessListener { result ->
                    var validFlag = false
                    for (doc in result) {
                        if (doc["ownerInviteCode"] == partnerInviteCode && doc["codeExpirationFlag"] == false) {
                            validFlag = true

                            inviteCodeViewModel.setOwnerEmail(doc["ownerEmail"].toString())

                            break

                            // time stamp (invite code expiration 때 사용, 현재 시간과 비교하여 일정 시간이 지났을 경우 해당 row 삭제)
//                                val tmp: Timestamp = doc["addedDate"] as Timestamp
//                                val cal = Calendar.getInstance().apply {
//                                    time = tmp.toDate()
//                                }
                        }
                    }

                    if (validFlag) {
                        Snackbar.make(
                            binding.root,
                            "상대방에게 연결을 요청했습니다.\n상대방이 요청에 응할 때까지 기다려주세요.",
                            Snackbar.LENGTH_LONG
                        ).show()

                        // partner's point of view
                        val ownerEmail = inviteCodeViewModel.ownerEmail.value.toString()
                        val myEmail = firebaseAuth.currentUser?.email.toString()
                        val docId = "${ownerEmail}_${myEmail}"
                        Log.d("snap_test/docID-1", docId)

                        firestore.collection("couple").document(docId)
                            .addSnapshotListener { snapshot, error ->
                                if (error != null) return@addSnapshotListener

                                if (snapshot != null && snapshot.exists()) {
                                    if (snapshot["connectionFlag"] == true
                                        && snapshot["user2Email"] == myEmail
                                    ) {
                                        Toast.makeText(
                                            applicationContext,
                                            "커플이 연결되었습니다.",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        val coupleChatID = snapshot["coupleChatID"].toString()

                                        App.userPrefs.apply {
                                            setString("${myEmail}_couple_connection_flag", "true")
                                            setString("${myEmail}_couple_input_flag", "false")
                                            setString("couple_chat_id", coupleChatID)
                                        }

                                        hashMapOf(
                                            "partnerEmail" to ownerEmail,
                                            "coupleConnectionFlag" to true,
                                            "profileInputFlag" to false,
                                            "profileImageFlag" to false,
                                            "coupleChatID" to coupleChatID
                                        ).also {
                                            firestore.collection("user")
                                                .document(myEmail)
                                                .set(it)
                                        }

                                        Intent(
                                            this@ConnectionActivity,
                                            ConnectionInputActivity::class.java
                                        ).also {
                                            startActivity(it)
                                            slideRight()
                                            finish()
                                        }
                                    }
                                } else {
                                    Log.d(
                                        "db_test/couple-error1",
                                        "snapshot is Null OR does not exists"
                                    )
                                    Log.d("db_test/couple-error2", "error : ${error?.message}")
                                }
                            }
                    } else {
                        Snackbar.make(binding.root, "유효하지 않은 초대코드 입니다.", Snackbar.LENGTH_SHORT)
                            .show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.d("db_test", "error getting documents: $e")
                }
        }

        // 상대의 유효한 invite code를 입력 후 연결 요청한 경우, 상대 document의 partnerEmail에 내 email 저장
        fun updatePartnerEmail(ownerEmail: String, partnerEmail: String) {
            firestore.collection("connection")
                .document(ownerEmail)
                .update("partnerEmail", partnerEmail)
        }

        // dialog accept btn cliecked
        fun updateConnectionFlag(flag: Boolean) {
            firestore.collection("connection")
                .document(firebaseAuth.currentUser?.email.toString())
                .update("connectionFlag", flag)
        }

        fun deleteMyData() {
            firestore.collection("connection")
                .document(firebaseAuth.currentUser?.email.toString())
                .delete()
        }

    }
}

class InviteCodeViewModel : ViewModel() {
    private val _myInviteCode = MutableLiveData<String>()
    val myInviteCode: LiveData<String> get() = _myInviteCode

    private val _ownerEmail = MutableLiveData<String>()
    val ownerEmail: LiveData<String> get() = _ownerEmail

    private val _partnerEmail = MutableLiveData<String>()
    val partnerEmail: LiveData<String> get() = _partnerEmail

    private val _expirationFlag = MutableLiveData<Boolean>()
    val expirationFlag: LiveData<Boolean> get() = _expirationFlag

    init {
        this._myInviteCode.value = ""
        this._ownerEmail.value = ""
        this._expirationFlag.value = false
    }

    fun updateMyInviteCode(inviteCode: String) = viewModelScope.launch {
        _myInviteCode.value = inviteCode
        _myInviteCode.postValue(inviteCode)
    }

    // partner's point of view
    fun setOwnerEmail(email: String) = viewModelScope.launch {
        _ownerEmail.value = email
        _ownerEmail.postValue(email)
    }

    // owner's point of view
    fun setPartnerEmail(email: String) = viewModelScope.launch {
        _partnerEmail.value = email
        _partnerEmail.postValue(email)
    }

    fun updateExpirationFlag(flag: Boolean) = viewModelScope.launch {
        _expirationFlag.value = flag
        _expirationFlag.postValue(flag)
    }
}