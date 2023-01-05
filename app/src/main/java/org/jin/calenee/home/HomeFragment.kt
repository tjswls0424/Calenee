package org.jin.calenee.home

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.util.ArrayMap
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.MetadataChanges
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
const val BLACK_BACKGROUND = 1
const val WHITE_BACKGROUND = 2

class HomeFragment : Fragment(R.layout.fragment_home) {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var mContext: Context
    private lateinit var coupleInfoViewModel: CoupleInfoViewModel
    private lateinit var pickImageResult: ActivityResultLauncher<Intent>
    private lateinit var mActivity: Activity

    private var bitmap: Bitmap? = null
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

    override fun onAttach(context: Context) {
        super.onAttach(context)

        mActivity = requireActivity()
        if (context is MainActivity) {
            mContext = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        coupleInfoViewModel =
            ViewModelProvider(activity as ViewModelStoreOwner)[CoupleInfoViewModel::class.java]

        bitmap = getHomeBackground()

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
        updateHomeBackground()

        return binding.root
    }

    private fun listener() {
        binding.root.setOnLongClickListener {
            val bottomSheetDialog = BottomSheetDialog(mContext)
            val bottomSheetView = LayoutInflater.from(mContext)
                .inflate(R.layout.home_bottom_sheet_layout, null) as LinearLayout

            bottomSheetView.pick_image_btn.setOnClickListener {
                // 0
                coupleInfoDoc.update("homeBackground", CUSTOM_BACKGROUND)
                Intent().apply {
                    action = Intent.ACTION_PICK
                    type = "image/*"
                    pickImageResult.launch(Intent.createChooser(this, null))
                }

                bottomSheetDialog.dismiss()
            }
            bottomSheetView.black_background_btn.setOnClickListener {
                // 1
                coupleInfoDoc.update("homeBackground", BLACK_BACKGROUND)
                bottomSheetDialog.dismiss()
            }
            bottomSheetView.white_background_btn.setOnClickListener {
                // 2
                coupleInfoDoc.update("homeBackground", WHITE_BACKGROUND)
                bottomSheetDialog.dismiss()
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
                    val fileName = "home_background.jpg"

                    // save image to Storage(Firebase)
                    val fileRef = storageRef.child(filePath + File.separator + fileName)
                    CoroutineScope(Dispatchers.IO).launch {
                        result.data?.data?.let { uri ->
                            fileRef.putFile(uri)
                                .addOnSuccessListener {
                                    saveImage(fileName, uri)
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

    private fun saveImage(fileName: String, uri: Uri) {
        try {
            val fo = activity?.openFileOutput(fileName, Context.MODE_PRIVATE)
            val bitmap = ImageDecoder.decodeBitmap(
                ImageDecoder.createSource(
                    mActivity.contentResolver,
                    uri
                )
            )
            fo.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getHomeBackground(): Bitmap? {
        val fileName = "home_background.jpg"
        return BitmapFactory.decodeStream(mActivity.openFileInput(fileName)) ?: null
    }

    private fun updateHomeBackground() {
        coupleInfoDoc.addSnapshotListener(MetadataChanges.INCLUDE) { value, error ->
            value?.data?.get("homeBackground")?.let {
                when (it.toString().toInt()) {
                    0 -> {
                        setCoupleInfoTextColor(WHITE_BACKGROUND)
                        setStatusBarColor(true)

                        value.data?.get("homeBackgroundPath")?.let { path ->
                            if (path != "") {
                                if (bitmap != null) {
                                    // less blinking
                                    Glide.with(mActivity)
                                        .load(bitmap)
                                        .transition(DrawableTransitionOptions.withCrossFade())
                                        .into(binding.homeBackgroundIv)
                                } else {
                                    getHomeBackground()?.let { bitmap ->
                                        Glide.with(this)
                                            .load(bitmap)
                                            .transition(DrawableTransitionOptions.withCrossFade())
                                            .into(binding.homeBackgroundIv)
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        coupleInfoDoc.update("homeBackgroundPath", "")
                        setCoupleInfoTextColor(BLACK_BACKGROUND)
                        setStatusBarColor(false)
                    }
                    2 -> {
                        coupleInfoDoc.update("homeBackgroundPath", "")
                        setCoupleInfoTextColor(WHITE_BACKGROUND)
                        setStatusBarColor(true)
                    }
                }
            }
        }
    }

    private fun setCoupleInfoTextColor(color: Int) {
        when (color) {
            CUSTOM_BACKGROUND -> {

            }

            BLACK_BACKGROUND -> {
                binding.parentLayout.setBackgroundColor(Color.BLACK)
                binding.nickname1Tv.setTextColor(Color.WHITE)
                binding.nickname2Tv.setTextColor(Color.WHITE)
                binding.coupleDaysTv.setTextColor(Color.WHITE)
            }

            WHITE_BACKGROUND -> {
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