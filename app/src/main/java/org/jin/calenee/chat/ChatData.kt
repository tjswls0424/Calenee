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
) {
    companion object {
        const val VIEW_TYPE_LEFT = 0
        const val VIEW_TYPE_RIGHT = 1
        const val VIEW_TYPE_CENTER = 2
        const val VIEW_TYPE_IMAGE = 3
    }
}