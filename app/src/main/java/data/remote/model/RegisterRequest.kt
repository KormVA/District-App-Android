package com.example.district.data.remote.model

data class RegisterRequest(
    val username: String,
    val password: String,
    val display_name: String,
    val address: String
)