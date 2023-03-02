package org.jin.calenee.chat

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
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
import kotlinx.coroutines.*
import org.jin.calenee.App
import org.jin.calenee.chat.notification.ChatNotificationBody
import org.jin.calenee.chat.notification.ChatViewModel
import org.jin.calenee.databinding.ActivityChattingBinding
import org.jin.calenee.dialog.CaptureDialog
import org.jin.calenee.util.NetworkStatusHelper
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.round
import kotlin.time.Duration.Companion.milliseconds

const val DATE_TIME = 0
const val TIME = 1
const val DATE = 2

const val CAMERA = 10
const val ALBUM = 11
const val CAPTURE_VIDEO = 12

const val CHAT_TEXT_COUNT = 5
const val CHAT_IMAGE_COUNT = 8
const val CHAT_VIDEO_COUNT = 9
const val CHAT_FILE_COUNT = 8

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

    private lateinit var pickFilesResult: ActivityResultLauncher<Intent>

    private var isKeyboardShown: Boolean = false
    private var chatDataList: MutableList<ChatData> = mutableListOf()
    private var message: String = ""
    private var nickname: String = ""

    private var tmpMediaMap = hashMapOf<String, Int>()
    private lateinit var currentImagePath: String
    private lateinit var currentVideoPath: String

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)
        listener()
        firebaseListener()
        resultCallbackListener()
        checkNetworkStatus()
        getNickname()
        getSavedChatData()

        Log.d("fcm_test/my_token", App.userPrefs.getString("my_fcm_token"))
        Toast.makeText(this, App.userPrefs.getString("my_fcm_token"), Toast.LENGTH_LONG).show()
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
                // bottom sheet view : gone -> visible
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
            Intent().apply {
                action = Intent.ACTION_PICK
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                type = "video/* image/*"
                startActivityForResult(Intent.createChooser(this, null), ALBUM)
            }
        }

        binding.fileBtn.setOnClickListener {
            // + extraMimeType
            if (checkPermission(ALBUM)) {
                Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                    pickFilesResult.launch(this)
                }
            }
        }

        binding.messageEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(text: Editable?) {
                message = "$text"
            }
        })

        // recycler view item click listener
        chatAdapter.setOnItemClickListener(object : ChatAdapter.OnItemClickListener {
            override fun onItemClick(v: View, data: ChatData, position: Int) {
                Log.d("position_test", position.toString())
                Log.d("position_test", data.toString())

                try {
                    when (data.dataType) {
                        "image" -> {
                            Intent(
                                this@ChattingActivity,
                                ChatImageDetailsActivity::class.java
                            ).apply {
                                putExtra("fileName", createTempFileForBitmap(data.bitmap))
                                putExtra("nickname", data.nickname.toString())
                                putExtra("time", data.time.toString())
                                putExtra("timeInMillis", data.timeInMillis)
                            }.run { startActivity(this) }
                        }

                        "video" -> {
                            Intent(
                                this@ChattingActivity,
                                ChatVideoDetailsActivity::class.java
                            ).apply {
                                putExtra("fileName", data.fileNameWithExtension)
                                putExtra("nickname", data.nickname.toString())
                                putExtra("time", data.time.toString())
                                putExtra("timeInMillis", data.timeInMillis)
                            }.run { startActivity(this) }
                        }

                        "file" -> {
                            val tmpFile =
                                File(applicationContext.cacheDir, data.fileNameWithExtension)
                            val uri = FileProvider.getUriForFile(
                                applicationContext,
                                "$packageName.fileprovider",
                                tmpFile
                            )
                            val type = applicationContext.contentResolver.getType(uri)

                            Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, type)
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                startActivity(this)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d("position_test/err", e.stackTraceToString())
                    Log.d("position_test/err", e.message.toString())
                }
            }

            override fun onItemLongClick(v: View, data: ChatData, position: Int) {
                try {
                    when (data.dataType) {
                        "file" -> {
                            val items = arrayOf("저장", "공유")
                            AlertDialog.Builder(this@ChattingActivity).apply {
                                title = "작업 선택"
                                setItems(items) { _, index ->
                                    when (items[index]) {
                                        "저장" -> {
                                            val srcFile =
                                                File(context.cacheDir, data.fileNameWithExtension)

                                            try {
                                                val fos: OutputStream?
                                                val contentValues = ContentValues().apply {
                                                    put(
                                                        MediaStore.DownloadColumns.DISPLAY_NAME,
                                                        srcFile.name
                                                    )
                                                    put(
                                                        MediaStore.DownloadColumns.RELATIVE_PATH,
                                                        Environment.DIRECTORY_DOWNLOADS + File.separator + "CaleneeDownload"
                                                    )
                                                }

                                                val uri =
                                                    contentResolver.insert(
                                                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                                                        contentValues
                                                    )
                                                fos =
                                                    uri?.let { contentResolver.openOutputStream(it) }
                                                fos.use {
                                                    fos?.write(srcFile.readBytes())
                                                }
                                            } catch (e: IOException) {
                                                Log.d("file_test", "IOException")
                                                e.printStackTrace()
                                            }

                                            Snackbar.make(
                                                binding.root,
                                                "저장되었습니다.",
                                                Snackbar.LENGTH_SHORT
                                            )
                                                .show()
                                        }

                                        "공유" -> {
                                            val uri = FileProvider.getUriForFile(
                                                this@ChattingActivity,
                                                "$packageName.fileprovider",
                                                File(
                                                    applicationContext.cacheDir,
                                                    data.fileNameWithExtension
                                                )
                                            )
                                            val mimeType =
                                                applicationContext.contentResolver.getType(uri)
                                            Intent(Intent.ACTION_SEND).apply {
                                                type =
                                                    "image/* video/* text/* audio/* application/*"
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                setDataAndType(uri, mimeType)
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                startActivity(Intent.createChooser(this, "공유"))
                                            }
                                        }
                                    }
                                }
                            }.show()
                        }
                    }
                } catch (e: Exception) {
                    Log.d("position_test/err", e.stackTraceToString())
                    Log.d("position_test/err", e.message.toString())
                }
            }
        })
    }

    private fun firebaseListener() {
        // get realtime chat data (from point of view partner)
        chatDB.child(App.userPrefs.getString("couple_chat_id"))
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {}

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

                        when (data?.dataType) {
                            "text" -> {
                                if (snapshot.childrenCount.toInt() == CHAT_TEXT_COUNT) {
                                    val viewType = when {
                                        (data.senderEmail == currentUserEmail) -> ChatData.VIEW_TYPE_RIGHT_TEXT
                                        (data.senderEmail != currentUserEmail) -> ChatData.VIEW_TYPE_LEFT_TEXT
                                        else -> -1
                                    }

                                    addChatDataList(
                                        viewType,
                                        data,
                                        snapshot.key.toString(),
                                        data.senderEmail == currentUserEmail,
                                        isRealtime = true
                                    )
                                    initRecycler()
                                }
                            }

                            "image" -> {
                                if (snapshot.childrenCount.toInt() == CHAT_IMAGE_COUNT) {
                                    val isMyChat = data.senderEmail == currentUserEmail
                                    addChatDataList(
                                        ChatData.VIEW_TYPE_IMAGE,
                                        data,
                                        snapshot.key.toString(),
                                        data.senderEmail == currentUserEmail,
                                        true
                                    )

                                    initRecycler()
                                }
                            }

                            "video" -> {
                                if (snapshot.childrenCount.toInt() == CHAT_VIDEO_COUNT) {
                                    addChatDataList(
                                        ChatData.VIEW_TYPE_VIDEO,
                                        data,
                                        snapshot.key.toString(),
                                        data.senderEmail == currentUserEmail,
                                        true
                                    )

                                    initRecycler()
                                }
                            }

                            "file" -> {
                                // files
                                if (snapshot.childrenCount.toInt() == CHAT_FILE_COUNT) {
                                    addChatDataList(
                                        ChatData.VIEW_TYPE_FILE,
                                        data,
                                        snapshot.key.toString(),
                                        data.senderEmail == currentUserEmail,
                                        true
                                    )
                                }
                            }

                            else -> {
                                Log.d("data_type_err", "data type err")
                            }
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
    }

    private fun sendNotification(message: String, nickname: String) {
        val chatViewModel = ChatViewModel(application)
        val myToken = App.userPrefs.getString("my_fcm_token")
        val partnerToken = App.userPrefs.getString("partner_fcm_token")
        val data = ChatNotificationBody.ChatNotificationData("캘린이", message, nickname)

        // partner fcm token으로 보내면 내 device로 알림이 오고
        // my token을 써야 상대로 감
        val body = ChatNotificationBody(partnerToken, data)
        val body2 = ChatNotificationBody(myToken, data)

        Log.d("fcm_test/token1", "my: ${App.userPrefs.getString("my_fcm_token")}")
        Log.d("fcm_test/token2", "partner: $partnerToken")
        chatViewModel.sendNotification(body2)
    }

    private fun resultCallbackListener() {
        pickFilesResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    // mimeType: image, video, txt, audio(m4a)
                    // mimeType 조건문 세팅해서 나머지는 "지원하지 않는 파일 형식입니다." Toast
                    // 파일을 열 수 있는 앱이 없습니다.

                    result.data?.data?.let { returnUri ->
                        applicationContext.contentResolver.query(returnUri, null, null, null, null)
                    }?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                        cursor.moveToFirst()
                        val name = cursor.getString(nameIndex)
                        val size = cursor.getString(sizeIndex)

                        Log.d("uri_test/name", name)
                        Log.d("uri_test/size", size)

                        result.data?.data?.let { saveFileData(it, name) }
                    }
                } else {
                    Log.d("intent_test", "resultCode: ${result.resultCode}")
                }
            }
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
                child("dataType").setValue(data.dataType)
                child("senderEmail").setValue(currentUserEmail)
                child("senderNickname").setValue(data.nickname)
                child("message").setValue(data.message)
                child("createdAt").setValue(data.time)
                child("mimeType").setValue(data.mimeType)
