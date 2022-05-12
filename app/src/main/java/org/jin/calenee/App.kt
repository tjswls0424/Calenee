package org.jin.calenee

import android.app.Application
import android.content.Context
import android.content.SharedPreferences

class App : Application() {
    companion object {
        lateinit var userPrefs: UserPrefs
    }

    override fun onCreate() {
        userPrefs = UserPrefs(applicationContext)
        super.onCreate()
    }
}

class UserPrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun getString(key: String, defValue: String = ""): String {
        return prefs.getString(key, defValue).toString()
    }

    fun setString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun deleteString(key: String) {
        prefs.edit().remove(key).apply()
    }
}