package org.jin.calenee.chat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.jin.calenee.R
import org.jin.calenee.databinding.ActivityChattingBinding

class ChattingActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityChattingBinding.inflate(layoutInflater)
    }

    private val bottomSheetDialog by lazy {
        setBottomSheetDialog()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        listener()


        setContentView(binding.root)
    }

    private fun listener() {
        binding.plusBtn.setOnClickListener {
            window.statusBarColor = getColor(R.color.transparent)
            bottomSheetDialog.apply {
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }.show()
        }
    }

    private fun setBottomSheetDialog(): BottomSheetDialog {
        val bottomSheetView = layoutInflater.inflate(R.layout.chat_botom_sheet_layout, null)

        return BottomSheetDialog(this, R.style.BottomSheetDialog).apply {
            setContentView(bottomSheetView)

            behavior.state = BottomSheetBehavior.STATE_HIDDEN

        }
    }

    override fun onBackPressed() {
        super.onBackPressed()

        if (bottomSheetDialog.isShowing) {
            bottomSheetDialog.dismiss()
//            bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }
}