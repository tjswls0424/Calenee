package org.jin.calenee.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.gun0912.tedpermission.provider.TedPermissionProvider.context
import org.jin.calenee.R
import java.lang.RuntimeException

class ChatAdapter(context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var listener: OnItemClickListener? = null
    var data = mutableListOf<ChatData>()

    interface OnItemClickListener {
        fun onItemClick(v: View, data: ChatData, position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View?
        return when (viewType) {
            ChatData.VIEW_TYPE_LEFT_TEXT -> {
                view = LayoutInflater.from(parent.context).inflate(
                    R.layout.chat_partner_item,
                    parent,
                    false
                )

                LeftViewHolder(view)
            }
            ChatData.VIEW_TYPE_RIGHT_TEXT -> {
                view = LayoutInflater.from(parent.context).inflate(
                    R.layout.chat_mine_item,
                    parent,
                    false
                )

                RightViewHolder(view)
            }
            ChatData.VIEW_TYPE_CENTER_TEXT -> {
                view = LayoutInflater.from(parent.context).inflate(
                    R.layout.chat_date_item,
                    parent,
                    false
                )

                CenterViewHolder(view)
            }
            ChatData.VIEW_TYPE_IMAGE -> {
                view = LayoutInflater.from(parent.context).inflate(
                    R.layout.chat_image_item,
                    parent,
                    false
                )

                ImageViewHolder(view)
            }
            else -> throw RuntimeException("unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (data[position].viewType) {
            ChatData.VIEW_TYPE_LEFT_TEXT -> {
                (holder as LeftViewHolder).bind(data[position])
                holder.setIsRecyclable(false)
            }
            ChatData.VIEW_TYPE_RIGHT_TEXT -> {
                (holder as RightViewHolder).bind(data[position])
                holder.setIsRecyclable(false)
            }
            ChatData.VIEW_TYPE_CENTER_TEXT -> {
                (holder as CenterViewHolder).bind(data[position])
                holder.setIsRecyclable(false)
            }
            ChatData.VIEW_TYPE_IMAGE -> {
                (holder as ImageViewHolder).bind(data[position], position)
                holder.setIsRecyclable(false)
            }
            else -> throw RuntimeException("unknown view type")
        }
    }

    override fun getItemCount(): Int = data.size

    override fun getItemViewType(position: Int): Int = data[position].viewType


    // partner
    inner class LeftViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val nickname = view.findViewById<TextView>(R.id.partner_nickname_text)
        private val message = view.findViewById<TextView>(R.id.partner_msg_text)
        private val time = view.findViewById<TextView>(R.id.partner_time_text)

        fun bind(item: ChatData) {
            nickname.text = item.nickname
            message.text = item.message
            time.text = item.time
        }
    }

    // mine
    inner class RightViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val message = view.findViewById<TextView>(R.id.my_msg_text)
        private val time = view.findViewById<TextView>(R.id.my_time_text)

        fun bind(item: ChatData) {
            message.text = item.message
            time.text = item.time
        }
    }

    // time stamp
    inner class CenterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val time = view.findViewById<TextView>(R.id.chat_date_text)

        // db에서 받아올 때 해당 날짜에 첫 번째 메세지인 경우(index = 0) 추가
        fun bind(item: ChatData) {
            time.text = item.time
        }
    }

    // image file
    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        private val parentLayout = view.findViewById<ConstraintLayout>(R.id.chat_image_layout)
        private val timeTextView = view.findViewById<TextView>(R.id.chat_image_time_text)
        private var imageView = view.findViewById<ImageView>(R.id.chat_image)

        private val dp = 250 // can be modified as needed
        private val density = context.resources.displayMetrics.density
        private val width = (dp * density).toInt()

        fun bind(item: ChatData, position: Int) {
            if (!item.isMyChat) {
                // chat data from partner
                imageView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    endToEnd = ConstraintLayout.LayoutParams.UNSET
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                }

                timeTextView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    endToStart = ConstraintLayout.LayoutParams.UNSET
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    startToEnd = imageView.id
                }
            }

            val loadingBitmap = imageView.background.toBitmap(300, 300)
            if (item.bitmap != null && item.time != "") {
                Glide.with(context)
                    .load(item.bitmap)
                    .override(width, (width * item.ratio).toInt())
                    .fallback(R.drawable.chat_item_background)
                    .centerCrop()
                    .into(imageView)
            } else {
                // first loading when enter chat room
                Glide.with(context)
                    .load(loadingBitmap)
                    .override(width, (width * item.ratio).toInt())
                    .centerCrop()
                    .into(imageView)
            }
            timeTextView.text = item.time

            // preload
            if (position <= data.size) {
                val endPosition = if (position + 6 > data.size) {
                    data.size
                } else {
                    position + 6
                }

                data.subList(position, endPosition).map { it.bitmap }.forEach {
                    if (it != null) {
                        Glide.with(context)
                            .load(it)
                            .preload(width, (width * item.ratio).toInt())
                    } else {
                        Glide.with(context)
                            .load(loadingBitmap)
                            .preload(width, (width * item.ratio).toInt())
                    }
                }
            }

            // listener
            if (position != RecyclerView.NO_POSITION) {
                imageView.setOnClickListener {
                    listener?.onItemClick(imageView, data[position], position)
                }
            }
        }
    }

}