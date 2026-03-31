package com.example.district.data.remote.model

data class RegisterResponse(
    val access_token: String,
    val refresh_token: String,
    val user: User
)