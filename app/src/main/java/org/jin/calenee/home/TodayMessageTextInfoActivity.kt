package org.jin.calenee.home

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.jin.calenee.App
import org.jin.calenee.MainActivity.Companion.slideLeft
import org.jin.calenee.R
import org.jin.calenee.databinding.ActivityTodayMessageTextInfoBinding

class TodayMessageTextInfoActivity : AppCompatActivity() {

    private val binding: ActivityTodayMessageTextInfoBinding by lazy {
        ActivityTodayMessageTextInfoBinding.inflate(layoutInflater)
    }

    private lateinit var coupleInfo: CoupleInfo
    private var messagePosition: Int = 0
    private var messageAlignment: Int = 0 // left: 0, center: 1, right: 2
    private var messageSize: Int = 0 // small: 0, medium: 1, large: 2
    private var messageColor: Int = 0 // white: 0, grey: 1, black: 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        messagePosition = intent.getIntExtra("messagePosition", 0)
        coupleInfo = intent.getSerializableExtra("coupleInfo") as CoupleInfo

        listener()
    }

    private fun listener() {
        with(binding) {
            previousBtn.setOnClickListener {
                onBackPressed()
            }

            saveBtn.setOnClickListener {
                updateData() // to Firestore
                saveData() // to SP

                Intent(
                    this@TodayMessageTextInfoActivity,
                    EditCoupleInfoActivity::class.java
                ).apply {
                    putExtra("coupleInfo", coupleInfo)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(this)
                    slideLeft()
                    finish()
                }
            }

            textAlignmentToggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
                if (isChecked) {
                    messageAlignment = when (checkedId) {
                        R.id.align_left_btn -> 0
                        R.id.align_center_btn -> 1
                        R.id.align_right_btn -> 2
                        else -> 0
                    }
                }
            }

            textSizeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    messageSize = when (checkedId) {
                        R.id.size_small_btn -> {
                            textSizeSampleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                            0
                        }
                        R.id.size_medium_btn -> {
                            textSizeSampleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                            1
                        }
                        R.id.size_large_btn -> {
                            textSizeSampleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                            2
                        }
                        else -> 0
                    }
                }
            }

            textColorToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    messageColor = when (checkedId) {
                        R.id.text_white_btn -> 0
                        R.id.text_grey_btn -> 1
                        R.id.text_black_btn -> 2
                        else -> 0
                    }
                }
            }

        }
    }

    // to Firestore
    private fun updateData() {
        Firebase.firestore.collection("coupleInfo")
            .document(App.userPrefs.getString("couple_chat_id")).apply {
                when (coupleInfo.position) {
                    1 -> update("user${coupleInfo.position}Message", coupleInfo.user1Message)
                    2 -> update("user${coupleInfo.position}Message", coupleInfo.user2Message)
                }

                update("user${coupleInfo.position}MessagePosition", messagePosition)
                update("user${coupleInfo.position}MessageAlignment", messageAlignment)
                update("user${coupleInfo.position}MessageSize", messageSize)
                update("user${coupleInfo.position}MessageColor", messageColor)
            }
    }

    // to SP
    private fun saveData() {
        with(App.userPrefs) {
            when (coupleInfo.position) {
                1 -> updateTodayMessageInfo(coupleInfo.position, coupleInfo.user1Message, messagePosition, messageAlignment, messageSize, messageColor)
                2 -> updateTodayMessageInfo(coupleInfo.position, coupleInfo.user2Message, messagePosition, messageAlignment, messageSize, messageColor)
            }
        }
    }
}