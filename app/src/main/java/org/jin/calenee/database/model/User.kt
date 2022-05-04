package org.jin.calenee.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class User (
    @ColumnInfo val name: String?,
    @PrimaryKey val email: String,
    @ColumnInfo val pw: String,
    @ColumnInfo(name = "login_state") var loginState: Boolean = false
)