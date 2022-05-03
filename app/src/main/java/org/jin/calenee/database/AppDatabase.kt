package org.jin.calenee.database

import androidx.room.Database
import androidx.room.RoomDatabase
import org.jin.calenee.database.dao.CalendarDao
import org.jin.calenee.database.dao.UserDao
import org.jin.calenee.database.model.Calendar
import org.jin.calenee.database.model.User

@Database(entities = [User::class, Calendar::class], version = 1, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun calendarDao(): CalendarDao

    companion object {
        private var instance: AppDatabase? = null



    }
}