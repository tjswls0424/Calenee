package org.jin.calenee.chat

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.jin.calenee.App
import org.jin.calenee.databinding.ActivityChattingBinding
import java.text.SimpleDateFormat
import java.util.*

const val DATE_TIME = 0
const val TIME = 1

class ChattingActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityChattingBinding.inflate(layoutInflater)
    }

    private val bottomSheetBehavior by lazy {
        BottomSheetBehavior.from(binding.bottomSheetView)
    }

    private val currentUserEmail by lazy {
        FirebaseAuth.getInstance().currentUser?.email.toString()
    }
    private val firestore by lazy {
        Firebase.firestore
    }
    private val chatDB by lazy {
        FirebaseDatabase.getInstance().getReference("chat")
    }
    private val chatAdapter by lazy {
        ChatAdapter()
    }

    //    private lateinit var chatAdapter: ChatAdapter
    private var isKeyboardShown: Boolean = false
    private var chatDataList: MutableList<ChatData> = mutableListOf()
    private var message: String = ""
    private var nickname: String = ""

//    override fun onStart() {
//        super.onStart()
//
//        // lottie button click does not work at once
//        binding.lottieAddCloseBtn.performClick()
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)
        listener()
        getNickname()
        getSavedChatData()
    }

    private fun initRecycler() {
        Log.d("fb_test/chat-init", "init recycler view")
        LinearLayoutManager(applicationContext).apply {
            stackFromEnd = true
            binding.chatRecyclerView.layoutManager = this
        }

        chatAdapter.apply {
            binding.chatRecyclerView.adapter = this
            data = chatDataList
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun listener() {
        val inputMethodManager =
            getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var viewHeight = -1

        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            binding.root.getWindowVisibleDisplayFrame(rect)
            val screenHeight = binding.root.rootView.height
            val keyBoardHeight = screenHeight - rect.bottom
            if (keyBoardHeight > screenHeight * 0.15) {
                isKeyboardShown = true

                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                    // bottom sheet OFF -> soft keyboard appear
                    setLottieInitialState()
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    binding.coordinatorLayout.visibility = View.GONE

                    inputMethodManager.showSoftInput(binding.root, 0)
                }
                Log.d("k_test", "is showing")
            } else {
                isKeyboardShown = false
                Log.d("k_test", "is closed")
            }

            val currentViewHeight = binding.root.height
            if (currentViewHeight > viewHeight) {
                binding.bottomSheetView.minimumHeight =
                    currentViewHeight / 2 - binding.messageEt.height
                bottomSheetBehavior.peekHeight = currentViewHeight / 2 - binding.messageEt.height

                viewHeight = currentViewHeight
            }
        }

        binding.lottieAddCloseBtn.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                // bottom sheet OFF -> soft keyboard appear
                setLottieInitialState()
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                binding.coordinatorLayout.visibility = View.GONE

                inputMethodManager.showSoftInput(binding.messageEt, 0)
            } else {
                binding.lottieAddCloseBtn.apply {
                    progress = 0.0f
                    playAnimation()
                }
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                binding.coordinatorLayout.visibility = View.VISIBLE

                inputMethodManager.hideSoftInputFromWindow(binding.root.windowToken, 0)
            }
        }

        binding.sendBtn.setOnClickListener {
            if (message.isNotEmpty()) {
                ChatData(nickname, message, getCurrentTimeStamp(TIME), 1).also {
                    chatDataList.add(it)
                    saveChatData(it)
                }

                binding.messageEt.setText("")
                initRecycler()
            }
        }

        binding.messageEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(text: Editable?) {
                message = "$text"
            }
        })

        // get realtime chat data
        chatDB.child(App.userPrefs.getString("couple_chat_id"))
            .addChildEventListener(object: ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    Log.d("fb_test_chat/add", snapshot.value.toString())
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    snapshot.getValue(SavedChatData::class.java).also { data ->
                        if (
                            !data?.message.isNullOrBlank() &&
                            !data?.createdAt.isNullOrBlank() &&
                            !data?.senderEmail.isNullOrBlank() &&
                            !data?.senderNickname.isNullOrBlank()
                        ) {
                            val viewType =
                                if (data?.senderEmail == currentUserEmail) 1 else 0
                            chatDataList.add(
                                ChatData(
                                    data?.senderNickname,
                                    data?.message,
                                    data?.createdAt,
                                    viewType
                                )
                            )

                            initRecycler()
                        }
                    }

                    Log.d("fb_test_chat/changed", snapshot.value.toString())
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    Log.d("fb_test_chat/removed", snapshot.value.toString())
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    Log.d("fb_test_chat/moved", snapshot.value.toString())
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("fb_test_chat/cancelled", error.message)
                }
            })
    }

    // 첫 실행시 (함수 종료 될 때까지) success listener에 값이 들어오기까지 몇 초 delay가 있기 때문에 미리 호출
    private fun getNickname() {
        firestore.collection("user")
            .document(currentUserEmail)
            .get()
            .addOnSuccessListener { doc ->
                nickname = doc.data?.get("nickname").toString()
                Log.d("db_test/nickname1", nickname)
            }
            .addOnFailureListener {
                Log.d("db_test/login-err", "${it.printStackTrace()}")
            }
    }

    private fun saveChatData(data: ChatData) {
        chatDB.child(App.userPrefs.getString("couple_chat_id"))
            .child(getCurrentTimeStamp(DATE_TIME)).apply {
                child("senderEmail").setValue(currentUserEmail)
                child("senderNickname").setValue(data.nickname)
                child("message").setValue(data.message)
                child("createdAt").setValue(data.time)
//                child("read").setValue(false)
            }
    }

    // first loading chat screen
    private fun getSavedChatData() {
        chatDB.child(App.userPrefs.getString("couple_chat_id"))
            .get().addOnSuccessListener {
                it.children.forEachIndexed { index, dataSnapshot ->
                    dataSnapshot.getValue(SavedChatData::class.java).also { data ->
                        val viewType = if (data?.senderEmail == currentUserEmail) 1 else 0
                        chatDataList.add(
                            ChatData(
                                data?.senderNickname,
                                data?.message,
                                data?.createdAt,
                                viewType
                            )
                        )
                    }
                }

                initRecycler()
            }
            .addOnFailureListener {
                Log.d("db_test", "fail to read from realtime database")
            }
    }


    private fun setLottieInitialState() {
        binding.lottieAddCloseBtn.apply {
            progress = 0.0f
            cancelAnimation()
        }
    }

    private fun getCurrentTimeStamp(type: Int): String {
        return when (type) {
            DATE_TIME -> System.currentTimeMillis().toString()
            TIME -> SimpleDateFormat("HH:mm", Locale.KOREA).format(System.currentTimeMillis())
            else -> throw RuntimeException("get time error")
        }
    }

    override fun onBackPressed() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            setLottieInitialState()
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            binding.coordinatorLayout.visibility = View.GONE
        } else {
            super.onBackPressed()
        }
    }
}