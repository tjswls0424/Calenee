package org.jin.calenee.chat

data class ChatProgressData (
    val progress: Double = 0.0,
    val progressUnit: String = "",
    val total: Double = 0.0,
    val totalUnit: String = "",
    val visibility: Boolean = false,
)