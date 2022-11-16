package org.jin.calenee.chat

import android.content.ContentValues
import android.content.Intent
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
import androidx.core.content.FileProvider
import com.google.android.material.snackbar.Snackbar
import org.jin.calenee.R
import org.jin.calenee.databinding.ActivityChatImageDetailsBinding
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class ChatImageDetailsActivity : AppCompatActivity() {

    private val binding: ActivityChatImageDetailsBinding by lazy {
        ActivityChatImageDetailsBinding.inflate(layoutInflater)
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
                time = getStringExtra("time"),
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
        binding.toolbar.bringToFront()
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
//                        val id = this?.path?.split("/") ?: listOf()
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

    private fun saveBitmapToCacheDir(bitmap: Bitmap?) {
        val bos = ByteArrayOutputStream()
        bos.use {
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }

        val cacheFile = File(applicationContext.cacheDir, "calenee_${imageData.timeInMillis}.jpg")
        try {
            cacheFile.createNewFile()
            val fos = FileOutputStream(cacheFile, false)
            fos.use {
                bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, it)
//                it.write(bos.toByteArray())
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun clearFileCache(cacheFile: File) {
        cacheFile.delete()
        applicationContext.deleteFile(cacheFile.name)
        Log.d("del_test", "deleted cache file")
    }

    private fun getCacheFile(): File =
        File(applicationContext.cacheDir, "calenee_${imageData.timeInMillis}.jpg")


    private fun shareImage(cacheFile: File) {
        val bitmapUri = FileProvider.getUriForFile(
            this@ChatImageDetailsActivity,
            "org.jin.calenee.fileprovider",
            cacheFile
        )

        Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_STREAM, bitmapUri)
            startActivity(Intent.createChooser(this, "공유"))
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
        return when (item.itemId) {
            R.id.chat_media_download -> {
                saveImageFile()
                true
            }
            R.id.chat_media_share -> {
                saveBitmapToCacheDir(imageData.bitmap)
                shareImage(getCacheFile())
                true
            }
            R.id.chat_media_info -> {
                showImageInfo()
                Log.d("menu_test", "info")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (binding.imageInfoLayout.visibility == View.VISIBLE) {
            hideImageInfo()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        clearFileCache(getCacheFile())
        super.onDestroy()
    }
}