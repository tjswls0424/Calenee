package org.jin.calenee.home

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.ColorDrawable
import android.icu.util.Calendar
import android.os.Bundle
import android.util.ArrayMap
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.home_bottom_sheet_layout.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jin.calenee.App
import org.jin.calenee.MainActivity
import org.jin.calenee.R
import org.jin.calenee.databinding.FragmentHomeBinding
import java.io.File

const val CUSTOM_BACKGROUND = 0
const val BLACK = 1
const val WHITE = 2

class HomeFragment : Fragment(R.layout.fragment_home) {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var mContext: Context
    private lateinit var coupleInfoViewModel: CoupleInfoViewModel
    private lateinit var pickImageResult: ActivityResultLauncher<Intent>
    private lateinit var mActivity: Activity

    private var bitmap: Bitmap? = null
    private var previousBitmap: Bitmap? = null
    private var coupleInfoMap: ArrayMap<Int, TodayMessageInfo> = ArrayMap(10)

    private val firestore by lazy {
        Firebase.firestore
    }
    private val storageRef by lazy {
        FirebaseStorage.getInstance().reference
    }

    private val coupleInfoDoc by lazy {
        firestore.collection("coupleInfo").document(App.userPrefs.getString("couple_chat_id"))
    }

    private var homeBackgroundName: String = "" // SP

    override fun onAttach(context: Context) {
        super.onAttach(context)

        mActivity = requireActivity()
        if (context is MainActivity) {
            mContext = context
        }
    }

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        coupleInfoViewModel =
            ViewModelProvider(activity as ViewModelStoreOwner)[CoupleInfoViewModel::class.java]


        with(App.userPrefs.getString("home_background_name")) {
            if (this.isNotEmpty()) {
                homeBackgroundName = this
            }
        }

        getHomeBackground() // 초기 bitmap 설정
        updateHomeBackground()

        syncTodayMessageData()
        resultCallbackListener()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate<FragmentHomeBinding?>(
            inflater,
            R.layout.fragment_home,
            container,
            false
        ).apply {
            viewModel = coupleInfoViewModel
        }

        listener()
        initTodayMessageInfo()
        getTodayMessageData()

