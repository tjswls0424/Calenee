package org.jin.calenee.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Calendar (
    @PrimaryKey val email: String,
    @ColumnInfo val date: String,
    @ColumnInfo val contents: String,
    @ColumnInfo val color: String,
)