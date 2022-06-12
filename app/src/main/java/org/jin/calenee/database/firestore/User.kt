package org.jin.calenee.database.firestore

import android.graphics.Bitmap

data class User(
    var gender: String = "",
    var nickname: String = "",
    var birthday: String = "",
    var firstMetDate: String = "",
    var profileImage: Bitmap? = null,
)