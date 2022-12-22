package org.jin.calenee.home

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.jin.calenee.databinding.ActivityTodayMessagePositionBinding

class TodayMessagePositionActivity : AppCompatActivity() {

    private val binding: ActivityTodayMessagePositionBinding by lazy {
        ActivityTodayMessagePositionBinding.inflate(layoutInflater)
    }

    private lateinit var coupleInfo: CoupleInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        coupleInfo = intent.getSerializableExtra("coupleInfo") as CoupleInfo
        binding.coupleInfo = coupleInfo

        listener()
    }

    private fun listener() {
        with(binding) {
            position0Tb.setOnClickListener {
                unCheckedAll()
                position0Tb.isChecked = true
            }
            position1Tb.setOnClickListener {
                unCheckedAll()
                position1Tb.isChecked = true
            }
            position2Tb.setOnClickListener {
                unCheckedAll()
                position2Tb.isChecked = true
            }
            position3Tb.setOnClickListener {
                unCheckedAll()
                position3Tb.isChecked = true
            }
            position4Tb.setOnClickListener {
                unCheckedAll()
                position4Tb.isChecked = true
            }
            position5Tb.setOnClickListener {
                unCheckedAll()
                position5Tb.isChecked = true
            }
            position6Tb.setOnClickListener {
                unCheckedAll()
                position6Tb.isChecked = true
            }
            position7Tb.setOnClickListener {
                unCheckedAll()
                position7Tb.isChecked = true
            }
            position8Tb.setOnClickListener {
                unCheckedAll()
                position8Tb.isChecked = true
            }
            position9Tb.setOnClickListener {
                unCheckedAll()
                position9Tb.isChecked = true
            }

        }
    }

    private fun unCheckedAll() {
        with(binding) {
            position0Tb.isChecked = false
            position1Tb.isChecked = false
            position2Tb.isChecked = false
            position3Tb.isChecked = false
            position4Tb.isChecked = false
            position5Tb.isChecked = false
            position6Tb.isChecked = false
            position7Tb.isChecked = false
            position8Tb.isChecked = false
            position9Tb.isChecked = false
        }
    }

}