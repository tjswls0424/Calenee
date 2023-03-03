package org.jin.calenee.chat.notification

import com.google.gson.annotations.SerializedName

data class NotificationData(
    @SerializedName("senderNickname") val senderNickname: String,
    @SerializedName("message") val message: String,
    @SerializedName("receiverToken") val receiverToken: String,
)