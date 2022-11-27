package org.jin.calenee.chat.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.jin.calenee.App
import org.jin.calenee.R
import org.jin.calenee.chat.ChattingActivity

class CaleneeFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        // putData 사용시
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("fcm_test", "Message data payload: ${remoteMessage.data}")

            Log.d("fcm_test/data-title", remoteMessage.data["title"].toString())
            Log.d("fcm_test/data-body", remoteMessage.data["message"].toString())
            Log.d("fcm_test/data-senderNickname", remoteMessage.data["senderNickname"].toString())

            val title = remoteMessage.data["title"].toString()
            val message = remoteMessage.data["message"].toString()
            val senderNickname = remoteMessage.data["senderNickname"].toString()

            sendNotification(title, message, senderNickname)

            // check if data needs to be processed by long running job
//            if (true) {
//                // for long-running tasks (10 sec or more) use Firebase Job Dispatcher
//                scheduleJob()
//            } else {
//                // handle message within 10 sec
//                handleNow()
//            }
        } else {
            // from server
            remoteMessage.notification?.let {
                Log.d("fcm_test", "Message Notification Title: ${it.title}")
                Log.d("fcm_test", "Message Notification Body: ${it.body}")

                sendNotificationFromServer(
                    remoteMessage.notification?.title.toString(),
                    remoteMessage.notification?.body.toString(),
                )
            }
        }
    }

    override fun onDeletedMessages() {
        Log.d("fcm_test", "onDeletedMessages()")
        super.onDeletedMessages()
    }

    override fun onMessageSent(msgId: String) {
        Log.d("fcm_test", "onMessageSent()")

        super.onMessageSent(msgId)
    }

    override fun onNewToken(token: String) {
        Log.d("fcm_test", "onNewToken()")

        sendRegistrationToServer(token)
    }

    private fun scheduleJob() {
        Log.d("fcm_test", "scheduleJob()")

        // 1
        // start dispatch_job
//        val work = OneTimeWorkRequest.Builder(CaleneeWorker::class.java).build()
//        WorkManager.getInstance().beginWith(work).enqueue()
        // end dispatch_job


        // 2
//        val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(this))
//        val myJob = dispatcher.newJobBuilder()
//            .setService(CaleneeJobService::class.java)
//            .setTag("calenee-job-tag")
//            .build()
//        dispatcher.schedule(myJob)
    }

    private fun handleNow() {
        Log.d("fcm_test", "handleNow()")
    }

    private fun sendRegistrationToServer(token: String?) {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email.toString()
        Firebase.firestore.collection("user")
            .document(currentUserEmail)
            .update("FCMToken", token)

        App.userPrefs.setString("my_fcm_token", token.toString())

        Log.d("fcm_test", "sendRegistrationTokenToServer($token)")
    }

    // 다른 기기에서 서버로 보냈을 때
    private fun sendNotification(title: String, body: String, senderNickname: String = "") {
        // 개별 알림 표시를 위한 id(requestCode) 값
        val uniId = System.currentTimeMillis().toInt()

        val intent = Intent(this, ChattingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent =
            PendingIntent.getActivity(this, uniId, intent, PendingIntent.FLAG_IMMUTABLE)

        // message style
        val user = androidx.core.app.Person.Builder()
            .setName(senderNickname)
            .setIcon(IconCompat.createWithResource(this, R.drawable.calenee_face))
            .build()

        val message = NotificationCompat.MessagingStyle.Message(
            body,
            System.currentTimeMillis(),
            user
        )

        val messageStyle = NotificationCompat.MessagingStyle(user).addMessage(message)

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.calenee_icon)
            .setColor(getColor(R.color.sub_color))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(messageStyle)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel =
            NotificationChannel(
                channelId,
                "새 메세지 알림",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                enableVibration(true)
                enableLights(true)
                setSound(defaultSoundUri, audioAttributes)
            }
        notificationManager.createNotificationChannel(channel)

        // id를 0으로 설정하면 상태바에에 최신 알림 하나만 보임
        notificationManager.notify(0, notificationBuilder.build())
    }

    // 서버에서 직접 보냈을 때
    private fun sendNotificationFromServer(title: String, body: String) {
        val uniId = System.currentTimeMillis().toInt()

        val intent = Intent(this, ChattingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent =
            PendingIntent.getActivity(this, uniId, intent, PendingIntent.FLAG_IMMUTABLE)

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.calenee_face)
            .setColor(getColor(R.color.sub_color))
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel =
            NotificationChannel(channelId, "알림", NotificationManager.IMPORTANCE_DEFAULT).apply {
                enableVibration(true)
                enableLights(true)
                setSound(defaultSoundUri, audioAttributes)
            }
        notificationManager.createNotificationChannel(channel)
        notificationManager.notify(0, notificationBuilder.build())
    }
}