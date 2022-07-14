package org.jin.calenee.chat

data class SavedChatData(
    val createdAt: String ="",
    val message: String ="",
    val read: Boolean = false,
    val senderEmail: String = "",
    val senderNickname: String = "",
)