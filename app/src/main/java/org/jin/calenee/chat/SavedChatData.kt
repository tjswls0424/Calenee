package org.jin.calenee.chat

data class SavedChatData(
    val createdAt: String? ="",
    val message: String? ="",
    val senderEmail: String? = "",
    val senderNickname: String? = "",
//    val read: Boolean? = false,
)