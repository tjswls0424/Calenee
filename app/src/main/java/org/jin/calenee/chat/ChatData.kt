package org.jin.calenee.chat

data class ChatData(
    var nickname: String = "",
    var message: String = "",
    var time: String = "",
    var viewType: Int
) {
    companion object {
        const val VIEW_TYPE_LEFT = 0
        const val VIEW_TYPE_RIGHT = 1
        const val VIEW_TYPE_CENTER = 2
    }
}