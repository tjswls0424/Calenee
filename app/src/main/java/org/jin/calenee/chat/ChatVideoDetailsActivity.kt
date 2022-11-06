package org.jin.calenee.chat

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import androidx.core.content.FileProvider
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.material.snackbar.Snackbar
import org.jin.calenee.R
import org.jin.calenee.databinding.ActivityChatVideoDetailsBinding
import java.io.File
import java.io.OutputStream

class ChatVideoDetailsActivity : AppCompatActivity() {
    private val binding: ActivityChatVideoDetailsBinding by lazy {
        ActivityChatVideoDetailsBinding.inflate(layoutInflater)
    }
    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(this).build()
    }
    private val muteBtn by lazy {
        findViewById<ImageButton>(R.id.exo_volume_mute_btn)
    }


    //    private val muteBtn = findViewById<ImageButton>(R.id.exo_volume_mute_btn)
    private var videoPath: String = ""
    private var videoUri: Uri = Uri.EMPTY
    private var isVolumeMuted: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setToolbar()
        listener()

        initPlayer()
        setContentView(binding.root)
    }

    private fun listener() {
        binding.playerView.setOnClickListener {
            if (binding.videoInfoLayout.visibility == View.VISIBLE) {
                hideVideoInfo()
            } else if (supportActionBar?.isShowing == true) {
                supportActionBar?.hide()
                binding.playerView.hideController()
            } else {
                supportActionBar?.show()
                binding.playerView.showController()
            }
        }

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {

                    }
                    Player.STATE_BUFFERING -> {
                        // 재생 준비
                    }
                    Player.STATE_IDLE -> {
                        // 재생 실패
                    }
                    Player.STATE_ENDED -> {
                        // 재생 마침침
                    }
                }
                super.onPlaybackStateChanged(playbackState)
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                super.onPlayWhenReadyChanged(playWhenReady, reason)
            }
        })

//        muteBtn.setOnClickListener {
//            if (isVolumeMuted) {
//                // muted X
//                binding.playerView.player?.isDeviceMuted = false
//                muteBtn.setBackgroundResource(R.drawable.ic_baseline_volume_up_24)
//                isVolumeMuted = false
//            } else {
//                // muted
//                binding.playerView.player?.isDeviceMuted = true
//                muteBtn.setBackgroundResource(R.drawable.ic_baseline_volume_off_24)
//                isVolumeMuted = true
//            }
//        }
    }

    private fun setToolbar() {
        binding.toolbar.bringToFront()
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun initPlayer() {
        videoPath = intent.getStringExtra("filePath").toString()
        val file = File(videoPath)
        videoUri =
            FileProvider.getUriForFile(this, "org.jin.calenee.fileprovider", file, file.name)

        exoPlayer.setMediaItem(MediaItem.fromUri(videoUri))
        binding.playerView.player = exoPlayer
        exoPlayer.prepare()
    }

    private fun saveVideoFile() {
        try {
            val fos: OutputStream?
            val file = File(videoPath)

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_MOVIES + File.separator + "Calenee"
                )
            }

            val uri =
                contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
            fos = uri?.let { contentResolver.openOutputStream(it) }
            fos.use {
                fos?.write(file.readBytes())
            }

            Snackbar.make(binding.root, "저장되었습니다.", Snackbar.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun shareVideo() {
        val videoUri = FileProvider.getUriForFile(
            this@ChatVideoDetailsActivity,
            "org.jin.calenee.fileprovider",
            File(videoPath)
        )

        Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_STREAM, videoUri)
            startActivity(Intent.createChooser(this, "공유"))
        }
    }

//    private fun showVideoInfo() {
//        val exif = ExifInterface(File(videoPath))
//        val videoSize =
//            exif.getAttribute(ExifInterface.TAG_THUMBNAIL_IMAGE_WIDTH) + "x" + exif.getAttribute(
//                ExifInterface.TAG_THUMBNAIL_IMAGE_LENGTH
//            )
//
//        binding.apply {
//            videoInfoLayout.visibility = View.VISIBLE
//            infoTypeTv.text = "MP4"
//            infoSizeTv.text = videoSize
//        }
//    }

    private fun hideVideoInfo() {
        binding.apply {
            videoInfoLayout.visibility = View.GONE
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
                saveVideoFile()
                true
            }
            R.id.chat_media_share -> {
                shareVideo()
                true
            }
            R.id.chat_media_info -> {
//                showVideoInfo()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (binding.videoInfoLayout.visibility == View.VISIBLE) {
            hideVideoInfo()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        exoPlayer.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()

        exoPlayer.apply {
            stop()
            playWhenReady = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
    }
}