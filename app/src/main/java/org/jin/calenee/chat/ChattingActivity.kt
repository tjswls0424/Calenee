package org.jin.calenee.chat

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.jin.calenee.databinding.ActivityChattingBinding

class ChattingActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityChattingBinding.inflate(layoutInflater)
    }

    private val bottomSheetBehavior by lazy {
        BottomSheetBehavior.from(binding.bottomSheetView)
    }

    private var isKeyboardShown: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        getSoftInputHeight()
        listener()
        setContentView(binding.root)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun listener() {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var viewHeight = -1

        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            binding.root.getWindowVisibleDisplayFrame(rect)
            val screenHeight = binding.root.rootView.height
            val keyBoardHeight = screenHeight - rect.bottom
            if (keyBoardHeight > screenHeight * 0.15) {
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                    // bottom sheet OFF -> soft keyboard appear
                    setLottieInitialState()
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    binding.coordinatorLayout.visibility = View.GONE

                    inputMethodManager.showSoftInput(binding.root, 0)
                }
                Log.d("k_test", "is showing")
            } else {
                Log.d("k_test", "is closed")
            }

            val currentViewHeight = binding.root.height
            if (currentViewHeight > viewHeight) {
                binding.bottomSheetView.minimumHeight = currentViewHeight/2 - binding.messageEt.height
                bottomSheetBehavior.peekHeight = currentViewHeight/2 - binding.messageEt.height

                viewHeight = currentViewHeight
            }
        }

        binding.lottieAddCloseBtn.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                // bottom sheet OFF -> soft keyboard appear
                setLottieInitialState()
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                binding.coordinatorLayout.visibility = View.GONE

                inputMethodManager.showSoftInput(binding.root, 0)
            } else {
                binding.lottieAddCloseBtn.apply {
                    progress = 0.0f
                    playAnimation()
                }
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                binding.coordinatorLayout.visibility = View.VISIBLE

                inputMethodManager.hideSoftInputFromWindow(binding.root.windowToken, 0)
            }

        }

        binding.view.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                setLottieInitialState()
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                binding.coordinatorLayout.visibility = View.GONE
            }
        }
    }

    private fun getSoftInputHeight() {





    }

    private fun setLottieInitialState() {
        binding.lottieAddCloseBtn.apply {
            progress = 0.0f
            cancelAnimation()
        }
    }

    override fun onBackPressed() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            setLottieInitialState()
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            binding.coordinatorLayout.visibility = View.GONE
        } else {
            super.onBackPressed()
        }
    }
}