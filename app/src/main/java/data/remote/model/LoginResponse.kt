package com.example.district.data.remote.model

data class LoginResponse(
    val access_token: String,
    val refresh_token: String,
    val user: User
)

data class User(
    val id: Int,
    val username: String,
    val display_name: String?,
    val address: String
)