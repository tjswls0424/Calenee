package org.jin.calenee.chat

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.MarginLayoutParamsCompat
import androidx.core.view.marginBottom
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
const val DATE = 2

class ChattingActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityChattingBinding.inflate(layoutInflater)
    }

//    private val bottomSheetBehavior by lazy {
//        BottomSheetBehavior.from(binding.bottomSheetView)
//    }

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

//                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
//                    // bottom sheet OFF -> soft keyboard appear
//                    setLottieInitialState()
//                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
//                    binding.coordinatorLayout.visibility = View.GONE
//
//                    inputMethodManager.showSoftInput(binding.root, 0)
//                }

                // bbi
                if (binding.bottomSheetView.visibility == View.VISIBLE) {
                    setLottieInitialState()
                    binding.bottomSheetView.visibility = View.GONE
                    inputMethodManager.showSoftInput(binding.root, 0)
                }

                Log.d("k_test", "is showing")
            } else {
                isKeyboardShown = false
                Log.d("k_test", "is closed")
            }

            val currentViewHeight = binding.root.height
            if (currentViewHeight > viewHeight) {
                val dm = applicationContext.resources.displayMetrics.heightPixels
                window.attributes.height = (dm * 0.7).toInt()


//                binding.bottomSheetView.minimumHeight =
//                    currentViewHeight / 2 - binding.messageEt.height
//                bottomSheetBehavior.peekHeight = currentViewHeight / 2 - binding.messageEt.height
//
//                viewHeight = currentViewHeight
            }
        }

        binding.lottieAddCloseBtn.setOnClickListener {
            // bbi
            val param = binding.scrollView.layoutParams as ViewGroup.MarginLayoutParams
            if (binding.bottomSheetView.visibility == View.GONE) {

                param.setMargins(0, 0, 0, binding.bottomSheetView.minimumHeight)
                binding.scrollView.layoutParams = param

                binding.bottomSheetView.visibility = View.VISIBLE

                binding.lottieAddCloseBtn.apply {
                    progress = 0.0f
                    playAnimation()
                }

                inputMethodManager.hideSoftInputFromWindow(binding.root.windowToken, 0)

//                setLottieInitialState()
            } else {
                // if visible
                setLottieInitialState()
                param.setMargins(0, 0, 0, binding.bottomLayout.height)
                binding.scrollView.layoutParams = param
                binding.bottomSheetView.visibility = View.GONE
            }

//            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
//                // bottom sheet OFF -> soft keyboard appear
//                setLottieInitialState()
//                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
//                binding.coordinatorLayout.visibility = View.GONE
//
//            } else {
//                binding.lottieAddCloseBtn.apply {
//                    progress = 0.0f
//                    playAnimation()
//                }
//                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//                binding.coordinatorLayout.visibility = View.VISIBLE
//
////                inputMethodManager.hideSoftInputFromWindow(binding.root.windowToken, 0)
//            }
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
            .addChildEventListener(object : ChildEventListener {
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

                            // Prevent messages from being shown twice if I send message
                            if (viewType != 1) {
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
    var tmpTimeKey = 0L
    private fun getSavedChatData() {
        chatDB.child(App.userPrefs.getString("couple_chat_id"))
            .get().addOnSuccessListener {
                it.children.forEachIndexed { index, dataSnapshot ->
                    dataSnapshot.getValue(SavedChatData::class.java).also { data ->
                        val tmpDate = Calendar.getInstance().apply {
                            timeInMillis = tmpTimeKey
                            add(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        Log.d("fb_test/date2", getCurrentTimeStamp(DATE, tmpDate))

                        // for date text
                        when {
                            // tmpTimeKey랑 비교하는 절이 항상 true임
                            index == 0 || tmpDate < (dataSnapshot.key?.toLong() ?: 0L) -> {
                                tmpTimeKey = dataSnapshot.key?.toLong() ?: 0
                                chatDataList.add(
                                    ChatData(
                                        time = getCurrentTimeStamp(
                                            DATE,
                                            tmpTimeKey
                                        ), viewType = 2
                                    )
                                )
                            }
                        }

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

    private fun getCurrentTimeStamp(type: Int, timeMillis: Long = 0): String {
        return when (type) {
            DATE_TIME -> System.currentTimeMillis().toString()
            TIME -> SimpleDateFormat("HH:mm", Locale.KOREA).format(System.currentTimeMillis())
            DATE -> SimpleDateFormat("yyyy년 MM월 dd일 (E)", Locale.KOREAN).format(timeMillis)
            else -> throw RuntimeException("get time error")
        }
    }

    override fun onBackPressed() {
        // bbi
        if (binding.bottomSheetView.visibility == View.VISIBLE) {
            val param = binding.scrollView.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 0, 0, binding.bottomLayout.height)
            binding.scrollView.layoutParams = param

            setLottieInitialState()
            binding.bottomSheetView.visibility = View.GONE
        } else {
            super.onBackPressed()
        }
    }
}