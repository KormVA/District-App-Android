package com.example.district.data.remote.api

import com.example.district.data.remote.model.LoginRequest
import com.example.district.data.remote.model.LoginResponse
import com.example.district.data.remote.model.Profile
import com.example.district.data.remote.model.ProfileUpdate
import com.example.district.data.remote.model.AdvertResponse
import com.example.district.data.remote.model.RegisterRequest
import com.example.district.data.remote.model.RegisterResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

interface ApiService {
    @GET("/")
    suspend fun test(): Map<String, String>

    @GET("/profile/me")
    suspend fun getProfile(): Profile

    @PATCH("/profile/me")
    suspend fun updateProfile(@Body update: ProfileUpdate): Profile

    @POST("/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("/ads")
    suspend fun getAds(): List<AdvertResponse>

    @POST("/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse
}