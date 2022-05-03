package org.jin.calenee.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class User (
    @PrimaryKey val email: String,
    @ColumnInfo val name: String?,
    @ColumnInfo(name = "login_state") var loginState: Boolean = false
)