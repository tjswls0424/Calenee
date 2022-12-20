package org.jin.calenee

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.*
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
        saveFCMToken()
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

// get token (mine, partner), save token to SP & Firestore
fun saveFCMToken() {
    val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email.toString()
    CoroutineScope(Dispatchers.IO).launch {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.d("fcm_test", "fetching FCM registration token failed")
                return@addOnCompleteListener
            }

            val token = task.result
            App.userPrefs.setString("my_fcm_token", token)

            Firebase.firestore.collection("user")
                .document(currentUserEmail)
                .update("FCMToken", token)
        }

        launch {
            Firebase.firestore.collection("user").document(currentUserEmail)
                .addSnapshotListener { value, error ->
                    if (value?.get("partnerEmail").toString().isNotEmpty()) {
                        val partnerEmail = value?.get("partnerEmail").toString()
                        Firebase.firestore.collection("user").document(partnerEmail)
                            .addSnapshotListener { value2, _ ->
                                val tmpToken = value2?.get("FCMToken").toString()
                                if (tmpToken.isNotEmpty()) {
                                    App.userPrefs.setString("partner_fcm_token", tmpToken)
                                    Firebase.firestore.collection("user")
                                        .document(currentUserEmail)
                                        .update("partnerFCMToken", tmpToken)
                                }
                            }
                    }
                }
        }
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

    fun updateUserNickname(nickname: String, isMine: Boolean) {
        if (isMine) {
            setString("current_nickname", nickname)
        } else {
            setString("current_partner_nickname", nickname)
        }
    }

    fun updateUserBirthday(birthday: String, isMine: Boolean) {
        if (isMine) {
            setString("current_birthday", birthday)
        } else {
            setString("current_partner_birthday", birthday)
        }
    }

    fun updateFirstMetDate(date: String) {
        setString("firstMetDate", date)
    }
}