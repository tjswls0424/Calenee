package org.jin.calenee.dialog

import android.app.Dialog
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import org.jin.calenee.chat.ChatData
import org.jin.calenee.databinding.CaptureDialogBinding

class CaptureDialog(private val context: AppCompatActivity) {
    private lateinit var listener: CaptureDialogClickedListener
    private lateinit var binding: CaptureDialogBinding
    private val dialog = Dialog(context)

    fun show() {
        binding = CaptureDialogBinding.inflate(context.layoutInflater)

        dialog.apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(binding.root)
            setCancelable(true)
        }

        binding.imageCaptureBtn.setOnClickListener {
            listener.onClicked(ChatData.VIEW_TYPE_IMAGE)
            dialog.dismiss()
        }
        binding.videoCaptureBtn.setOnClickListener {
            listener.onClicked(ChatData.VIEW_TYPE_VIDEO)
            dialog.dismiss()
        }

        dialog.show()
    }

    fun setOnClickedListener(listener: (Int) -> Unit) {
        this.listener = object : CaptureDialogClickedListener {
            override fun onClicked(captureType: Int) {
                listener(captureType)
            }
        }
    }

    interface CaptureDialogClickedListener {
        fun onClicked(captureType: Int)
    }
}