package com.example.district.data.remote

import android.content.Context
import com.example.district.BuildConfig
import com.example.district.data.remote.interceptor.HmacInterceptor
import com.example.district.data.remote.interceptor.AuthInterceptor
import com.example.district.data.remote.api.ApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://district-app.ru/"

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val authInterceptor: AuthInterceptor
        get() = AuthInterceptor(appContext)

    private val hmacInterceptor: HmacInterceptor
        get() = HmacInterceptor(appSecret = BuildConfig.APP_SECRET)

    private val loggingInterceptor: HttpLoggingInterceptor
        get() = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    private val okHttpClient: OkHttpClient
        get() = OkHttpClient.Builder()
            .addInterceptor(hmacInterceptor)      // ← СНАЧАЛА HMAC
            .addInterceptor(authInterceptor)      // ← ПОТОМ ТОКЕН
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}