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
import org.jin.calenee.data.firestore.CoupleInfoSync
import org.jin.calenee.data.firestore.UserSync

class App : Application() {
    companion object {
        lateinit var userPrefs: UserPrefs
        const val FCM_BASE_URL = "https://fcm.googleapis.com/"
    }

    override fun onCreate() {
//        subscribeTopic()
        userPrefs = UserPrefs(applicationContext)

        if (userPrefs.getString("login_status") == "true") {
            startFCMService(this)
            saveFCMToken()
        }

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
private fun saveFCMToken() {
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
                .update("fcmToken", token)
        }

        launch {
            Firebase.firestore.collection("user").document(currentUserEmail)
                .addSnapshotListener { value, error ->
                    if (value?.get("partnerEmail").toString().isNotEmpty()) {
                        val partnerEmail = value?.get("partnerEmail").toString()
                        Firebase.firestore.collection("user").document(partnerEmail)
                            .addSnapshotListener { value2, _ ->
                                val partnerToken = value2?.get("fcmToken").toString()
                                if (partnerToken.isNotEmpty()) {
                                    App.userPrefs.setString("partner_fcm_token", partnerToken)
                                    Firebase.firestore.collection("user")
                                        .document(currentUserEmail)
                                        .update("partnerFCMToken", partnerToken)
                                }
                            }
                    }
                }
        }
    }
}

class UserPrefs(context: Context, fileName: String = "user_prefs") {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(fileName, Context.MODE_PRIVATE)

    fun getString(key: String, defValue: String = ""): String {
        return prefs.getString(key, defValue).toString()
    }

    fun setString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    private fun deleteString(key: String) {
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

    fun updateTodayMessageInfo(
        position: Int,
        message: String,
        messagePosition: Int?,
        alignment: Int,
        textSize: Int,
        textColor: Int
    ) {
        setString("user${position}Message", message)
        setString("user${position}MessagePosition", messagePosition.toString())
        setString("user${position}MessageAlignment", alignment.toString())
        setString("user${position}MessageSize", textSize.toString())
        setString("user${position}MessageColor", textColor.toString())
    }

    // sync user data
    fun updateUserData(user: UserSync, email: String) {
        setString("my_fcm_token", user.fcmToken)
        setString("current_nickname", user.nickname)
        setString("current_birthday", user.birthday)
        setString("current_gender", user.gender)
        setString("couple_chat_id", user.coupleChatID)
        setString("current_partner_email", user.partnerEmail)
        setString("partner_fcm_token", user.partnerFCMToken)
        setString("${email}_couple_connection_flag", user.coupleConnectionFlag.toString())
        setString("${email}_profile_image_flag", user.profileImageFlag.toString())
        setString("${email}_couple_input_flag", user.profileInputFlag.toString())
    }

    // sync couple info data
    fun updateCoupleInfoData(coupleInfo: CoupleInfoSync) {
        setString("firstMetDate", coupleInfo.firstMetDate)
        setString("home_background_name", coupleInfo.homeBackgroundPath.split("/").last())
        setString("user1Message", coupleInfo.user1Message)
        setString("user1MessageAlignment", coupleInfo.user1MessageAlignment.toString())
        setString("user1MessageColor", coupleInfo.user1MessageColor.toString())
        setString("user1MessagePosition", coupleInfo.user1MessagePosition.toString())
        setString("user1MessageSize", coupleInfo.user1MessageSize.toString())
        setString("user2Message", coupleInfo.user2Message)
        setString("user2MessageAlignment", coupleInfo.user2MessageAlignment.toString())
        setString("user2MessageColor", coupleInfo.user2MessageColor.toString())
        setString("user2MessagePosition", coupleInfo.user2MessagePosition.toString())
        setString("user2MessageSize", coupleInfo.user2MessageSize.toString())
    }

    // when logging out
    fun clearUserData(email: String) {
        Log.d("context_test/prefs", "clearUserData()")

        deleteString("login_status")
        deleteString("firstMetDate")
        deleteString("home_background_name")
        deleteString("chat_last_msg_time")

        deleteString("my_fcm_token")
        deleteString("partner_fcm_token")

//        deleteString("couple_chat_id")

        deleteString("current_email")
        deleteString("current_nickname")
        deleteString("current_name")
        deleteString("current_birthday")
        deleteString("current_gender")
        deleteString("current_partner_email")
        deleteString("current_partner_nickname")
        deleteString("current_partner_birthday")
        deleteString("${email}_couple_connection_flag")
        deleteString("${email}_profile_image_flag")
        deleteString("${email}_couple_input_flag")

        deleteString("user1Message")
        deleteString("user1MessageAlignment")
//        deleteString("user1MessageColor")
        deleteString("user1MessagePosition")
        deleteString("user1MessageSize")
        deleteString("user2Message")
        deleteString("user2MessageAlignment")
//        deleteString("user2MessageColor")
        deleteString("user2MessagePosition")
        deleteString("user2MessageSize")
    }
}