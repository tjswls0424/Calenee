package org.jin.calenee.chat

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.graphics.drawable.toBitmap
import org.jin.calenee.databinding.ActivityChatMediaDetailsBinding
import java.util.*

class ChatMediaDetailsActivity : AppCompatActivity() {

    private val binding: ActivityChatMediaDetailsBinding by lazy {
        ActivityChatMediaDetailsBinding.inflate(layoutInflater)
    }

    private var imageData: ChatData? = null
    private var isBitmapNull: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initImageData()
        Log.d("position_test/imageData", imageData.toString())


    }

    private fun initImageData() {
        Intent().apply {
            val byteArray = getByteArrayExtra("byteArray")
            val bitmapForDetails = if (byteArray != null) {
                isBitmapNull = true
                BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            } else {
                binding.imageView.background.toBitmap(300, 300)
            }

            imageData?.apply {
                bitmap = bitmapForDetails
                nickname = getStringExtra("nickname")
                time = getStringExtra("time")
                timeInMillis = getLongExtra("timeInMillis", Calendar.getInstance(Locale.KOREA).timeInMillis)
            }
        }

        checkBitmapCorrect()

        Log.d("position_test/imageData", imageData.toString())

    }

    private fun checkBitmapCorrect() {
        if (imageData?.bitmap == null || isBitmapNull) {
            binding.imageView.visibility = View.GONE
            binding.cannotLoadingText.visibility = View.VISIBLE
        } else {
            binding.imageView.visibility = View.VISIBLE
            binding.cannotLoadingText.visibility = View.GONE
        }
    }

}