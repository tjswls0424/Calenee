package org.jin.calenee.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CoupleInfoViewModel : ViewModel() {
    private val _nickname1 = MutableLiveData<String>()
    val nickname1: LiveData<String> get() = _nickname1

    private val _nickname2 = MutableLiveData<String>()
    val nickname2: LiveData<String> get() = _nickname2

    private val _days = MutableLiveData<String>()
    val days: LiveData<String> get() = _days

    init {
        _nickname1.value = ""
        _nickname2.value = ""
        _days.value = ""
    }

    fun updateValue(nickname: String, position: Int) {
        when (position) {
            1 -> _nickname1.value = nickname
            2 -> _nickname2.value = nickname
        }
    }

    fun updateDays(days: String) {
        _days.value = days
    }
}