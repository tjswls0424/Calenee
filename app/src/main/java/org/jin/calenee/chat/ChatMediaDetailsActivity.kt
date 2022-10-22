package org.jin.calenee.chat

import android.content.ContentValues
import android.graphics.*
import android.media.ExifInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.android.material.snackbar.Snackbar
import org.jin.calenee.R
import org.jin.calenee.databinding.ActivityChatMediaDetailsBinding
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class ChatMediaDetailsActivity : AppCompatActivity() {

    private val binding: ActivityChatMediaDetailsBinding by lazy {
        ActivityChatMediaDetailsBinding.inflate(layoutInflater)
    }

    private var imageData: ChatData = ChatData()
    private var imageSize: String = ""
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
            if (binding.imageInfoLayout.visibility == View.VISIBLE) {
                hideImageInfo()
            } else if (supportActionBar?.isShowing == true) {
                supportActionBar?.hide()
            } else {
                supportActionBar?.show()
            }
        }

        if (binding.imageInfoLayout.visibility == View.VISIBLE) {
            binding.parentLayout.setOnClickListener {
                hideImageInfo()
            }
        }
    }

    private fun initImageData() {
        intent.apply {
            val fileName = getStringExtra("fileName")
            val bitmapForDetails = BitmapFactory.decodeStream(openFileInput(fileName))

            ExifInterface(openFileInput(fileName)).apply {
                imageSize =
                    getAttribute(ExifInterface.TAG_IMAGE_WIDTH).toString() + "x" + getAttribute(
                        ExifInterface.TAG_IMAGE_LENGTH
                    ).toString()
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
                ).format(imageData.timeInMillis)
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

    private fun saveImageFile() {
        try {
            var fos: OutputStream?
            val fileName = imageData.timeInMillis.toString()

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + File.separator + "Calenee"
                )
            }

            contentResolver.apply {
                val imageUri =
                    insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues).apply {
                        val id = this?.path?.split("/") ?: listOf()
                        Log.d("img_test/path", this?.path.toString())
                    }

                fos = imageUri?.let {
                    openOutputStream(it)
                }
            }

            fos?.use { outputStream ->
                imageData.bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                Snackbar.make(binding.root, "사진이 저장되었습니다", Snackbar.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showImageInfo() {
        binding.apply {
            imageInfoLayout.visibility = View.VISIBLE
            infoTypeTv.text = "JPEG"
            infoSizeTv.text = imageSize
        }
    }

    private fun hideImageInfo() {
        binding.apply {
            imageInfoLayout.visibility = View.GONE
        }
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
            R.id.chat_image_download -> {
                saveImageFile()
                Log.d("menu_test", "chat_image_download")
                return true
            }
            R.id.chat_image_share -> {
                Log.d("menu_test", "chat_image_share")
                return true
            }
            R.id.chat_image_info -> {
                showImageInfo()
                Log.d("menu_test", "info")
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (binding.imageInfoGl.visibility == View.VISIBLE) {
            hideImageInfo()
        } else {
            super.onBackPressed()
        }
    }
}