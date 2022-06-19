package org.jin.calenee.chat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.jin.calenee.databinding.ActivityChattingBinding

class ChattingActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityChattingBinding.inflate(layoutInflater)
    }

    private val bottomSheetBehavior by lazy {
        BottomSheetBehavior.from(binding.bottomSheetView)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        listener()
        setContentView(binding.root)
    }

    private fun listener() {
        binding.lottieAddCloseBtn.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                setLottieInitialState()
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                binding.coordinatorLayout.visibility = View.GONE
            } else {
                binding.lottieAddCloseBtn.apply {
                    progress = 0.0f
                    playAnimation()
                }
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                binding.coordinatorLayout.visibility = View.VISIBLE
            }
        }

        binding.view.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                setLottieInitialState()
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                binding.coordinatorLayout.visibility = View.GONE
            }
        }
    }

    private fun setLottieInitialState() {
        binding.lottieAddCloseBtn.apply {
            progress = 0.0f
            cancelAnimation()
        }
    }

    override fun onBackPressed() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            setLottieInitialState()
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            binding.coordinatorLayout.visibility = View.GONE
        } else {
            super.onBackPressed()
        }
    }
}