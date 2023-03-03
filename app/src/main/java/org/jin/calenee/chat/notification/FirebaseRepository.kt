package org.jin.calenee.chat.notification

import android.util.Log
import androidx.lifecycle.MutableLiveData
import okhttp3.ResponseBody
import retrofit2.Response

class FirebaseRepository {
    val myResponse : MutableLiveData<Response<ResponseBody>> = MutableLiveData()

    // send notification
    suspend fun sendNotification(notification: ChatNotificationBody) {
        myResponse.value = RetrofitInstance.api.sendNotification(notification)
        Log.d("fcm_test/response from server 1:", myResponse.value.toString())
    }
}