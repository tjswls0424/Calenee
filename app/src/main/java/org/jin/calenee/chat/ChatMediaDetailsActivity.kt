package org.jin.calenee.chat

import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.graphics.drawable.toBitmap
import org.jin.calenee.R
import org.jin.calenee.databinding.ActivityChatMediaDetailsBinding
import java.text.SimpleDateFormat
import java.util.*

class ChatMediaDetailsActivity : AppCompatActivity() {

    private val binding: ActivityChatMediaDetailsBinding by lazy {
        ActivityChatMediaDetailsBinding.inflate(layoutInflater)
    }

    private var imageData: ChatData = ChatData()
    private var isBitmapNull: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initImageData()
        setToolbar()
        listener()
    }

    private fun listener() {
        binding.imageView.setOnClickListener {
            if (supportActionBar?.isShowing == true) {
                supportActionBar?.hide()
            } else {
                supportActionBar?.show()
            }
        }
    }

    private fun initImageData() {
        intent.apply {
            val byteArray = getByteArrayExtra("byteArray")
            val bitmapForDetails = if (byteArray != null) {
                BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            } else {
                isBitmapNull = true
                binding.imageView.background.toBitmap(300, 300)
            }

            imageData = ChatData(
                bitmap = bitmapForDetails,
                nickname = getStringExtra("nickname"),
                time = getStringExtra("nickname"),
                timeInMillis = getLongExtra(
                    "timeInMillis",
                    Calendar.getInstance(Locale.KOREA).timeInMillis
                )
            )
        }

        if (checkBitmapCorrect()) {
            binding.apply {
                imageView.setImageBitmap(imageData.bitmap)
                nicknameTv.text = imageData.nickname
                dateTimeTv.text = SimpleDateFormat(
                    "yyyy.MM.dd (E) HH:mm",
                    Locale.KOREAN
                ).format(imageData?.timeInMillis ?: System.currentTimeMillis())
            }
        }
    }

    private fun checkBitmapCorrect(): Boolean {
        return if (imageData.bitmap == null || isBitmapNull) {
            binding.imageView.visibility = View.GONE
            binding.cannotLoadingText.visibility = View.VISIBLE
            false
        } else {
            binding.imageView.visibility = View.VISIBLE
            binding.cannotLoadingText.visibility = View.GONE
            true
        }
    }

    private fun setToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.chat_top_menu, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.chat_image_share -> {
                Log.d("menu_test", "chat_image_share")
                return true
            }
            R.id.chat_image_download -> {
                Log.d("menu_test", "chat_image_download")
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}