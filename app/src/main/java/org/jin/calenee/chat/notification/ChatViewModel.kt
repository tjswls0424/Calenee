package org.jin.calenee.chat.notification

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FirebaseRepository = FirebaseRepository()
    val result = repository.myResponse

    // send push notification
    fun sendNotification(notification: ChatNotificationBody) {
        viewModelScope.launch {
            repository.sendNotification(notification)
            Log.d("fcm_test/response from server 2:", result.value.toString())
        }
    }
}