package com.example.district.data.remote

import com.example.district.data.remote.interceptor.HmacInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.example.district.BuildConfig

object RetrofitClient {

    private const val BASE_URL = "https://district-app.ru/"

    private val hmacInterceptor = HmacInterceptor(
        appSecret = BuildConfig.APP_SECRET
    )

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(hmacInterceptor)
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