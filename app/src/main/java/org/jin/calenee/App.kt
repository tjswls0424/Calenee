package org.jin.calenee

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import org.jin.calenee.chat.notification.CaleneeFirebaseMessagingService
import org.jin.calenee.chat.notification.CaleneeWorker

class App : Application() {
    companion object {
        lateinit var userPrefs: UserPrefs
        const val FCM_BASE_URL = "https://fcm.googleapis.com/"
    }

    override fun onCreate() {
        userPrefs = UserPrefs(applicationContext)
//        subscribeTopic()

        startFCMService(this)
        super.onCreate()
    }
}

//private fun subscribeTopic() {
//    val coupleChatId = App.userPrefs.getString("couple_chat_id")
//    if (coupleChatId.isNotEmpty()) {
//        FirebaseMessaging.getInstance().subscribeToTopic(App.userPrefs.getString("couple"))
//    }
//}
//
//private fun unsubscribeTopic() {
//    val coupleChatId = App.userPrefs.getString("couple_chat_id")
//    FirebaseMessaging.getInstance().unsubscribeFromTopic(coupleChatId)
//}


private fun startFCMService(context: Context) {
    val intent = Intent(context, CaleneeFirebaseMessagingService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val request = OneTimeWorkRequest.Builder(CaleneeWorker::class.java)
            .addTag("BACKUP_WORKER_TAG").build()
        WorkManager.getInstance(context).enqueue(request)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
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