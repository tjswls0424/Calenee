package org.jin.calenee.chat

data class SavedChatData(
    val createdAt: String? ="",
    val message: String? ="",
    val senderEmail: String? = "",
    val senderNickname: String? = "",
    val fileAbsolutePath: String? = "",
    val fileName: String? = "",
    val fileRatio: Double = 0.5,
    val dataType: String = "text",
    val duration: String = "",
//    val read: Boolean? = false,
)