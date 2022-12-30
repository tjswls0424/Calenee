package org.jin.calenee.home

import android.content.Context
import android.os.Bundle
import android.util.ArrayMap
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.home_bottom_sheet_layout.view.*
import org.jin.calenee.App
import org.jin.calenee.MainActivity
import org.jin.calenee.R
import org.jin.calenee.databinding.FragmentHomeBinding

class HomeFragment : Fragment(R.layout.fragment_home) {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var mContext: Context
    private lateinit var coupleInfoViewModel: CoupleInfoViewModel
    private var coupleInfoMap: ArrayMap<Int, TodayMessageInfo> = ArrayMap(10)

    private val firestore by lazy {
        Firebase.firestore
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity) {
            mContext = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        coupleInfoViewModel =
            ViewModelProvider(activity as ViewModelStoreOwner)[CoupleInfoViewModel::class.java]

        syncTodayMessageData()
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

        binding.root.setOnLongClickListener {
            val bottomSheetDialog = BottomSheetDialog(mContext)
            val bottomSheetView = LayoutInflater.from(mContext).inflate(R.layout.home_bottom_sheet_layout, null) as LinearLayout

            bottomSheetView.main_background_btn.setOnClickListener {
                Toast.makeText(mContext, "1", Toast.LENGTH_SHORT).show()
            }
            bottomSheetView.black_background_btn.setOnClickListener {
                Toast.makeText(mContext, "2", Toast.LENGTH_SHORT).show()

            }
            bottomSheetView.white_background_btn.setOnClickListener {
                Toast.makeText(mContext, "3", Toast.LENGTH_SHORT).show()

            }

            bottomSheetDialog.setContentView(bottomSheetView)
            bottomSheetDialog.show()

            true
        }

        initTodayMessageInfo()
        getTodayMessageData()

        return binding.root
    }

    private fun initTodayMessageInfo() {
        for (i in 0..9) {
            coupleInfoMap[i] = TodayMessageInfo()
        }
    }

    private fun syncTodayMessageData() {
        try {
            firestore.collection("coupleInfo").document(App.userPrefs.getString("couple_chat_id"))
                .addSnapshotListener { value, error ->
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