//                child("read").setValue(false)
            }
    }

    // in case of sending message by me
    private fun saveImageData(bitmap: Bitmap, uri: Uri) {
        val baos = ByteArrayOutputStream().also {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }
        val mimeType = applicationContext.contentResolver.getType(uri)
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        val dateTime = getCurrentTimeStamp(DATE_TIME)
        val imageRef =
            storageRef.child("chat/${App.userPrefs.getString("couple_chat_id")}/$dateTime.$extension")
        val ratio = bitmap.height.toDouble().div(bitmap.width.toDouble())
        val metadata = StorageMetadata.Builder()
            .setCustomMetadata("contentType", mimeType ?: "image/jpeg")
            .setCustomMetadata("resolution", "${bitmap.width}x${bitmap.height}")
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            val uploadTask = imageRef.putBytes(baos.toByteArray(), metadata)
                .addOnProgressListener {
                    showFileTransferProgress(it.bytesTransferred)
                }
                .addOnSuccessListener { taskSnapshot ->
                    Log.d("img_test/success", "success")

                    chatDB.child(App.userPrefs.getString("couple_chat_id")).child(dateTime).apply {
                        child("dataType").setValue("image")
                        child("mimeType").setValue(mimeType ?: "image/jpeg")
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
        val duration = getDurationText(mp.duration.milliseconds.toString())
        val mimeType = applicationContext.contentResolver.getType(videoUri)
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        val dateTime = getCurrentTimeStamp(DATE_TIME)
        val videoRef =
            storageRef.child("chat/${App.userPrefs.getString("couple_chat_id")}/$dateTime.$extension")
        val ratio = (mp.videoHeight.toDouble() / mp.videoWidth.toDouble())
        val metadata = StorageMetadata.Builder()
            .setCustomMetadata("contentType", mimeType ?: "video/mp4")
            .setCustomMetadata("duration", duration)
            .setCustomMetadata("resolution", "${mp.videoWidth}x${mp.videoHeight}")
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            videoRef.putFile(videoUri, metadata)
                .addOnProgressListener {
                    showFileTransferProgress(it.bytesTransferred)
                }
                .addOnSuccessListener { taskSnapshot ->
                    chatDB.child(App.userPrefs.getString("couple_chat_id")).child(dateTime).apply {
                        child("dataType").setValue("video")
                        child("mimeType").setValue(mimeType ?: "video")
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

    private fun saveFileData(fileUri: Uri, fileName: String) {
        val dateTime = getCurrentTimeStamp(DATE_TIME)
        val mimeType = applicationContext.contentResolver.getType(fileUri)
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        val expirationDate = Calendar.getInstance().apply {
            timeInMillis = dateTime.toLong()
            add(Calendar.DAY_OF_MONTH, 14)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis
        val expirationDateText =
            SimpleDateFormat("yyyy.MM.dd HH:mm:s", Locale.KOREA).format(expirationDate)

        Log.d("uri_test/extension", extension.toString())
        Log.d("uri_test/mimeType", mimeType.toString())
        Log.d("uri_test/expirationDate", expirationDateText)

        val fileRef =
            storageRef.child("chat/${App.userPrefs.getString("couple_chat_id")}/files/$dateTime.$extension")
        val metadata = StorageMetadata.Builder()
            .setCustomMetadata("contentType", mimeType)
            .setCustomMetadata("expirationDate", expirationDateText)
            .setCustomMetadata("fileName", fileName)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            fileRef.putFile(fileUri, metadata)
                .addOnProgressListener {
                    showFileTransferProgress(it.bytesTransferred)
                }
                .addOnSuccessListener {
                    Log.d("uri_test/size2", it.metadata?.sizeBytes.toString())

                    chatDB.child(App.userPrefs.getString("couple_chat_id")).child(dateTime).apply {
                        child("dataType").setValue("file")
                        child("mimeType").setValue(mimeType)
                        child("senderEmail").setValue(currentUserEmail)
                        child("senderNickname").setValue(nickname)
                        child("fileAbsolutePath").setValue(fileRef.path)
                        child("fileName").setValue(fileName)
                        child("createdAt").setValue(getCurrentTimeStamp(TIME, dateTime.toLong()))
                        child("expirationDate").setValue(expirationDateText.split(" ")[0])
                    }
                }
        }
    }

    private fun showFileTransferProgress(fileSizeBytes: Long) {
        var fileSizeNum = 0.0
        var fileSizeUnit = "KB"
        if (fileSizeBytes != 0L) {
            if (fileSizeBytes.floorDiv(1024).toString().length <= 3) {
                fileSizeNum = fileSizeBytes.div(1024.0)
                fileSizeUnit = "KB"
            } else if (fileSizeBytes.floorDiv(1024).toString().length <= 6) {
                fileSizeNum = fileSizeBytes.div(1024.0.pow(2))
                fileSizeUnit = "MB"
            } else {
                fileSizeNum = fileSizeBytes.toDouble()
                fileSizeUnit = "B"
            }
        }

        Snackbar.make(binding.scrollView, "${round(fileSizeNum*100)/100}$fileSizeUnit", Snackbar.LENGTH_LONG).show()
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
                            "video" -> ChatData.VIEW_TYPE_VIDEO
                            "file" -> ChatData.VIEW_TYPE_FILE
                            else -> -1
                        }

                        addChatDataList(viewType, data, dataSnapshot.key.toString(), isMyChat, false)
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
        isMyChat: Boolean = false,
        isRealtime: Boolean = false,
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

                Log.d("fcm_test/if-statement1", "isMyChat: $isMyChat")
                Log.d("fcm_test/if-statement2", "isRealtime: $isRealtime")
                if (!isMyChat && isRealtime) {
                    Log.d("fcm_test/send1", "send 1")
                    sendNotification(data?.message.toString(), data?.senderNickname.toString())
                    Log.d("fcm_test/send2", "send 2")

                }
            }

            ChatData.VIEW_TYPE_IMAGE -> {
                // Since it takes time to complete loading the image from server,
                // add a temporary item(empty ImageView) to chatDataList where the image data will be stored.
                tmpMediaMap[key] = chatDataList.size

                val tmpChatData = ChatData(
                    viewType,
                    data?.senderNickname,
                    time = "",
                    ratio = data?.fileRatio ?: 1.0,
                    tmpIndex = chatDataList.size,
                    timeInMillis = key.toLong(),
                    mimeType = data?.mimeType.toString(),
                    dataType = data?.dataType.toString(),
                    fileNameWithExtension = data?.fileName.toString(),
                    isMyChat = isMyChat
                )

                // in case of sending image by me
                if (isMyChat && isRealtime) {
                    tmpChatData.time = "전송 중"
                }

                chatDataList.add(
                    tmpMediaMap[key] ?: chatDataList.size,
                    tmpChatData
                )

                val tmpFile = File(applicationContext.cacheDir, data?.fileName.toString())
                CoroutineScope(Dispatchers.IO).launch {
                    FirebaseStorage.getInstance().getReference(data?.fileAbsolutePath.toString())
                        .apply {
                            getFile(tmpFile)
                                .addOnSuccessListener {
                                    Log.d("fb_test", "success to get file")
                                    val bitmap = BitmapFactory.decodeFile(tmpFile.path)
                                    val chatData = tmpChatData.apply {
                                        this.time = data?.createdAt
                                        this.bitmap = bitmap
                                    }

                                    chatDataList[tmpMediaMap[key] ?: tmpChatData.tmpIndex] =
                                        chatData
                                    notifyItemChanged(tmpMediaMap[key] ?: tmpChatData.tmpIndex)
                                    App.userPrefs.setString("chat_last_msg_time", key)

                                    if (!isMyChat && isRealtime) sendNotification("사진을 보냈습니다.", data?.senderNickname.toString())
                                }
                                .addOnFailureListener {
                                    Log.d("fb_test", "fail to get image file")
                                }
                        }
                }
            }

            ChatData.VIEW_TYPE_VIDEO -> {
                tmpMediaMap[key] = chatDataList.size

                val tmpChatData = ChatData(
                    viewType,
                    data?.senderNickname,
                    time = "",
                    ratio = data?.fileRatio ?: 1.0,
                    tmpIndex = chatDataList.size,
                    timeInMillis = key.toLong(),
                    fileNameWithExtension = data?.fileName.toString(),
                    mimeType = data?.mimeType.toString(),
                    dataType = data?.dataType.toString(),
                    isMyChat = isMyChat,
                    duration = data?.duration.toString()
                )

                // in case of sending image by me
                if (isMyChat && isRealtime) {
                    tmpChatData.time = "전송 중"
                }

                chatDataList.add(
                    tmpMediaMap[key] ?: chatDataList.size,
                    tmpChatData
                )

                val tmpFile = File(applicationContext.cacheDir, data?.fileName.toString())
                CoroutineScope(Dispatchers.IO).launch {
                    FirebaseStorage.getInstance().getReference(data?.fileAbsolutePath.toString())
                        .apply {
                            getFile(tmpFile)
                                .addOnSuccessListener {
                                    val mr = MediaMetadataRetriever().apply {
                                        setDataSource(tmpFile.path)
                                    }
                                    val chatData = tmpChatData.apply {
                                        this.time = data?.createdAt
                                        this.bitmap = mr.getFrameAtTime(1000000)
                                    }

                                    chatDataList[tmpMediaMap[key] ?: tmpChatData.tmpIndex] =
                                        chatData
                                    notifyItemChanged(tmpMediaMap[key] ?: tmpChatData.tmpIndex)
                                    App.userPrefs.setString("chat_last_msg_time", key)

                                    if (!isMyChat && isRealtime) sendNotification("동영상을 보냈습니다.", data?.senderNickname.toString())
                                }
                                .addOnFailureListener {
                                    Log.d("fb_test", "fail to get video file")
                                }
                        }
                }
            }

            ChatData.VIEW_TYPE_FILE -> {
                tmpMediaMap[key] = chatDataList.size

                val tmpChatData = ChatData(
                    viewType,
                    data?.senderNickname,
                    time = "",
                    tmpIndex = chatDataList.size,
                    timeInMillis = key.toLong(),
                    mimeType = data?.mimeType.toString(),
                    dataType = data?.dataType.toString(),
                    fileNameWithExtension = data?.fileName.toString(),
                    isMyChat = isMyChat,
                    expirationDate = data?.expirationDate.toString()
                )

                if (isMyChat && isRealtime) {
                    tmpChatData.time = "전송 중"
                }

                chatDataList.add(
                    tmpMediaMap[key] ?: chatDataList.size,
                    tmpChatData
                )

                val tmpFile = File(applicationContext.cacheDir, data?.fileName.toString())
                CoroutineScope(Dispatchers.IO).launch {
                    FirebaseStorage.getInstance().getReference(data?.fileAbsolutePath.toString())
                        .apply {
                            getFile(tmpFile)
                                .addOnSuccessListener {
//                                    val fileSizeBytes = this.metadata.result.sizeBytes
                                    val fileSizeBytes = it.totalByteCount
                                    val chatData = tmpChatData.apply {
                                        this.time = data?.createdAt
                                        this.fileSize = fileSizeBytes
                                    }

                                    chatDataList[tmpMediaMap[key] ?: tmpChatData.tmpIndex] =
                                        chatData
                                    notifyItemChanged(tmpMediaMap[key] ?: tmpChatData.tmpIndex)
                                    App.userPrefs.setString("chat_last_msg_time", key)

                                    if (!isMyChat && isRealtime) sendNotification("파일을 보냈습니다.", data?.senderNickname.toString())
                                }
                                .addOnFailureListener {
                                    Log.d("fb_test", "fail to get file")
                                }
                        }
                }
            }

            else -> {
                Log.d("chat_test", "view type error")
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

                    // file말고 uri로 수정?
                    val imageFile = File(currentImagePath)
                    val uri = FileProvider.getUriForFile(
                        applicationContext,
                        "$packageName.fileprovider", imageFile
                    )
                    val decode = ImageDecoder.createSource(
                        this.contentResolver,
                        Uri.fromFile(imageFile)
                    )
                    val bitmap = ImageDecoder.decodeBitmap(decode)

                    saveImageData(bitmap, uri)
                }

                CAPTURE_VIDEO -> {
                    val videoUri =
                        FileProvider.getUriForFile(
                            this,
                            "org.jin.calenee.fileprovider",
                            File(currentVideoPath)
                        )

                    saveVideoData(videoUri)
                }

                ALBUM -> {
                    if (data?.clipData != null) {
                        // picked multiple files
                        val count = data.clipData!!.itemCount
                        if (count > 15) {
                            Snackbar.make(
                                binding.root,
                                "15장 이상은 전송할 수 없습니다.",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        } else {
                            for (i in 0 until count) {
                                val uri = data.clipData!!.getItemAt(i).uri
                                val mimeType = applicationContext.contentResolver.getType(uri)

                                if (mimeType != null) {
                                    if (mimeType.contains("image")) {
                                        val bitmap = ImageDecoder.decodeBitmap(
                                            ImageDecoder.createSource(
                                                contentResolver,
                                                uri
                                            )
                                        )
                                        saveImageData(bitmap, uri)
                                    } else if (mimeType.contains("video")) {
                                        saveVideoData(uri)
                                    }
                                } else {
                                    Log.d("uri_test/mimtType-err", "mimeType is null")
                                }

                                Log.d("uri_test", uri.toString())
                                Log.d("uri_test/mimeType", mimeType.toString())
                            }
                        }
                    } else {
                        // picked a single file
                    }
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
