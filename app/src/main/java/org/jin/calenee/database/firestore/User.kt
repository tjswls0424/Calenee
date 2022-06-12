package org.jin.calenee.database.firestore

data class User(
    var gender: String = "",
    var nickname: String = "",
    var birthday: String = "",
    var firstMetDate: String = "",
)