package org.jin.calenee.chat.notification

data class ChatNotificationBody(
    val to: String,
    val data: ChatNotificationData,
) {
    class ChatNotificationData(
        val title: String = "",
        val message: String = "",
        val senderNickname: String = "",
    )
}