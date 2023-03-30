package org.jin.calenee.data.firestore

data class CoupleInfoSync(
    val firstMetDate: String = "", // firstMetDate
    val homeBackgroundPath: String = "", // split해서 name만
//    val homeBackground: Int, // X
//    val homeTextColor: Int, // X
//    val user1Email: String, // X
//    val user1Nickname: String, // X
//    val user1Birthday: String, // X
    val user1Message: String = "", // 이하 동일
    val user1MessageAlignment: Int = 0,
    val user1MessageColor: Int = 0,
    val user1MessagePosition: Int = 0,
    val user1MessageSize: Int = 0,
//    val user2Email: String, // X
//    val user2Nickname: String, // X
//    val user2Birthday: String, // X
    val user2Message: String = "",
    val user2MessageAlignment: Int = 0,
    val user2MessageColor: Int = 0,
    val user2MessagePosition: Int = 0,
    val user2MessageSize: Int = 0,
)
