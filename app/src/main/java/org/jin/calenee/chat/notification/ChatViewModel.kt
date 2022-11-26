package org.jin.calenee.chat.notification

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FirebaseRepository = FirebaseRepository()
    val myResponse = repository.myResponse

    // send push message
    fun sendNotification(notification: ChatNotificationBody) {
        viewModelScope.launch {
            repository.sendNotification(notification)
        }
    }
}