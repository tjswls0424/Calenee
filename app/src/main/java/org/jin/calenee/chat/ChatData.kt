package org.jin.calenee.chat

import android.graphics.Bitmap

data class ChatData(
    var viewType: Int,
    var nickname: String? = "",
    var message: String? = "",
    var time: String? = "",
    var bitmap: Bitmap? = null,
    var ratio: Double = 1.0
) {
    companion object {
        const val VIEW_TYPE_LEFT = 0
        const val VIEW_TYPE_RIGHT = 1
        const val VIEW_TYPE_CENTER = 2
        const val VIEW_TYPE_IMAGE = 3
    }
}