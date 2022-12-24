package org.jin.calenee.home

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
                updateData()

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
                            textSizeSampleTv.textSize = 12f
                            0
                        }
                        R.id.size_medium_btn -> {
                            textSizeSampleTv.textSize = 14f
                            1
                        }
                        R.id.size_large_btn -> {
                            textSizeSampleTv.textSize = 16f
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

    private fun updateData() {
        Firebase.firestore.collection("coupleInfo")
            .document(App.userPrefs.getString("couple_chat_id")).apply {
                update("user${coupleInfo.position}MessagePosition", messagePosition)
                update("user${coupleInfo.position}MessageAlignment", messageAlignment)
                update("user${coupleInfo.position}MessageSize", messageSize)
                update("user${coupleInfo.position}MessageColor", messageColor)
            }
    }
}