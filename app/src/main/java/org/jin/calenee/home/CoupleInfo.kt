package org.jin.calenee.home

import java.io.Serializable

data class CoupleInfo(
    val user1Nickname: String = "",
    val user2Nickname: String = "",
    val user1Birthday: String = "",
    val user2Birthday: String = "",
    val user1Message: String = "",
    val user2Message: String = "",
    val firstMetDate: String = "",
) : Serializable