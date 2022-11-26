package org.jin.calenee.chat.notification

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface CaleneeNotificationService {
    @POST("fcm/send")
    suspend fun sendNotification(
        @Body notification: ChatNotificationBody
    ): Response<ResponseBody>
}