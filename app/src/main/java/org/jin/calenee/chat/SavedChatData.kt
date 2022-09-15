package org.jin.calenee.chat

data class SavedChatData(
    val createdAt: String? ="",
    val message: String? ="",
    val senderEmail: String? = "",
    val senderNickname: String? = "",
    val fileAbsolutePath: String? = "",
    val fileRelativePath: String? = "",
    val fileRatio: Double = 0.5,
    val fileType: String? = "",
//    val read: Boolean? = false,
)