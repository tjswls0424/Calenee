package org.jin.calenee.home

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.jin.calenee.databinding.ActivityTodayMessagePositionBinding

class TodayMessagePositionActivity : AppCompatActivity() {

    private val binding: ActivityTodayMessagePositionBinding by lazy {
        ActivityTodayMessagePositionBinding.inflate(layoutInflater)
    }

    private lateinit var coupleInfo: CoupleInfo
    private var messagePosition: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        coupleInfo = intent.getSerializableExtra("coupleInfo") as CoupleInfo
        binding.coupleInfo = coupleInfo

        listener()
    }

    private fun listener() {
        binding.cancelBtn.setOnClickListener {
            onBackPressed()
        }
        binding.nextBtn.setOnClickListener {
            Intent(this, TodayMessageTextInfoActivity::class.java).apply {
                putExtra("coupleInfo", coupleInfo)
                putExtra("messagePosition", messagePosition)
                startActivity(this)
            }
        }

        with(binding) {
            positionBtn0.setOnClickListener {
                unCheckedAll()
                messagePosition = 0
                positionBtn0.isChecked = true
            }
            positionBtn1.setOnClickListener {
                unCheckedAll()
                messagePosition = 1
                positionBtn1.isChecked = true
            }
            positionBtn2.setOnClickListener {
                unCheckedAll()
                messagePosition = 2
                positionBtn2.isChecked = true
            }
            positionBtn3.setOnClickListener {
                unCheckedAll()
                messagePosition = 3
                positionBtn3.isChecked = true
            }
            positionBtn4.setOnClickListener {
                unCheckedAll()
                messagePosition = 4
                positionBtn4.isChecked = true
            }
            positionBtn5.setOnClickListener {
                unCheckedAll()
                messagePosition = 5
                positionBtn5.isChecked = true
            }
            positionBtn6.setOnClickListener {
                unCheckedAll()
                messagePosition = 6
                positionBtn6.isChecked = true
            }
        }
    }

    private fun unCheckedAll() {
        with(binding) {
            positionBtn0.isChecked = false
            positionBtn1.isChecked = false
            positionBtn2.isChecked = false
            positionBtn3.isChecked = false
            positionBtn4.isChecked = false
            positionBtn5.isChecked = false
            positionBtn6.isChecked = false
        }
    }
}