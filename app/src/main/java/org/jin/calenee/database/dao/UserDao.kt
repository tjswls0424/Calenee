package org.jin.calenee.database.dao

import androidx.room.*
import org.jin.calenee.database.model.User

@Dao
interface UserDao {
    @Query("SELECT * FROM user")
    fun getAll(): List<User>

    // email, name으로 search
    @Query("SELECT * FROM user WHERE email IN (:userEmail)")
    fun loadUserInfo(userEmail: String): User

    @Insert
    fun insertUserInfo(userInfo: User)

    // reset PW
    @Update
    fun updateUserInfo(userInfo: User)

    @Delete
    fun deleteUserInfo(user: User)
}