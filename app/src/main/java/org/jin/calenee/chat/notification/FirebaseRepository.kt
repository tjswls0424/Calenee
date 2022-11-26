package org.jin.calenee.chat.notification

import androidx.lifecycle.MutableLiveData
import okhttp3.ResponseBody
import retrofit2.Response

class FirebaseRepository {
    val myResponse : MutableLiveData<Response<ResponseBody>> = MutableLiveData()

    // send notification
    suspend fun sendNotification(notification: ChatNotificationBody) {
        myResponse.value = RetrofitInstance.api.sendNotification(notification)
    }
}