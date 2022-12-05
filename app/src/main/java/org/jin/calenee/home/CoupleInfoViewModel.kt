package org.jin.calenee.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class CoupleInfoViewModel : ViewModel() {
    private val _nickname1 = MutableLiveData<String>()
    val nickname1: LiveData<String> get() = _nickname1

    private val _nickname2 = MutableLiveData<String>()
    val nickname2: LiveData<String> get() = _nickname2

    private val _firstMetDate = MutableLiveData<String>()
    val firstMetDate: LiveData<String> get() = _firstMetDate

    private val _days = MutableLiveData<String>()
    val days: LiveData<String> get() = _days

    private val _birthday1 = MutableLiveData<String>()
    val birthday1: LiveData<String> get() = _birthday1

    private val _birthday2 = MutableLiveData<String>()
    val birthday2: LiveData<String> get() = _birthday2

    private val _message1 = MutableLiveData<String>()
    val message1: LiveData<String> get() = _message1

    private val _message2 = MutableLiveData<String>()
    val message2: LiveData<String> get() = _message2

    init {
        _nickname1.value = ""
        _nickname2.value = ""
        _firstMetDate.value = ""
        _days.value = ""
        _birthday1.value = ""
        _birthday2.value = ""
        _message1.value = ""
        _message2.value = ""
    }

    fun updateNickname(nickname: String, position: Int) {
        when (position) {
            1 -> _nickname1.value = nickname
            2 -> _nickname2.value = nickname
        }
    }

    fun updateBirthday(birthday: String, position: Int) {
        when (position) {
            1 -> _birthday1.value = birthday
            2 -> _birthday2.value = birthday
        }
    }

    fun updateMessage(message: String, position: Int) {
        when (position) {
            1 -> _message1.value = message
            2 -> _message2.value = message
        }
    }

    fun updateDays(days: String) {
        _firstMetDate.value = days

        val firstMetDate = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).parse(days)
        val currentDate = Calendar.getInstance().time
        val diff = currentDate.time - firstMetDate!!.time
        val resDays = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).plus(1).toString()

        _days.value = "${resDays}일"
    }

    fun getCoupleInfo(): CoupleInfo {
        return CoupleInfo(
            nickname1.value.toString(),
            nickname2.value.toString(),
            birthday1.value.toString(),
            birthday2.value.toString(),
            message1.value.toString(),
            message2.value.toString(),
            firstMetDate.value.toString(),
        )
    }
}