package org.jin.calenee.chat.notification

import com.google.gson.annotations.SerializedName

data class ChatNotificationBody(
    @SerializedName("to") val to: String,
    @SerializedName("priority") val priority: String,
    @SerializedName("data") val data: NotificationData,
)