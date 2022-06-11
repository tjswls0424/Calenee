package org.jin.calenee.database.firestore

data class Couple(
    var user1Email: String = "",
    var user2Email: String = "",
    var firstMetDate: String = "",
    var connectionFlag: Boolean = true, // for couple disconnected
)