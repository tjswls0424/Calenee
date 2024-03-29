package org.jin.calenee

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*
import org.jin.calenee.MainActivity.Companion.slideRight
import org.jin.calenee.data.firestore.User
import org.jin.calenee.databinding.ActivityConnectionInputBinding
import java.io.ByteArrayOutputStream
import java.io.File

class ConnectionInputActivity : AppCompatActivity() {
    companion object {
        lateinit var defaultProfileImage: Bitmap
    }

    private val GALLERY = 1

    private val binding by lazy {
        ActivityConnectionInputBinding.inflate(layoutInflater)
    }

    private val profileViewModel by lazy {
        ViewModelProvider(this).get(ProfileViewModel::class.java)
    }

    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val firestore by lazy {
        Firebase.firestore
    }

    private val myEmail by lazy {
        firebaseAuth.currentUser?.email.toString()
    }

//    private lateinit var partnerEmail: String

    private val user = User()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        defaultProfileImage = binding.profileImg.drawable.toBitmap()

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

        profileViewModel.profileImage.observe(this) {
            user.profileImage = it
            binding.profileImg.setImageBitmap(it)
        }
    }

    private fun listener() {
        profileInputListener()

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

        binding.profileImg.setOnClickListener {
            getImageFromGallery()
        }
        binding.changeProfileText.setOnClickListener {
            getImageFromGallery()
        }

        binding.startBtn.setOnClickListener {
            if (checkInputCondition()) {
                binding.loadingScreen.visibility = View.VISIBLE
                binding.progressBar.visibility = View.VISIBLE

                updateFirstMetDate()
                updateProfileInfo()
                updateProfileImage()
            } else {
                Snackbar.make(binding.root, "프로필에 들어갈 정보를 모두 입력해주세요", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun TextInputEditText.disableSelection(keyword: String) {
        this.setOnKeyListener { _, _, keyEvent ->
            val length: Int = when (keyword) {
                "nickname" -> profileViewModel.nickname.value!!.length
                "birthday" -> profileViewModel.birthday.value!!.length
                "firstMetDate" -> profileViewModel.firstMetDate.value!!.length
                else -> this.length()
            }

            if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                this.setSelection(length)
            }

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
                        binding.inputNicknameLayout.error = null
                    }
                    R.id.input_birthday_et -> {
                        if (!backspace && "$text".length == 4) text?.append("-")
                        else if (!backspace && "$text".length == 7) text?.append("-")
                        profileViewModel.setBirthday("$text")
                        binding.inputBirthdayLayout.error = null
                    }
                    R.id.input_first_met_date_et -> {
                        if (!backspace && "$text".length == 4) text?.append("-")
                        else if (!backspace && "$text".length == 7) text?.append("-")
                        profileViewModel.setFirstMetDate("$text")
                        binding.inputFirstMetDateLayout.error = null
                    }
                }
            }
        })
    }

    private fun checkInputCondition(): Boolean {
        with(binding) {
            return when {
                profileViewModel.nickname.value?.length?.compareTo(2) == -1 -> {
                    inputNicknameLayout.apply {
                        error = "닉네임을 2글자 이상 입력해주세요"
                        requestFocus()
                    }
                    false
                }
                profileViewModel.birthday.value?.length?.compareTo(10) == -1 -> {
                    inputBirthdayLayout.apply {
                        error = "생년월일 8자리를 입력해주세요"
                        requestFocus()
                    }
                    false
                }
                profileViewModel.firstMetDate.value?.length?.compareTo(10) == -1 -> {
                    inputFirstMetDateLayout.apply {
                        error = "처음 만난 날짜 8자리를 입력해주세요"
                        requestFocus()
                    }
                    false
                }

                else -> true
            }
        }
    }

    private fun profileInputListener() {
        firestore.collection("user").document(myEmail)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot != null && snapshot.exists()) {
                    if (snapshot.data?.get("profileInputFlag") as Boolean && snapshot.data?.get("profileImageFlag") as Boolean) {
                        binding.loadingScreen.visibility = View.GONE
                        binding.progressBar.visibility = View.GONE

                        saveCoupleInfo()
                    }
                }
            }
    }

    private fun saveCoupleInfo() {
        // for SP
        App.userPrefs.apply {
            setString("current_nickname", profileViewModel.nickname.value.toString())
            setString("current_birthday", profileViewModel.birthday.value.toString())
        }

        val coupleChatID = App.userPrefs.getString("couple_chat_id")
        val partnerEmail = App.userPrefs.getString("current_partner_email")
        val position = if (myEmail.first().code <= partnerEmail.first().code) 1 else 2

        // for Firestore
        firestore.collection("coupleInfo").document(coupleChatID)
            .update(
                "user${position}Nickname", profileViewModel.nickname.value.toString(),
                "user${position}Birthday", profileViewModel.birthday.value.toString(),
                "firstMetDate", profileViewModel.firstMetDate.value.toString(),
                "user${position}Message", "",
                "user${position}MessagePosition", 0,
                "user${position}MessageAlignment", 0,
                "user${position}MessageSize", 0,
                "user${position}MessageColor", 0,
            ).addOnSuccessListener {
                Intent(this@ConnectionInputActivity, MainActivity::class.java).also {
                    startActivity(it)
                    slideRight()
                    finish()
                }
            }.addOnFailureListener {
                Snackbar.make(binding.root, "프로필 저장에 실패했습니다 잠시후 다시 시도해주세요.", Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun updateProfileImage() {
        val baos = ByteArrayOutputStream().also {
            profileViewModel.profileImage.value?.compress(Bitmap.CompressFormat.JPEG, 80, it)
        }
//        val data = baos.toByteArray()
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef =
            storageRef.child("profile/" + myEmail + ".jpg")

        CoroutineScope(Dispatchers.IO).launch {
            val uploadTask = imageRef.putBytes(baos.toByteArray())
                .addOnSuccessListener { taskSnapshot ->
                    firestore.collection("user").document(myEmail)
                        .update("profileImageFlag", true)
                    Log.d("img_test", "success - meta data: ${taskSnapshot.metadata}")
                }
                .addOnFailureListener {
                    Log.d("img_test", "fail to upload bitmap1: ${it.printStackTrace()}")
                    Log.d("img_test", "fail to upload bitmap2: ${it.message}")
                }
        }
    }

    private fun updateProfileInfo() {
        CoroutineScope(Dispatchers.IO).launch {
            // update user profile information
            firestore.collection("user").document(myEmail)
                .update(
                    "gender", profileViewModel.gender.value,
                    "nickname", profileViewModel.nickname.value,
                    "birthday", profileViewModel.birthday.value,
                    "profileInputFlag", true
                )
                .addOnSuccessListener {
                    Log.d("fb_test", "update profile info success")
                }
                .addOnFailureListener {
                    Log.d("fb_test", it.printStackTrace().toString())
                }
        }
    }

    private fun updateFirstMetDate() {
        CoroutineScope(Dispatchers.IO).launch {
            firestore.collection("user").document(myEmail).get()
                .addOnSuccessListener {
                    firestore.collection("couple").document(it["coupleDocID"].toString())
                        .update("firstMetDate", profileViewModel.firstMetDate.value.toString())
                }
                .addOnFailureListener {
                    Log.d("fb_test/err", "updateFirstMetDate() - fail to get couple doc ID")
                }
        }
    }

    private fun getImageFromGallery() {
        Intent(Intent.ACTION_GET_CONTENT).apply {
            setType("image/*")
            startActivityForResult(this, GALLERY)
        }
    }

    private fun getRotatedBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_NORMAL -> return bitmap
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }

        return try {
            val rotatedBitmap =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            bitmap.recycle()
            rotatedBitmap
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            bitmap
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                GALLERY -> {
                    val imageData: Uri? = data?.data
                    try {
                        MediaStore.Images.Media.getBitmap(contentResolver, imageData).also {
                            val file = File.createTempFile(
                                "prefix",
                                ".suffix",
                                applicationContext.cacheDir
                            )
                            file.outputStream().use {
                                contentResolver.openInputStream(imageData!!)?.copyTo(it)
                            }

                            val orientation = ExifInterface(file.absolutePath).getAttributeInt(
                                ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_UNDEFINED
                            )
                            val rotatedBitmap = getRotatedBitmap(it, orientation)

                            Glide.with(applicationContext)
                                .asBitmap()
                                .load(rotatedBitmap)
                                .circleCrop()
                                .into(object : SimpleTarget<Bitmap>() {
                                    override fun onResourceReady(
                                        resource: Bitmap,
                                        transition: Transition<in Bitmap>?
                                    ) {
                                        profileViewModel.setProfileImage(resource)
                                    }

                                    override fun onLoadCleared(placeholder: Drawable?) {
                                    }
                                })

                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
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

    private val _profileImage = MutableLiveData<Bitmap>()
    val profileImage: LiveData<Bitmap> get() = _profileImage

    init {
        this._gender.value = "female"
        this._nickname.value = ""
        this._birthday.value = ""
        this._firstMetDate.value = ""
        this._profileImage.value = ConnectionInputActivity.defaultProfileImage
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

    fun setProfileImage(bitmap: Bitmap) = viewModelScope.launch {
        _profileImage.value = bitmap
    }
}