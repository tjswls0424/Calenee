package org.jin.calenee.chat

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

data class ChatData(
    var viewType: Int = -1,
    var nickname: String? = "",
    var message: String? = "",
    var time: String? = "", // HH:mm
    var timeInMillis: Long = 0L,
    var bitmap: Bitmap? = null,
    var ratio: Double = -1.0,
    var tmpIndex: Int = 0,
    var dataType: String = "text",
    var isMyChat: Boolean = true,
) {
    companion object {
        const val VIEW_TYPE_LEFT_TEXT = 0
        const val VIEW_TYPE_RIGHT_TEXT = 1
        const val VIEW_TYPE_CENTER_TEXT = 2
        const val VIEW_TYPE_IMAGE = 3
    }
}