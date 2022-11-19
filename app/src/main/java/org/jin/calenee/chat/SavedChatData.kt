package org.jin.calenee.chat

data class SavedChatData(
    val createdAt: String? ="",
    val message: String? ="",
    val senderEmail: String? = "",
    val senderNickname: String? = "",
    val fileAbsolutePath: String? = "",
    val fileName: String? = "",
    val fileRatio: Double = 0.5,
    val mimeType: String = "text/*",
    val duration: String = "",
    val expirationDate: String = "",
    val fileSize: Long = 0L,
//    val read: Boolean? = false,
)