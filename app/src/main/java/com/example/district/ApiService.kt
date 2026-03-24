package com.example.district.data.remote.api

import com.example.district.data.remote.model.Profile
import com.example.district.data.remote.model.ProfileUpdate
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface ApiService {
    @GET("/")
    suspend fun test(): Map<String, String>

    @GET("/profile/me")
    suspend fun getProfile(): Profile

    @PATCH("/profile/me")
    suspend fun updateProfile(@Body update: ProfileUpdate): Profile
}