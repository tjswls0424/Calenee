package org.jin.calenee.home

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.dialog_date_picker.view.*
import org.jin.calenee.App
import org.jin.calenee.R
import org.jin.calenee.databinding.ActivityEditCoupleInfoBinding
import org.jin.calenee.databinding.DialogDatePickerBinding
import java.lang.IndexOutOfBoundsException
import java.text.SimpleDateFormat
import java.util.*

const val DAYS = 1
const val BIRTHDAY = 2
const val NICKNAME = 3
const val MESSAGE = 4

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
        with(intent) {
            coupleInfo = getSerializableExtra("coupleInfo") as CoupleInfo
            position = getIntExtra("position", 0)
        }

        with(binding) {
            couple = coupleInfo
            companion = EditCoupleInfoActivity.Companion
        }

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

    private fun refreshView() {
        binding.couple = coupleInfo
    }

    private fun listener() {
        val currentDate = Calendar.getInstance().timeInMillis
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(currentDate)
        val tmpDate = date.split("-")

        binding.firstMetDateRow.setOnClickListener {
            try {
                with(coupleInfo.firstMetDate.split("-")) {
                    setDatePicker("변경할 처음 만난 날짜를 입력해주세요", get(0).toInt(), get(1).toInt(), get(2).toInt())
                }
            } catch (e: IndexOutOfBoundsException) {
                with(tmpDate) {
                    setDatePicker("변경할 처음 만난 날짜를 입력해주세요", get(0).toInt(), get(1).toInt(), get(2).toInt())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        if (enableFlag1) {
            binding.user1Row.setOnClickListener {
                Log.d("row_test", "user1Row")
            }
            binding.user1BirthdayRow.setOnClickListener {
                try {
                    with(coupleInfo.user1Birthday.split("-")) {
                        setDatePicker("${coupleInfo.user1Nickname}님의 생일을 입력해주세요", get(0).toInt(), get(1).toInt(), get(2).toInt(), 1)
                    }
                } catch (e: IndexOutOfBoundsException) {
                    with (tmpDate) {
                        setDatePicker("${coupleInfo.user1Nickname}님의 생일을 입력해주세요", get(0).toInt(), get(1).toInt(), get(2).toInt(), 1)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
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
                try {
                    with(coupleInfo.user2Birthday.split("-")) {
                        setDatePicker("${coupleInfo.user2Nickname}님의 생일을 입력해주세요", get(0).toInt(), get(1).toInt(), get(2).toInt(), 2)
                    }
                } catch (e: IndexOutOfBoundsException) {
                    with (tmpDate) {
                        setDatePicker("${coupleInfo.user2Nickname}님의 생일을 입력해주세요", get(0).toInt(), get(1).toInt(), get(2).toInt(), 2)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
            binding.user2MessageRow.setOnClickListener {
                Log.d("row_test", "user2MessageRow")
            }
        }
    }

    private fun setDatePicker(
        title: String,
        previousYear: Int,
        previousMonth: Int,
        previousDay: Int,
        position: Int = 0
    ) {
        val dialog = AlertDialog.Builder(this).create()
        val edialog = LayoutInflater.from(this)
        val mView = edialog.inflate(R.layout.dialog_date_picker, null)

        val currentDate = Calendar.getInstance().timeInMillis

        mView.dialog_title_tv.text = title
        dialog.setView(mView)
        dialog.show()

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

            year_np.value = previousYear
            month_np.value = previousMonth
            day_np.value = previousDay


            val yearMinValue = 1900
            val yearMaxValue = SimpleDateFormat("yyyy", Locale.KOREA).format(currentDate).toInt()
            val yearSize = yearMaxValue - yearMinValue + 1
            year_np.setOnValueChangedListener { picker, oldVal, newVal ->
                Log.d("cal_test/year-new", newVal.toString())
            }

            month_np.setOnValueChangedListener { picker, oldVal, newVal ->
                if (month_np.value == 2) {
                    day_np.maxValue = 28
                } else {
                    day_np.maxValue = 31
                }
            }

            day_np.setOnValueChangedListener { picker, oldVal, newVal ->
                if (month_np.value == 2) {
                    day_np.maxValue = 28
                } else {
                    day_np.maxValue = 31
                }
            }


            year_np.displayedValues = Array(yearSize) { i -> (i + yearMinValue).toString() + "년" }
            month_np.displayedValues = Array(12) { i -> (i + 1).toString() + "월" }
            day_np.displayedValues = Array(31) { i -> (i + 1).toString() + "일" }

            cancel_btn.setOnClickListener {
                dialog.dismiss()
                dialog.cancel()
            }
            save_btn.setOnClickListener {
//                val result = "${year_np.value-month_np.value-day_np.value}"
                val result = "${year_np.value}-${month_np.value}-${day_np.value}"
                when (position) {
                    1 -> {
                        coupleInfo.user1Birthday = result
                        updateData(BIRTHDAY, position)
                    }
                    2 -> {
                        coupleInfo.user2Birthday = result
                        updateData(BIRTHDAY, position)
                    }
                    else -> {
                        coupleInfo.firstMetDate = result
                        updateData(DAYS)
                    }
                }

                refreshView()
                dialog.dismiss()
            }
        }
    }

    private fun updateData(type: Int, position: Int = 0) {
        val doc = Firebase.firestore.collection("coupleInfo").document(App.userPrefs.getString("couple_chat_id"))
        when (type) {
            DAYS -> {
                doc.update("firstMetDate", coupleInfo.firstMetDate)
            }

            BIRTHDAY -> {
                when (position) {
                    1 -> doc.update("user${position}Birthday", coupleInfo.user1Birthday)
                    2 -> doc.update("user${position}Birthday", coupleInfo.user2Birthday)
                }
            }

            NICKNAME -> {
                when (position) {
                    1 -> doc.update("user${position}Nickname", coupleInfo.user1Nickname)
                    2 -> doc.update("user${position}Nickname", coupleInfo.user2Nickname)
                }
            }

            MESSAGE -> {
                when (position) {
                    1 -> doc.update("user${position}Message", coupleInfo.user1Message)
                    2 -> doc.update("user${position}Message", coupleInfo.user2Message)
                }
            }
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