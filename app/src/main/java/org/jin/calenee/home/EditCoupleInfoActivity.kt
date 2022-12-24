package org.jin.calenee.home

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.dialog_date_picker.view.*
import kotlinx.android.synthetic.main.dialog_date_picker.view.dialog_title_tv
import kotlinx.android.synthetic.main.dialog_date_picker.view.save_btn
import kotlinx.android.synthetic.main.dialog_edit_text.view.*
import org.jin.calenee.App
import org.jin.calenee.R
import org.jin.calenee.databinding.ActivityEditCoupleInfoBinding
import org.jin.calenee.databinding.DialogDatePickerBinding
import java.lang.IndexOutOfBoundsException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

const val DAYS = 1
const val BIRTHDAY = 2
const val NICKNAME = 3
const val MESSAGE = 4

class EditCoupleInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditCoupleInfoBinding
    private lateinit var pickerBinding: DialogDatePickerBinding

    private lateinit var coupleInfo: CoupleInfo

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
        }

        with(binding) {
            couple = coupleInfo
            companion = EditCoupleInfoActivity.Companion
        }

        makeViewEditable()
    }

    private fun makeViewEditable() = when (coupleInfo.position) {
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
                    setDatePicker(
                        "변경할 처음 만난 날짜를 입력해주세요",
                        get(0).toInt(),
                        get(1).toInt(),
                        get(2).toInt()
                    )
                }
            } catch (e: IndexOutOfBoundsException) {
                with(tmpDate) {
                    setDatePicker(
                        "변경할 처음 만난 날짜를 입력해주세요",
                        get(0).toInt(),
                        get(1).toInt(),
                        get(2).toInt()
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }



        if (enableFlag1) {
            binding.user1Row.setOnClickListener {
                setEditTextDialog("변경할 닉네임을 입력해주세요")
            }
            binding.user1BirthdayRow.setOnClickListener {
                try {
                    with(coupleInfo.user1Birthday.split("-")) {
                        setDatePicker(
                            "${coupleInfo.user1Nickname}님의 생일을 입력해주세요",
                            get(0).toInt(),
                            get(1).toInt(),
                            get(2).toInt(),
                        )
                    }
                } catch (e: IndexOutOfBoundsException) {
                    with(tmpDate) {
                        setDatePicker(
                            "${coupleInfo.user1Nickname}님의 생일을 입력해주세요",
                            get(0).toInt(),
                            get(1).toInt(),
                            get(2).toInt(),
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            binding.user1MessageRow.setOnClickListener {
                setEditTextDialog("오늘의 한마디를 입력해주세요", true)
            }
        }

        if (enableFlag2) {
            binding.user2Row.setOnClickListener {
                setEditTextDialog("변경할 닉네임을 입력해주세요")
            }
            binding.user2BirthdayRow.setOnClickListener {
                try {
                    with(coupleInfo.user2Birthday.split("-")) {
                        setDatePicker(
                            "${coupleInfo.user2Nickname}님의 생일을 입력해주세요",
                            get(0).toInt(),
                            get(1).toInt(),
                            get(2).toInt(),
                        )
                    }
                } catch (e: IndexOutOfBoundsException) {
                    with(tmpDate) {
                        setDatePicker(
                            "${coupleInfo.user2Nickname}님의 생일을 입력해주세요",
                            get(0).toInt(),
                            get(1).toInt(),
                            get(2).toInt(),
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
            binding.user2MessageRow.setOnClickListener {
                setEditTextDialog("오늘의 한마디를 입력해주세요", true)
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
                // todo: 월별 일수 제한
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
                when (coupleInfo.position) {
                    1 -> {
                        coupleInfo.user1Birthday = result
                        updateData(BIRTHDAY, coupleInfo.position)
                    }
                    2 -> {
                        coupleInfo.user2Birthday = result
                        updateData(BIRTHDAY, coupleInfo.position)
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

    private fun setEditTextDialog(title: String, subTextFlag: Boolean = false) {
        val dialog = AlertDialog.Builder(this).create()
        val edialog = LayoutInflater.from(this)
        val mView = edialog.inflate(R.layout.dialog_edit_text, null)

        mView.dialog_title_tv.text = title
        dialog.setView(mView)
        dialog.show()

        with(mView) {
            if (subTextFlag) {
                // EditText Dialog for today message
                dialog_sub_tv.visibility = View.VISIBLE
                input_text_layout.hint = "오늘의 한마디"

                if (coupleInfo.position == 1) {
                    if (coupleInfo.user1Message.isNotEmpty()) {
                        input_text_layout.editText?.setText(coupleInfo.user1Message)
                    }
                } else {
                    if (coupleInfo.user2Message.isNotEmpty()) {
                        input_text_layout.editText?.setText(coupleInfo.user2Message)
                    }
                }
            } else {
                // EditText Dialog for nickname
                if (coupleInfo.position == 1) {
                    if (coupleInfo.user1Nickname.isNotEmpty()) {
                        input_text_layout.editText?.setText(coupleInfo.user1Nickname)
                    }
                } else {
                    if (coupleInfo.user2Nickname.isNotEmpty()) {
                        input_text_layout.editText?.setText(coupleInfo.user2Nickname)
                    }
                }
            }

            input_text_layout.editText?.doOnTextChanged { text, start, before, count ->
                when (coupleInfo.position) {
                    1 -> {
                        if (subTextFlag) coupleInfo.user1Message = text.toString()
                        else coupleInfo.user1Nickname = text.toString()
                    }

                    2 -> {
                        if (subTextFlag) coupleInfo.user2Message = text.toString()
                        else coupleInfo.user2Nickname = text.toString()
                    }
                }
                Log.d("et_test", text.toString())
            }

            et_cancel_btn.setOnClickListener {
                dialog.dismiss()
                dialog.cancel()
            }

            et_save_btn.setOnClickListener {
                if (subTextFlag) {
                    // today message
                    updateData(MESSAGE, coupleInfo.position)

                    Intent(this@EditCoupleInfoActivity, TodayMessagePositionActivity::class.java).apply {
                        calculateDays()
                        putExtra("coupleInfo", coupleInfo)
                        startActivity(this)
                    }
                } else {
                    // nickname
                    updateData(NICKNAME, coupleInfo.position)
                }

                refreshView()
                dialog.dismiss()
            }
        }
    }

    private fun calculateDays() {
        val firstMetDate = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).parse(coupleInfo.firstMetDate)
        val currentDate = Calendar.getInstance().time
        val diff = currentDate.time - firstMetDate!!.time
        val resDays = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).plus(1).toString()

        coupleInfo.days = resDays + "일"
    }

    private fun updateData(type: Int, position: Int = 0) {
        val doc = Firebase.firestore.collection("coupleInfo")
            .document(App.userPrefs.getString("couple_chat_id"))
        when (type) {
            DAYS -> {
                doc.update("firstMetDate", coupleInfo.firstMetDate)
            }

            BIRTHDAY -> {
                when (coupleInfo.position) {
                    1 -> doc.update("user${coupleInfo.position}Birthday", coupleInfo.user1Birthday)
                    2 -> doc.update("user${coupleInfo.position}Birthday", coupleInfo.user2Birthday)
                }
            }

            NICKNAME -> {
                when (coupleInfo.position) {
                    1 -> doc.update("user${coupleInfo.position}Nickname", coupleInfo.user1Nickname)
                    2 -> doc.update("user${coupleInfo.position}Nickname", coupleInfo.user2Nickname)
                }
            }

            MESSAGE -> {
                when (coupleInfo.position) {
                    1 -> {
                        doc.update("user${coupleInfo.position}Message", coupleInfo.user1Message)
                        doc.update("user${coupleInfo.position}MessagePosition", 0)
                    }
                    2 -> {
                        doc.update("user${coupleInfo.position}Message", coupleInfo.user2Message)
                        doc.update("user${coupleInfo.position}MessagePosition", 0)
                    }
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