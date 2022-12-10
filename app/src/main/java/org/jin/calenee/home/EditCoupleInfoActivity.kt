package org.jin.calenee.home

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import kotlinx.android.synthetic.main.dialog_date_picker.view.*
import org.jin.calenee.R
import org.jin.calenee.databinding.ActivityEditCoupleInfoBinding
import org.jin.calenee.databinding.DialogDatePickerBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class EditCoupleInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditCoupleInfoBinding
    private lateinit var pickerBinding: DialogDatePickerBinding

    private lateinit var coupleInfo: CoupleInfo
    private var position: Int = 0

    companion object {
        var enableFlag1 = false
        var enableFlag2 = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_edit_couple_info)
        pickerBinding = DialogDatePickerBinding.inflate(layoutInflater)

        initData()
        initToolbar()
        listener()
    }

    private fun initData() {
        intent.apply {
            coupleInfo = getSerializableExtra("coupleInfo") as CoupleInfo
            position = getIntExtra("position", 0)
        }

        binding.companion = EditCoupleInfoActivity.Companion
        binding.coupleInfo = coupleInfo
        makeViewEditable()
    }

    private fun makeViewEditable() = when (position) {
        1 -> enableFlag1 = true
        2 -> enableFlag2 = true

        else -> {
            enableFlag1 = false
            enableFlag2 = false
        }
    }

    private fun listener() {
        binding.firstMetDateRow.setOnClickListener {
            coupleInfo.firstMetDate.split("-").apply {
                setDatePicker("변경할 처음 만난 날짜를 입력해주세요", get(0).toInt(), get(1).toInt(), get(2).toInt())
            }
        }

        if (enableFlag1) {
            binding.user1Row.setOnClickListener {
                Log.d("row_test", "user1Row")
            }
            binding.user1BirthdayRow.setOnClickListener {
                coupleInfo.user1Birthday.split("-").apply {
                    setDatePicker("${coupleInfo.user1Nickname}님의 생일을 입력해주세요", get(0).toInt(), get(1).toInt(), get(2).toInt())
                }
            }
            binding.user1MessageRow.setOnClickListener {
                Log.d("row_test", "user1MessageRow")
            }
        }

        if (enableFlag2) {
            binding.user2Row.setOnClickListener {
                Log.d("row_test", "user2Row")
            }
            binding.user2BirthdayRow.setOnClickListener {
                coupleInfo.user2Birthday.split("-").apply {
                    setDatePicker("${coupleInfo.user2Nickname}님의 생일을 입력해주세요", get(0).toInt(), get(1).toInt(), get(2).toInt())
                }

            }
            binding.user2MessageRow.setOnClickListener {
                Log.d("row_test", "user2MessageRow")
            }

        }
    }

    private fun setDatePicker(title: String, previousYear: Int, previousMonth: Int, previousDay: Int) {
        val dialog = AlertDialog.Builder(this).create()
        val edialog = LayoutInflater.from(this)
        val mView = edialog.inflate(R.layout.dialog_date_picker, null)

        val currentDate = Calendar.getInstance().timeInMillis

        mView.dialog_title_tv.text = title
        dialog.setView(mView)

        with(mView) {
            // 순환 X
            year_np.wrapSelectorWheel = false
            month_np.wrapSelectorWheel = false
            day_np.wrapSelectorWheel = false

            year_np.minValue = 1900
            month_np.minValue = 1
            day_np.minValue = 1

            // todo: 월별로 최대 일수 제한

            year_np.maxValue = SimpleDateFormat("yyyy", Locale.KOREA).format(currentDate).toInt()
            month_np.maxValue = 12
            day_np.maxValue = 31

            year_np.value = previousYear.toInt()
            month_np.value = previousMonth.toInt()
            day_np.value = previousDay.toInt()


            val yearMinValue = 1900
            val yearMaxValue = SimpleDateFormat("yyyy", Locale.KOREA).format(currentDate).toInt()
            val yearSize = yearMaxValue-yearMinValue+1
            year_np.setOnValueChangedListener { picker, oldVal, newVal ->
                Log.d("cal_test/year-new", newVal.toString())
            }

            month_np.setOnValueChangedListener { picker, oldVal, newVal ->

            }

            day_np.setOnValueChangedListener { picker, oldVal, newVal ->

            }

            year_np.displayedValues = Array(yearSize){i -> (i+yearMinValue).toString()+"년"}
            month_np.displayedValues = Array(12){i -> (i+1).toString()+"월"}
            day_np.displayedValues = Array(31){i -> (i+1).toString()+"일"}
        }

        dialog.show()

        mView.cancel_btn.setOnClickListener {
            dialog.dismiss()
            dialog.cancel()
        }
        mView.save_btn.setOnClickListener {

        }

    }

    private fun initToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }

        return super.onOptionsItemSelected(item)
    }
}