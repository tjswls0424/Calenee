package org.jin.calenee.chat.notification

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.jin.calenee.App
import org.jin.calenee.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(App.FCM_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(provideOkHttpClient(AppInterceptor()))
            .build()
    }

    val api: CaleneeNotificationService by lazy {
        retrofit.create(CaleneeNotificationService::class.java)
    }

    // client
    private fun provideOkHttpClient(
        interceptor: AppInterceptor
    ): OkHttpClient = OkHttpClient.Builder().run {
        connectTimeout(30, TimeUnit.SECONDS)
        readTimeout(30, TimeUnit.SECONDS)
        writeTimeout(30, TimeUnit.SECONDS)
        addInterceptor(interceptor)
        build()
    }

    // add Header
    // Resources.getSystem().getString(R.string.fcm_key)
    class AppInterceptor : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response = with(chain) {
            val newRequest = request().newBuilder()
                .addHeader(
                    "Authorization",
                    "key=${BuildConfig.API_KEY_FCM}"
                )
                .addHeader("Content-Type", "application/json")
                .build()

            proceed(newRequest)
        }
    }
}