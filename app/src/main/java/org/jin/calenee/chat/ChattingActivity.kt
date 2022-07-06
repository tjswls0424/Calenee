package org.jin.calenee.chat

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.jin.calenee.databinding.ActivityChattingBinding
import java.lang.RuntimeException
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

    private lateinit var chatAdapter: ChatAdapter

    private var isKeyboardShown: Boolean = false
    private var chatDataList: MutableList<ChatData> = mutableListOf()
    private var message: String = ""

//    override fun onStart() {
//        super.onStart()
//
//        // lottie button click does not work at once
//        binding.lottieAddCloseBtn.performClick()
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        listener()
        setContentView(binding.root)
    }

    private fun initRecycler() {
        chatAdapter = ChatAdapter().apply {
            binding.chatRecyclerView.adapter = this
            data = chatDataList
            notifyDataSetChanged()
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
                ChatData(
                    nickname = getNickname(),
                    message = message,
                    time = getCurrentTimeStamp(TIME),
                    viewType = 1
                ).also {
                    chatDataList.add(it)
                    saveChatData(it)
                }

                initRecycler()
                binding.messageEt.setText("")

                LinearLayoutManager(applicationContext).apply {
                    stackFromEnd = true
                    binding.chatRecyclerView.layoutManager = this
                }
            }
        }

        binding.messageEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(text: Editable?) {
                message = "$text"
            }
        })
    }

    private fun getNickname(): String {
        var nickname = ""
        firestore.collection("user").document(currentUserEmail)
            .get()
            .addOnSuccessListener { doc ->
                Log.d("db_test/login-doc", doc.data.toString())
                nickname = doc.data?.get("nickname").toString()
            }
            .addOnFailureListener {
                Log.d("db_test/login-err", "${it.printStackTrace()}")
            }

        return nickname
    }

    private fun saveChatData(data: ChatData) {
        val path = getCurrentTimeStamp(DATE_TIME)
        val myRef = FirebaseDatabase.getInstance().getReference(path)
        myRef.apply {
            child("senderNickname").setValue(data.nickname)
            child("senderEmail").setValue(currentUserEmail)
            child("message").setValue(data.message)
            child("createdAt").setValue(data.time)
            child("read").setValue(false)
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
            DATE_TIME -> SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.KOREA).format(System.currentTimeMillis())
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