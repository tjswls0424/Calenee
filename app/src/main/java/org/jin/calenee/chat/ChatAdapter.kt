package org.jin.calenee.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.gun0912.tedpermission.provider.TedPermissionProvider.context
import kotlinx.android.synthetic.main.chat_image_item.view.*
import org.jin.calenee.R
import java.lang.RuntimeException

class ChatAdapter(context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var data = mutableListOf<ChatData>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View?
        return when (viewType) {
            ChatData.VIEW_TYPE_LEFT -> {
                view = LayoutInflater.from(parent.context).inflate(
                    R.layout.chat_partner_item,
                    parent,
                    false
                )

                LeftViewHolder(view)
            }
            ChatData.VIEW_TYPE_RIGHT -> {
                view = LayoutInflater.from(parent.context).inflate(
                    R.layout.chat_mine_item,
                    parent,
                    false
                )

                RightViewHolder(view)
            }
            ChatData.VIEW_TYPE_CENTER -> {
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
            ChatData.VIEW_TYPE_LEFT -> {
                (holder as LeftViewHolder).bind(data[position])
                holder.setIsRecyclable(false)
            }
            ChatData.VIEW_TYPE_RIGHT -> {
                (holder as RightViewHolder).bind(data[position])
                holder.setIsRecyclable(false)
            }
            ChatData.VIEW_TYPE_CENTER -> {
                (holder as CenterViewHolder).bind(data[position])
                holder.setIsRecyclable(false)
            }
            ChatData.VIEW_TYPE_IMAGE -> {
                (holder as ImageViewHolder).bind(data[position])
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
        private val time = view.findViewById<TextView>(R.id.chat_image_time_text)
        private var image = view.findViewById<ImageView>(R.id.chat_image)

        val dp = 300
        val density = context.resources.displayMetrics.density
        val px = (dp * density).toInt()
        fun bind(item: ChatData) {
            Glide.with(itemView)
                .load(item.bitmap)
                .override(px, (px*item.ratio).toInt())
                .into(itemView.chat_image)

//            when {
//                item.ratio in 0.832..1.165 -> {
//                    // 1:1, 1.0
//                    Glide.with(image)
//                        .load(item.bitmap)
//                        .override(300, 300)
//                        .into(image)
//                }
//
//                item.ratio in 1.165..1.555 -> {
//                    // 3:4, 1.33
//                    Glide.with(image)
//                        .load(item.bitmap)
//                        .override(300, 300)
//                        .into(image)
//
//                }
//
//                item.ratio in 1.555..1.89 -> {
//                    // 9:16, 1.78
//                    Glide.with(image)
//                        .load(item.bitmap)
//                        .override(300, 300)
//                        .into(image)
//
//                }
//
//                item.ratio > 1.89 -> {
//                    // 1:2, 2.0
//                    Glide.with(image)
//                        .load(item.bitmap)
//                        .override(300, 300)
//                        .into(image)
//
//                }
//
//                else -> {
//                    // 1:1
//                    Glide.with(image)
//                        .load(item.bitmap)
//                        .override(300, 300)
//                        .into(image)
//                }
//            }

            time.text = item.time
            image.setImageBitmap(item.bitmap)
        }
    }

}