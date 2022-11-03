package org.jin.calenee.chat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.FileProvider
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import org.jin.calenee.R
import org.jin.calenee.databinding.ActivityChatVideoDetailsBinding
import org.jin.calenee.databinding.PlayerControlViewBinding
import java.io.File

class ChatVideoDetailsActivity : AppCompatActivity() {
    private val binding: ActivityChatVideoDetailsBinding by lazy {
        ActivityChatVideoDetailsBinding.inflate(layoutInflater)
    }
    private val controllerBinding: PlayerControlViewBinding by lazy {
        PlayerControlViewBinding.inflate(layoutInflater)
    }

    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(this).build()
    }

    private var isVolumeMuted: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setToolbar()
        listener()

        initPlayer()
//        buildMediaSource()


//        if( tmpFile.isFile) {
//            Glide.with(applicationContext)
//                .load(tmpFile)
//                .into(binding.thumbnailView)
//        }
    }

    private fun listener() {
        binding.playerView.setOnClickListener {
            if (supportActionBar?.isShowing == true) {
                supportActionBar?.hide()
                binding.playerView.hideController()
//                binding.playerControlView.hide()
//                binding.controllerLayout1.visibility = View.GONE
            } else {
                supportActionBar?.show()
                binding.playerView.showController()

//                binding.playerControlView.show()

//                binding.controllerLayout1.visibility = View.VISIBLE
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

        controllerBinding.exoVolumeMuteBtn.setOnClickListener { btn ->
            if (isVolumeMuted) {
                //////////////////////////////
                /////////////
                controllerBinding.exoVolumeMuteBtn.background = AppCompatResources.getDrawable(this, R.drawable.ic_baseline_volume_up_24 )
                // muted X
                isVolumeMuted = false
                btn.setBackgroundResource(R.drawable.ic_baseline_volume_up_24)
                Log.d("mute_test", "volume on")
            } else {
                // muted
                isVolumeMuted = true
                btn.setBackgroundResource(R.drawable.ic_baseline_volume_off_24)
                Log.d("mute_test", "volume off")
            }
        }
    }

    private fun setToolbar() {
        binding.toolbar.bringToFront()
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun initVideoData() {

    }


    private fun initPlayer() {
        val filePath = intent.getStringExtra("filePath").toString()
        val videoUri =
            FileProvider.getUriForFile(this, "org.jin.calenee.fileprovider", File(filePath))

        exoPlayer.setMediaItem(MediaItem.fromUri(videoUri))
        binding.playerView.player = exoPlayer
        exoPlayer.prepare()

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
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