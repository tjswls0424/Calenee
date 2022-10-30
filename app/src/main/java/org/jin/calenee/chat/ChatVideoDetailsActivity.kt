package org.jin.calenee.chat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import org.jin.calenee.databinding.ActivityChatVideoDetailsBinding
import java.io.File

class ChatVideoDetailsActivity : AppCompatActivity() {
    private val binding: ActivityChatVideoDetailsBinding by lazy {
        ActivityChatVideoDetailsBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        val filePath = intent.getStringExtra("filePath").toString()
        val tmpFile = File(filePath)
        if( tmpFile.isFile) {
            Glide.with(applicationContext)
                .load(tmpFile)
                .into(binding.thumbnailView)
        }
    }
}