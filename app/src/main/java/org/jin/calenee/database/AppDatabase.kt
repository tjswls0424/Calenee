package org.jin.calenee.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.jin.calenee.database.dao.CalendarDao
import org.jin.calenee.database.model.Calendar

@Database(entities = [Calendar::class], version = 1, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {
    abstract fun calendarDao(): CalendarDao

    companion object {
        private var instance: AppDatabase? = null

        @Synchronized
        fun getInstance(context: Context, dbName: String = "none"): AppDatabase? {
            if (instance == null) {
                synchronized(AppDatabase::class) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        dbName
                    ).build()
                }
            }

            return instance
        }


    }
}