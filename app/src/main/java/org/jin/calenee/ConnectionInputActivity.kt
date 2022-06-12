package org.jin.calenee

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import androidx.lifecycle.*
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import org.jin.calenee.database.firestore.User
import org.jin.calenee.databinding.ActivityConnectionInputBinding

class ConnectionInputActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityConnectionInputBinding.inflate(layoutInflater)
    }

    private val profileViewModel by lazy {
        ViewModelProvider(this)[ProfileViewModel::class.java]
    }

    private val user = User()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        listener()
        observer()
    }

    private fun observer() {
        profileViewModel.gender.observe(this) {
            user.gender = it
        }

        profileViewModel.nickname.observe(this) {
            user.nickname = it
        }

        profileViewModel.birthday.observe(this) {
            user.birthday = it
        }

        profileViewModel.firstMetDate.observe(this) {
            user.firstMetDate = it
        }
    }

    private fun listener() {
        editTextListener(binding.inputNicknameEt)
        editTextListener(binding.inputBirthdayEt)
        editTextListener(binding.inputFirstMetDateEt)

        binding.inputNicknameEt.disableSelection("nickname")
        binding.inputBirthdayEt.disableSelection("birthday")
        binding.inputFirstMetDateEt.disableSelection("firstMetDate")

        binding.genderRadioGroup.setOnCheckedChangeListener { _, id ->
            when (id) {
                R.id.female_radio_btn -> profileViewModel.setGender("female")
                R.id.male_radio_btn -> profileViewModel.setGender("male")
                else -> profileViewModel.setGender("female")
            }
        }

//        binding.startBtn.setOnClickListener {
//            Log.d("input_test", user.toString())
//        }
    }

    private fun TextInputEditText.disableSelection(keyword: String) {
        this.setOnKeyListener { _, _, keyEvent ->
            val length: Int = when (keyword) {
                "nickname" -> profileViewModel.nickname.value!!.length
                "birthday" -> profileViewModel.birthday.value!!.length
                "firstMetDate" -> profileViewModel.firstMetDate.value!!.length
                else -> this.length()
            }

            if (keyEvent.action == KeyEvent.ACTION_DOWN) { this.setSelection(length) }

            return@setOnKeyListener false
        }
    }

    var previousLength = 0
    var backspace: Boolean = false
    private fun editTextListener(editText: TextInputEditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                previousLength = "$text".length
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(text: Editable?) {
                backspace = previousLength > "$text".length

                when (editText.id) {
                    R.id.input_nickname_et -> {
                        profileViewModel.setNickname("$text")
                    }
                    R.id.input_birthday_et -> {
                        if (!backspace && "$text".length == 4) text?.append("-")
                        else if (!backspace && "$text".length == 7) text?.append("-")
                        profileViewModel.setBirthday("$text")
                    }
                    R.id.input_first_met_date_et -> {
                        if (!backspace && "$text".length == 4) text?.append("-")
                        else if (!backspace && "$text".length == 7) text?.append("-")
                        profileViewModel.setFirstMetDate("$text")
                    }
                }
            }
        })
    }
}

class ProfileViewModel : ViewModel() {
    private val _gender = MutableLiveData<String>()
    val gender: LiveData<String> get() = _gender

    private val _nickname = MutableLiveData<String>()
    val nickname: LiveData<String> get() = _nickname

    private val _birthday = MutableLiveData<String>()
    val birthday: LiveData<String> get() = _birthday

    private val _firstMetDate = MutableLiveData<String>()
    val firstMetDate: LiveData<String> get() = _firstMetDate

    init {
        this._gender.value = "female"
        this._nickname.value = ""
        this._birthday.value = ""
        this._firstMetDate.value = ""

    }

    fun setGender(inputGender: String) = viewModelScope.launch {
        _gender.value = inputGender
    }

    fun setNickname(inputNickname: String) = viewModelScope.launch {
        _nickname.value = inputNickname
    }

    fun setBirthday(inputBirth: String) = viewModelScope.launch {
        _birthday.value = inputBirth
    }

    fun setFirstMetDate(inputDate: String) = viewModelScope.launch {
        _firstMetDate.value = inputDate
    }

}