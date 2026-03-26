package com.example.district.data.remote.interceptor

import android.content.Context
import com.example.district.security.SecureAuth
import okhttp3.Interceptor
import okhttp3.Response
import android.util.Log

class AuthInterceptor(
    private val context: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = SecureAuth(context).getToken()

        val request = if (token != null && token.isNotBlank()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        Log.d("AUTH", "Token present: ${token != null}")
        return chain.proceed(request)
    }
}