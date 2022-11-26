package org.jin.calenee.chat.notification

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.jin.calenee.App
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(App.FCM_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(provideOkHttpClient(AppInterCeptor()))
            .build()
    }

    val api: CaleneeNotificationService by lazy {
        retrofit.create(CaleneeNotificationService::class.java)
    }

    // client
    private fun provideOkHttpClient(
        interceptor: AppInterCeptor
    ): OkHttpClient = OkHttpClient.Builder().run {
        connectTimeout(30, TimeUnit.SECONDS)
        readTimeout(30, TimeUnit.SECONDS)
        writeTimeout(30, TimeUnit.SECONDS)
        addInterceptor(interceptor)
        build()
    }

    // add Header
    // Resources.getSystem().getString(R.string.fcm_key)
    class AppInterCeptor : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response = with(chain) {
            val newRequest = request().newBuilder()
                .addHeader(
                    "Authorization",
                    "key=AAAADSbZkjo:APA91bEbm_UGjP1RUs0jM9ul6Hr0nQSuNyhrouhnMrYj3uwDjp4Q_CEO5bwfmWaWKaqx4YytXtQ6XnV4Sc6Fj8ULn2ifePzi2dEq3hE60EykeCJP5kfvZTbJmUEIetekEv23uzMTryF3"
                )
                .addHeader("Content-Type", "application/json")
                .build()

            proceed(newRequest)
        }
    }
}