        return binding.root
    }

    private fun listener() {
        binding.root.setOnLongClickListener {
            val bottomSheetDialog = BottomSheetDialog(mContext)
            val bottomSheetView = LayoutInflater.from(mContext)
                .inflate(R.layout.home_bottom_sheet_layout, null) as LinearLayout

            with(bottomSheetView) {
                pick_image_btn.setOnClickListener {
                    // 0
                    Intent().apply {
                        action = Intent.ACTION_PICK
                        type = "image/*"
                        pickImageResult.launch(Intent.createChooser(this, null))
                    }

                    bottomSheetDialog.dismiss()
                }

                black_background_btn.setOnClickListener {
                    // 1
                    coupleInfoDoc.update("homeBackground", BLACK)
                    bottomSheetDialog.dismiss()
                }

                white_background_btn.setOnClickListener {
                    // 2
                    coupleInfoDoc.update("homeBackground", WHITE)
                    bottomSheetDialog.dismiss()
                }

                black_text_btn.setOnClickListener {
                    coupleInfoDoc.update("homeTextColor", BLACK)
                    bottomSheetDialog.dismiss()
                }

                white_text_btn.setOnClickListener {
                    coupleInfoDoc.update("homeTextColor", WHITE)
                    bottomSheetDialog.dismiss()
                }
            }

            bottomSheetDialog.setContentView(bottomSheetView)
            bottomSheetDialog.show()

            true
        }
    }

    private fun resultCallbackListener() {
        pickImageResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val filePath = "home/" + App.userPrefs.getString("couple_chat_id")
                    val fileName =
                        "home_background_" + Calendar.getInstance().timeInMillis.toString() + ".jpg"

                    // save image to Storage(Firebase)
                    val fileRef =
                        storageRef.child(filePath + File.separator + fileName)
                    CoroutineScope(Dispatchers.IO).launch {
                        result.data?.data?.let { uri ->
                            fileRef.putFile(uri)
                                .addOnSuccessListener {
                                    val tmpBitmap = ImageDecoder.decodeBitmap(
                                        ImageDecoder.createSource(
                                            mActivity.contentResolver,
                                            uri
                                        )
                                    )

                                    saveImage(fileName, tmpBitmap)
                                    bitmap = tmpBitmap
                                    previousBitmap = tmpBitmap
                                    setBackgroundImage()

                                    coupleInfoDoc.update("homeBackground", CUSTOM_BACKGROUND)
                                    coupleInfoDoc.update("homeBackgroundPath", fileRef.path)
                                }
                                .addOnFailureListener {
                                    coupleInfoDoc.update("homeBackgroundPath", "")
                                }
                        }
                    }
                }
            }
    }

    private fun saveImage(fileName: String, bitmap: Bitmap) {
        try {
            val fo = mActivity.openFileOutput(fileName, Context.MODE_PRIVATE)
            fo.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
            }

            App.userPrefs.setString("home_background_name", fileName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun deleteImage(fileName: String) {
        mActivity.deleteFile(fileName)
    }

    private fun getHomeBackground() {
        try {
            with(BitmapFactory.decodeStream(mActivity.openFileInput(homeBackgroundName))) {
                bitmap = this
                previousBitmap = this
            }
        } catch (e: Exception) {
            Log.d("home_test", "5")
            e.printStackTrace()

            bitmap = null
            previousBitmap = null
        }
    }

    private fun updateHomeBackground() {
        coupleInfoDoc.addSnapshotListener { snapshot, error ->
            // snapshot.metadata.hasPendingWrites()
            if (snapshot != null) {
                snapshot.data?.get("homeBackground")?.let {
                    when (it.toString().toInt()) {
                        CUSTOM_BACKGROUND -> {
//                            setHomeBackgroundStyle(WHITE)
                            setStatusBarColor(true)

                            snapshot.data?.get("homeBackgroundPath")?.let { path ->
                                if (path != "") {
//                                    val name2 = storageRef.child(path.toString()).name
                                    val name = path.toString().split("/").last()
                                    if (homeBackgroundName == name) {
                                        setBackgroundImage()
                                    } else {
                                        val tmpFile = File(mContext.cacheDir, name)
                                        CoroutineScope(Dispatchers.IO).launch {
//                                        FirebaseStorage.getInstance().getReference(path.toString())
                                            storageRef.child(path.toString())
                                                .getFile(tmpFile)
                                                .addOnSuccessListener {
                                                    App.userPrefs.setString(
                                                        "home_background_name",
                                                        tmpFile.name
                                                    )
                                                    homeBackgroundName = tmpFile.name

                                                    try {
                                                        val tmpBitmap =
                                                            BitmapFactory.decodeFile(tmpFile.path)
                                                        bitmap = tmpBitmap
                                                        setBackgroundImage()
                                                        saveImage(tmpFile.name, tmpBitmap)

                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                                .addOnFailureListener {
                                                    it.printStackTrace()
                                                }
                                        }
                                    }
                                }
                            }
                        }
                        BLACK -> {
                            coupleInfoDoc.update("homeBackgroundPath", "")
                            deleteImage(homeBackgroundName)
                            setHomeBackgroundStyle(BLACK)
                            setStatusBarColor(false)
                        }
                        WHITE -> {
                            coupleInfoDoc.update("homeBackgroundPath", "")
                            deleteImage(homeBackgroundName)
                            setHomeBackgroundStyle(WHITE)
                            setStatusBarColor(true)
                        }
                        else -> {}
                    }
                }

                snapshot?.data?.get("homeTextColor")?.let {
                    with(binding) {
                        when (it.toString().toInt()) {
                            BLACK -> {
                                nickname1Tv.setTextColor(Color.BLACK)
                                nickname2Tv.setTextColor(Color.BLACK)
                                coupleDaysTv.setTextColor(Color.BLACK)
                            }

                            WHITE -> {
                                nickname1Tv.setTextColor(Color.WHITE)
                                nickname2Tv.setTextColor(Color.WHITE)
                                coupleDaysTv.setTextColor(Color.WHITE)
                            }
                        }
                    }
                }
            } else {
                Log.d("home_test/err", error.toString())
            }
        }
    }

    private fun getProgress() = CircularProgressDrawable(mContext).apply {
        strokeWidth = 5f
        centerRadius = 30f
        start()
    }

    private fun setBackgroundImage() {
//        val thumbnailDrawable = BitmapDrawable(mContext.resources, bitmap)
        if (bitmap != null) {
            Log.d("home_test", "(2-1) set background image 1")
            // for less blinking
            Glide.with(mContext)
                .load(bitmap)
                .placeholder(getProgress())
                .error(ColorDrawable(Color.WHITE))
                .fallback(ColorDrawable(Color.WHITE))
                .thumbnail(Glide.with(mContext).asDrawable().load(bitmap).centerCrop())
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.homeBackgroundIv)
        } else {
            Log.d("home_test", "(2-2) set background image 2")
            getHomeBackground().let { bitmap ->
                Glide.with(mActivity)
                    .load(bitmap)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.homeBackgroundIv)

            }
        }
    }

    private fun setHomeBackgroundStyle(color: Int) {
        when (color) {
            CUSTOM_BACKGROUND -> {
            }

            BLACK -> {
                binding.parentLayout.setBackgroundColor(Color.BLACK)
                binding.nickname1Tv.setTextColor(Color.WHITE)
                binding.nickname2Tv.setTextColor(Color.WHITE)
                binding.coupleDaysTv.setTextColor(Color.WHITE)
            }

            WHITE -> {
                binding.parentLayout.setBackgroundColor(Color.WHITE)
                binding.nickname1Tv.setTextColor(Color.BLACK)
                binding.nickname2Tv.setTextColor(Color.BLACK)
                binding.coupleDaysTv.setTextColor(Color.BLACK)
            }
        }
    }

    private fun setStatusBarColor(lightMode: Boolean = true) {
        if (lightMode) {
            mActivity.window.statusBarColor = Color.WHITE
            WindowInsetsControllerCompat(
                mActivity.window,
                binding.root
            ).isAppearanceLightStatusBars = true
        } else {
            mActivity.window.statusBarColor = Color.BLACK
            WindowInsetsControllerCompat(
                mActivity.window,
                binding.root
            ).isAppearanceLightStatusBars = false
        }
    }

    private fun initTodayMessageInfo() {
        for (i in 0..9) {
            coupleInfoMap[i] = TodayMessageInfo()
        }
    }

    private fun syncTodayMessageData() {
        try {
            coupleInfoDoc.addSnapshotListener { value, error ->
                value?.data?.apply {
                    Log.d("msg_test", this.toString())
                    get("user1MessagePosition").let {
                        App.userPrefs.updateTodayMessageInfo(
                            1,
                            get("user1Message").toString(),
                            get("user1MessagePosition").toString().toInt(),
                            get("user1MessageAlignment").toString().toInt(),
                            get("user1MessageSize").toString().toInt(),
                            get("user1MessageColor").toString().toInt()
                        )
                    }

                    get("user2MessagePosition").let {
                        App.userPrefs.updateTodayMessageInfo(
                            2,
                            get("user2Message").toString(),
                            get("user2MessagePosition").toString().toInt(),
                            get("user2MessageAlignment").toString().toInt(),
                            get("user2MessageSize").toString().toInt(),
                            get("user2MessageColor").toString().toInt(),
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getTodayMessageData() {
        with(App.userPrefs) {
            try {
                if (getString("user1MessagePosition").isNotEmpty()) {
                    TodayMessageInfo(
                        getString("user1Message"),
                        getString("user1MessagePosition").toInt(),
                    ).apply {
                        messageAlignment = when (getString("user1MessageAlignment").toInt()) {
                            0 -> Gravity.START
                            1 -> Gravity.CENTER
                            2 -> Gravity.END
                            else -> Gravity.START
                        }

                        messageSize = when (getString("user1MessageSize").toInt()) {
                            0 -> 12
                            1 -> 15
                            2 -> 18
                            else -> 12
                        }

                        messageColor = when (getString("user1MessageColor").toInt()) {
                            0 -> "#FFFFFFFF"
                            1 -> "#535353"
                            2 -> "#FF000000"
                            else -> "#FFFFFFFF"
                        }

                        coupleInfoMap[this.messagePosition] = this
                        binding.todayMessage = coupleInfoMap
                    }
                }

                if (getString("user2MessagePosition").isNotEmpty()) {
                    TodayMessageInfo(
                        getString("user2Message"),
                        getString("user2MessagePosition").toInt(),
                    ).apply {
                        messageAlignment = when (getString("user2MessageAlignment").toInt()) {
                            0 -> Gravity.START
                            1 -> Gravity.CENTER
                            2 -> Gravity.END
                            else -> Gravity.START
                        }

                        messageSize = when (getString("user2MessageSize").toInt()) {
                            0 -> 12
                            1 -> 15
                            2 -> 18
                            else -> 12
                        }

                        messageColor = when (getString("user2MessageColor").toInt()) {
                            0 -> "#FFFFFF"
                            1 -> "#535353"
                            2 -> "#000000"
                            else -> "#FFFFFF"
                        }

                        coupleInfoMap[this.messagePosition] = this
                        binding.todayMessage = coupleInfoMap
                    }
                }
            } catch (e: Exception) {
                Log.d("err_test", e.printStackTrace().toString())
                e.printStackTrace()
            }
        }
    }

    data class TodayMessageInfo(
        val message: String = "",
        val messagePosition: Int = 0,
        var messageAlignment: Int = Gravity.START,
        var messageSize: Int = 12,
        var messageColor: String = "#FF535353",
    )
}