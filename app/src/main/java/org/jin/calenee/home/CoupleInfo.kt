package org.jin.calenee.home

import java.io.Serializable

data class CoupleInfo(
    var user1Nickname: String = "",
    var user2Nickname: String = "",
    var user1Birthday: String = "",
    var user2Birthday: String = "",
    var user1Message: String = "",
    var user2Message: String = "",
    var firstMetDate: String = "",
    var days: String = "0일",
) : Serializable