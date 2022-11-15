package org.jin.calenee.chat

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jin.calenee.App
import org.jin.calenee.databinding.ActivityChattingBinding
import org.jin.calenee.dialog.CaptureDialog
import org.jin.calenee.util.NetworkStatusHelper
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

const val DATE_TIME = 0
const val TIME = 1
const val DATE = 2

const val CAMERA = 10
const val ALBUM = 11
const val CAPTURE_VIDEO = 12

class ChattingActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityChattingBinding.inflate(layoutInflater)
    }
    private val currentUserEmail by lazy {
        FirebaseAuth.getInstance().currentUser?.email.toString()
    }
    private val firestore by lazy {
        Firebase.firestore
    }
    private val chatDB by lazy {
        FirebaseDatabase.getInstance().getReference("chat")
    }
    private val chatAdapter by lazy {
        ChatAdapter(applicationContext)
    }

    private val storageRef by lazy {
        FirebaseStorage.getInstance().reference
    }

    private var isKeyboardShown: Boolean = false
    private var chatDataList: MutableList<ChatData> = mutableListOf()
    private var message: String = ""
    private var nickname: String = ""

    private var tmpImageMap = hashMapOf<String, Int>()
    private lateinit var currentImagePath: String
    private lateinit var currentVideoPath: String

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)
        listener()
        checkNetworkStatus()
        getNickname()
        getSavedChatData()
    }

    private fun initRecycler() {
        binding.chatRecyclerView.itemAnimator = null

        LinearLayoutManager(applicationContext).apply {
            stackFromEnd = true
            binding.chatRecyclerView.layoutManager = this
        }

        chatAdapter.apply {
            binding.chatRecyclerView.adapter = this
            data = chatDataList
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun listener() {
        val inputMethodManager =
            getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
//        var viewHeight = -1

        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val param = binding.scrollView.layoutParams as ViewGroup.MarginLayoutParams
            val rect = Rect()
            binding.root.getWindowVisibleDisplayFrame(rect)
            val screenHeight = binding.root.rootView.height
            val keyBoardHeight = screenHeight - rect.bottom
            if (keyBoardHeight > screenHeight * 0.15) {
                isKeyboardShown = true

                if (binding.bottomSheetView.visibility == View.VISIBLE) {
                    setLottieInitialState()
                    param.setMargins(0, 0, 0, binding.bottomLayout.height)
                    binding.scrollView.layoutParams = param
                    binding.bottomSheetView.visibility = View.GONE

                    inputMethodManager.showSoftInput(binding.root, 0)
                }
            } else {
                isKeyboardShown = false
            }

//            val currentViewHeight = binding.root.height
//            if (currentViewHeight > viewHeight) {
//                val dm = applicationContext.resources.displayMetrics.heightPixels
//                window.attributes.height = (dm * 0.7).toInt()
//
//
//                binding.bottomSheetView.minimumHeight = currentViewHeight / 2 - binding.messageEt.height
//
//                viewHeight = currentViewHeight
//            }
        }

        binding.lottieAddCloseBtn.setOnClickListener {
            val param = binding.scrollView.layoutParams as ViewGroup.MarginLayoutParams
            if (isKeyboardShown) {
                inputMethodManager.hideSoftInputFromWindow(binding.root.windowToken, 0)
//                onBackPressed()

                binding.bottomSheetView.invalidate()
                binding.bottomSheetView.visibility = View.VISIBLE
            }

            if (binding.bottomSheetView.visibility == View.GONE) {
                // gone -> visible

                Log.d("k_test", "visible")
                param.apply {
                    setMargins(0, 0, 0, binding.bottomSheetView.minimumHeight)
                    binding.scrollView.layoutParams = this
                }

                binding.bottomSheetView.visibility = View.VISIBLE
                binding.lottieAddCloseBtn.apply {
                    progress = 0.0f
                    playAnimation()
                }

            } else {
                // visible -> gone
                Log.d("k_test", "gone")

                setLottieInitialState()
                param.setMargins(0, 0, 0, binding.bottomLayout.height)
                binding.scrollView.layoutParams = param
                binding.bottomSheetView.visibility = View.GONE
            }
        }

        binding.sendBtn.setOnClickListener {
            if (message.isNotEmpty()) {
                val currentTimeInMillis = Calendar.getInstance(Locale.KOREA).timeInMillis

                ChatData(1, nickname, message, getCurrentTimeStamp(TIME)).also {
//                    chatDataList.add(it)
                    saveChatData(it, currentTimeInMillis)
                }

                binding.messageEt.setText("")
                initRecycler()
            }
        }

        binding.cameraBtn.setOnClickListener {
            if (checkPermission(CAMERA)) {
                CaptureDialog(this).apply {
                    setOnClickedListener { captureType ->
                        when (captureType) {
                            ChatData.VIEW_TYPE_IMAGE -> {
                                startCapture()
                            }
                            ChatData.VIEW_TYPE_VIDEO -> {
                                checkPermission(CAPTURE_VIDEO)
                            }
                            else -> {
                                Snackbar.make(
                                    binding.root,
                                    "카메라를 실행할 수 없습니다. 잠시 후 다시 시도해주세요.",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }.show()

                Log.d("cam_test", "success")
            } else {
                Log.d("cam_test", "fail")
            }
        }

        binding.albumBtn.setOnClickListener {

        }

        binding.fileBtn.setOnClickListener {

        }

        binding.messageEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(text: Editable?) {
                message = "$text"
            }
        })

        // get realtime chat data (from point of view partner)
        chatDB.child(App.userPrefs.getString("couple_chat_id"))
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    Log.d("fb_test_chat/add", snapshot.value.toString())
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    snapshot.getValue(SavedChatData::class.java).also { data ->
                        // add element "chat_last_msg_time" to SP
                        // for comparing the date of the last message and the next message to be sent
                        // to display date text(viewType==2) when two messages have different dates
                        val currentTimeInMillis =
                            Calendar.getInstance(Locale.KOREA).timeInMillis
                        val tmpDate = Calendar.getInstance().apply {
                            App.userPrefs.getString("chat_last_msg_time").apply {
                                timeInMillis = if (this.isBlank()) {
                                    currentTimeInMillis
                                } else {
                                    this.toLong()
                                }
                            }
                            add(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis

                        when {
                            chatDataList.size == 0 || tmpDate <= currentTimeInMillis -> {
                                App.userPrefs.setString(
                                    "chat_last_msg_time",
                                    currentTimeInMillis.toString()
                                )

                                chatDataList.add(
                                    ChatData(
                                        time = getCurrentTimeStamp(
                                            DATE,
                                            currentTimeInMillis
                                        ), viewType = 2
                                    )
                                )

                                initRecycler()
                            }
                        }

                        /*
                        * !data?.message.isNullOrBlank() &&
                            !data?.createdAt.isNullOrBlank() &&
                            !data?.senderEmail.isNullOrBlank() &&
                            !data?.senderNickname.isNullOrBlank()
                        * */

                        // snapshot.childrenCount.toInt() == 4

                        if (snapshot.childrenCount.toInt() == 5 && data?.dataType == "text") {
                            val viewType = when {
                                (data.senderEmail == currentUserEmail) -> ChatData.VIEW_TYPE_RIGHT_TEXT
                                (data.senderEmail != currentUserEmail) -> ChatData.VIEW_TYPE_LEFT_TEXT
                                else -> -1
                            }

                            addChatDataList(viewType, data, snapshot.key.toString())
                            initRecycler()
                        } else if (snapshot.childrenCount.toInt() == 7 && data?.dataType == "image") {
                            val isMyChat = data.senderEmail == currentUserEmail
                            addChatDataList(
                                ChatData.VIEW_TYPE_IMAGE,
                                data,
                                snapshot.key.toString(),
                                isMyChat
                            )
                            initRecycler()
                        }
                    }

                    Log.d("fb_test_chat/changed", snapshot.value.toString())
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    Log.d("fb_test_chat/removed", snapshot.value.toString())
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    Log.d("fb_test_chat/moved", snapshot.value.toString())
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("fb_test_chat/cancelled", error.message)
                }
            })

        // recycler view item click listener
        chatAdapter.setOnItemClickListener(object : ChatAdapter.OnItemClickListener {
            override fun onItemClick(v: View, data: ChatData, position: Int) {
                Log.d("position_test", position.toString())
                Log.d("position_test", data.toString())

                try {
                    Intent(this@ChattingActivity, ChatMediaDetailsActivity::class.java).apply {
                        putExtra("fileName", createTempFileForBitmap(data.bitmap))
                        putExtra("nickname", data.nickname.toString())
                        putExtra("time", data.time.toString())
                        putExtra("timeInMillis", data.timeInMillis)
                    }.run { startActivity(this) }
                } catch (e: Exception) {
                    Log.d("position_test/err", e.stackTraceToString())
                    Log.d("position_test/err", e.message.toString())
                }

            }
        })
    }

    private fun createTempFileForBitmap(bitmap: Bitmap?): String? {
        var fileName: String? = "tempFileName"
        try {
            val fo = openFileOutput(fileName, Context.MODE_PRIVATE)
            fo.use {
                bitmap?.compress(Bitmap.CompressFormat.JPEG, 90, it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            fileName = null
        }

        return fileName
    }

    // 첫 실행시 (함수 종료 될 때까지) success listener에 값이 들어오기까지 몇 초 delay가 있기 때문에 미리 호출
    private fun getNickname() {
        firestore.collection("user")
            .document(currentUserEmail)
            .get()
            .addOnSuccessListener { doc ->
                nickname = doc.data?.get("nickname").toString()
                Log.d("db_test/nickname1", nickname)
            }
            .addOnFailureListener {
                Log.d("db_test/login-err", "${it.printStackTrace()}")
            }
    }

    private fun saveChatData(data: ChatData, timeInMillis: Long = 0L) {
        chatDB.child(App.userPrefs.getString("couple_chat_id"))
            .child(timeInMillis.toString()).apply {
                child("senderEmail").setValue(currentUserEmail)
                child("senderNickname").setValue(data.nickname)
                child("message").setValue(data.message)
                child("createdAt").setValue(data.time)
                child("dataType").setValue(data.dataType)
//                child("read").setValue(false)
            }
    }

    // in case of sending message by me
    private fun saveImageData(bitmap: Bitmap) {
        val baos = ByteArrayOutputStream().also {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }
        val dateTime = getCurrentTimeStamp(DATE_TIME)
        val imageRef =
            storageRef.child("chat/${App.userPrefs.getString("couple_chat_id")}/$dateTime.jpg")
        val ratio = bitmap.height.toDouble().div(bitmap.width.toDouble())
        val metadata = StorageMetadata.Builder()
            .setCustomMetadata("contentType", "image/jpg")
            .setCustomMetadata("resolution", "${bitmap.width}x${bitmap.height}")
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            val uploadTask = imageRef.putBytes(baos.toByteArray(), metadata)
                .addOnSuccessListener { taskSnapshot ->
                    Log.d("img_test/success", "success")

                    chatDB.child(App.userPrefs.getString("couple_chat_id")).child(dateTime).apply {
                        child("dataType").setValue("image")
                        child("senderEmail").setValue(currentUserEmail)
                        child("senderNickname").setValue(nickname)
                        child("fileAbsolutePath").setValue(imageRef.path)
                        child("fileName").setValue(imageRef.name)
                        child("fileRatio").setValue(ratio)
                        child("createdAt").setValue(getCurrentTimeStamp(TIME, dateTime.toLong()))
                    }
                }
                .addOnFailureListener {
                    Log.d("img_test", "fail to upload bitmap1: ${it.printStackTrace()}")
                    Log.d("img_test", "fail to upload bitmap2: ${it.message}")
                }
        }
    }

    private fun saveVideoData(videoUri: Uri) {
        val videoFile = File(videoUri.path.toString())
        val mp = MediaPlayer.create(applicationContext, videoUri)
//        val retriever = MediaMetadataRetriever().apply {
//            setDataSource(applicationContext, videoUri)
//        }
//
//        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toDouble() ?: 0.0
//        val height =
//            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toDouble() ?: 0.0
//        val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: ""

//        val ms = mp.duration.milliseconds.toString()
        val duration = getDurationText(mp.duration.milliseconds.toString())
        val ratio = (mp.videoWidth / mp.videoHeight).toDouble()

        Log.d("vid_test/name", videoFile.name)
        Log.d("vid_test/ratio", ratio.toString())
        Log.d("vid_test/durationText", duration)

        val videoRef =
            storageRef.child("chat/${App.userPrefs.getString("couple_chat_id")}/${videoFile.name}")
        val metadata = StorageMetadata.Builder()
            .setCustomMetadata("contentType", "video/mp4")
            .setCustomMetadata("duration", duration)
            .setCustomMetadata("resolution", "${mp.videoWidth}x${mp.videoHeight}")
            .build()

        val dateTime = videoFile.nameWithoutExtension.split("_").last()

        CoroutineScope(Dispatchers.IO).launch {
            videoRef.putFile(videoUri, metadata)
                .addOnSuccessListener { taskSnapshot ->
                    Log.d("vid_test", "success")

                    chatDB.child(App.userPrefs.getString("couple_chat_id")).child(dateTime).apply {
                        child("dataType").setValue("video")
                        child("senderEmail").setValue(currentUserEmail)
                        child("senderNickname").setValue(nickname)
                        child("fileAbsolutePath").setValue(videoRef.path)
                        child("fileName").setValue(videoRef.name)
                        child("fileRatio").setValue(ratio)
                        child("createdAt").setValue(getCurrentTimeStamp(TIME, dateTime.toLong()))
                        child("duration").setValue(duration)
                    }

                    clearFileCache(videoFile)
                }
                .addOnFailureListener {
                    Log.d("vid_test", "fail to upload video1: ${it.printStackTrace()}")
                    Log.d("vid_test", "fail to upload video2: ${it.message}")
                }
        }
    }

    private fun getDurationText(duration: String): String {
        var min = "0"
        var sec = ""

        if (duration.contains("m")) {
            duration.split("m ", ".").also { list ->
                min = list[0]
                sec = list[1]
            }
        } else {
            sec = duration.split(".", "s")[0]
        }

        return if (sec.length <= 1) {
            "$min:0$sec"
        } else {
            "$min:$sec"
        }
    }

    // first loading chat screen
    private var tmpTimeKey = 0L
    private fun getSavedChatData() {
        chatDB.child(App.userPrefs.getString("couple_chat_id"))
            .get().addOnSuccessListener {
                it.children.forEachIndexed { index, dataSnapshot ->
                    dataSnapshot.getValue(SavedChatData::class.java).also { data ->
                        // if index == last : SP에 마지막 메세지 데이터 저장 및 갱신
                        // SP에 저장된 마지막 메세지 데이터를 이용해 현재 메세지를 전송할 때 비교
                        Log.d("msg_test", "index: $index")
                        Log.d("msg_test", "last count: ${it.childrenCount}")
                        if (index.toLong() == (it.childrenCount - 1)) {
                            Log.d("msg_test", "key: ${dataSnapshot.key}")
                            App.userPrefs.setString(
                                "chat_last_msg_time",
                                dataSnapshot.key.toString()
                            )
                        } else if (index == 0) {
                            // first chat
                            App.userPrefs.setString(
                                "chat_last_msg_time",
                                getCurrentTimeStamp(DATE_TIME)
                            )
                        }

                        val tmpDate = Calendar.getInstance().apply {
                            timeInMillis = tmpTimeKey
                            add(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis

                        // for date text
                        when {
                            // tmpTimeKey랑 비교하는 절이 항상 true임
                            index == 0 || tmpDate <= (dataSnapshot.key?.toLong() ?: 0L) -> {
                                tmpTimeKey = dataSnapshot.key?.toLong() ?: 0
                                chatDataList.add(
                                    ChatData(
                                        time = getCurrentTimeStamp(
                                            DATE,
                                            tmpTimeKey
                                        ), viewType = 2
                                    )
                                )

                                App.userPrefs.setString("chat_last_msg_time", tmpTimeKey.toString())
                            }
                        }

                        val isMyChat = data?.senderEmail == currentUserEmail
                        val viewType: Int = when (data?.dataType) {
                            "text" -> {
                                if (isMyChat) ChatData.VIEW_TYPE_RIGHT_TEXT else ChatData.VIEW_TYPE_LEFT_TEXT
                            }

                            "image" -> ChatData.VIEW_TYPE_IMAGE

                            else -> -1
                        }

                        addChatDataList(viewType, data, dataSnapshot.key.toString(), isMyChat)
                    }
                }

                initRecycler()
            }
            .addOnFailureListener {
                Log.d("db_test", "fail to read from realtime database")
            }
    }

    private fun addChatDataList(
        viewType: Int,
        data: SavedChatData?,
        key: String = "",
        isMyChat: Boolean = false
    ) {
        when (viewType) {
            ChatData.VIEW_TYPE_LEFT_TEXT, ChatData.VIEW_TYPE_RIGHT_TEXT -> {
                chatDataList.add(
                    ChatData(
                        viewType,
                        data?.senderNickname,
                        data?.message,
                        data?.createdAt,
                    )
                )
            }

            ChatData.VIEW_TYPE_IMAGE -> {
                // Since it takes time to complete loading the image from server,
                // add a temporary item(empty ImageView) to chatDataList where the image data will be stored.
                tmpImageMap[key] = chatDataList.size

                val tmpChatData = ChatData(
                    viewType,
                    data?.senderNickname,
                    time = "",
                    ratio = data?.fileRatio ?: 1.0,
                    tmpIndex = chatDataList.size,
                    timeInMillis = key.toLong(),
                    dataType = "image",
                    isMyChat = isMyChat
                )

                // in case of sending image by me
//                if (isMyChat) {
//                    tmpChatData.time = "전송 중"
//                }

                chatDataList.add(
                    tmpImageMap[key] ?: 0,
                    tmpChatData
                )

                val imageRef =
                    FirebaseStorage.getInstance().reference.child(
                        "chat/${
                            App.userPrefs.getString(
                                "couple_chat_id"
                            )
                        }/${key}.jpg"
                    )

                CoroutineScope(Dispatchers.IO).launch {
                    imageRef.getBytes(3000 * 4000)
                        .addOnSuccessListener { byteArray ->
                            val bitmap = BitmapFactory.decodeByteArray(
                                byteArray,
                                0,
                                byteArray.size
                            )

                            chatDataList[tmpImageMap[key] ?: 0] =
                                ChatData(
                                    viewType,
                                    data?.senderNickname,
                                    time = data?.createdAt,
                                    bitmap = bitmap,
                                    ratio = data?.fileRatio ?: 1.0,
                                    timeInMillis = key.toLong(),
                                    dataType = "image",
                                    isMyChat = isMyChat
                                )

                            notifyItemChanged(tmpImageMap[key] ?: 0)
                            App.userPrefs.setString("chat_last_msg_time", key)
                        }
                }
            }

            else -> {
                Log.d("chat_test", "view type error : -1")
            }
        }

        App.userPrefs.setString("chat_last_msg_time", key)
    }

    private fun notifyItemChanged(position: Int) {
        binding.chatRecyclerView.adapter?.notifyItemChanged(position)
    }

    private fun notifyItemInserted(position: Int) {
        binding.chatRecyclerView.adapter?.notifyItemInserted(position)
    }

    private fun setLottieInitialState() {
        binding.lottieAddCloseBtn.apply {
            progress = 0.0f
            cancelAnimation()
        }
    }

    private fun getCurrentTimeStamp(
        type: Int,
        timeInMillis: Long = System.currentTimeMillis()
    ): String {
        return when (type) {
            DATE_TIME -> Calendar.getInstance(Locale.KOREA).timeInMillis.toString()
            TIME -> SimpleDateFormat("HH:mm", Locale.KOREA).format(timeInMillis)
            DATE -> SimpleDateFormat("yyyy년 MM월 dd일 (E)", Locale.KOREAN).format(timeInMillis)
            else -> throw RuntimeException("get time error")
        }
    }

    private fun checkPermission(type: Int): Boolean {
//        val tedPermission = TedPermission.create()

        return when (type) {
            CAMERA -> {
                TedPermission.create().apply {
                    setPermissionListener(object : PermissionListener {
                        override fun onPermissionGranted() {
//                        startCapture()
                        }

                        override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {}
                    })
                    setRationaleMessage("카메라 사진 권한 필요")
                    setDeniedMessage("카메라 권한이 거절되었습니다. 설정에서 권한을 허용해주세요.")
                    setPermissions(
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.CAMERA
                    )
                }.check()

                true
            }

            ALBUM -> {
                true
            }

            CAPTURE_VIDEO -> {
                TedPermission.create().apply {
                    setPermissionListener(object : PermissionListener {
                        override fun onPermissionGranted() {
                            startRecordVideo()
                            Snackbar.make(binding.root, "audio", Snackbar.LENGTH_SHORT).show()
                        }

                        override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {}
                    })
                    setRationaleMessage("오디오 녹화 권한 필요")
                    setPermissions(android.Manifest.permission.RECORD_AUDIO)
                    setDeniedMessage("설정에서 권한을 허용해주세요.")
                }.check()

                true
            }

            else -> {
                false
            }
        }
    }

    private fun startCapture() {
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
                intent.resolveActivity(packageManager)?.also {
                    val photofile: File? = try {
                        createImageFile()
                    } catch (e: IOException) {
                        null
                    }

                    photofile?.also {
                        val photoUri = FileProvider.getUriForFile(
                            this,
                            "org.jin.calenee.fileprovider",
                            it
                        )

                        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                        startActivityForResult(intent, CAMERA)
                    }
                }
            }
        }
    }

    private fun startRecordVideo() {
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { intent ->
                intent.resolveActivity(packageManager)?.also {
                    val videoFile: File? = try {
                        createVideoFile()
                    } catch (e: IOException) {
                        null
                    }

                    videoFile?.also {
                        val videoUri = FileProvider.getUriForFile(
                            this,
                            "org.jin.calenee.fileprovider",
                            it
                        )

                        intent.putExtra(
                            MediaStore.EXTRA_OUTPUT,
                            videoUri
                        ) // it will be written to specified path
                        startActivityForResult(intent, CAPTURE_VIDEO)
                    }
                }
            }
        }
    }

    private fun createImageFile(): File {
        return File(
            applicationContext.cacheDir,
            "IMG_${getCurrentTimeStamp(DATE_TIME)}.jpg"
        ).apply {
            currentImagePath = absolutePath
        }
    }

    private fun createVideoFile(): File {
        return File(
            applicationContext.cacheDir,
            "VID_${getCurrentTimeStamp(DATE_TIME)}.mp4"
        ).apply {
            currentVideoPath = absolutePath
        }
    }

//    private fun getCacheFile(fileNameWithExtension: String): File =
//        File(applicationContext.cacheDir, fileNameWithExtension)

    private fun clearFileCache(cacheFile: File) {
        cacheFile.delete()
        applicationContext.deleteFile(cacheFile.name)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun checkNetworkStatus() {
        NetworkStatusHelper(applicationContext).observe(this) { isConnected ->
            if (!isConnected) {
                Toast.makeText(this, "현재 인터넷이 연결되어 있지 않습니다. 인터넷을 연결해주세요.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                CAMERA -> {
                    // for thumbnail
//                    val bitmap = data?.extras?.get("data") as Bitmap

                    val file = File(currentImagePath)
                    val bitmap = if (Build.VERSION.SDK_INT < 28) {
                        MediaStore.Images.Media
                            .getBitmap(contentResolver, Uri.fromFile(file))
                    } else {
                        val decode = ImageDecoder.createSource(
                            this.contentResolver,
                            Uri.fromFile(file)
                        )
                        ImageDecoder.decodeBitmap(decode)
                    }

                    saveImageData(bitmap)
                }

                CAPTURE_VIDEO -> {
                    Log.d("video_test", "video abs path: $currentVideoPath")
                    Toast.makeText(this, "video success", Toast.LENGTH_SHORT).show()

                    val videoUri =
                        FileProvider.getUriForFile(
                            this,
                            "org.jin.calenee.fileprovider",
                            File(currentVideoPath)
                        )

                    saveVideoData(videoUri)

//                    Intent(applicationContext, ChatVideoDetailsActivity::class.java).apply {
//                        putExtra("filePath", currentVideoPath)
//                        startActivity(this)
//                    }

                }
            }
        }
    }

    override fun onBackPressed() {
        if (binding.bottomSheetView.visibility == View.VISIBLE) {
            val param = binding.scrollView.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 0, 0, binding.bottomLayout.height)
            binding.scrollView.layoutParams = param

            setLottieInitialState()
            binding.bottomSheetView.visibility = View.GONE
        } else {
            super.onBackPressed()
        }
    }
}