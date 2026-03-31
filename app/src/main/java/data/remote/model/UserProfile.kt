package com.example.district.models

data class UserProfile(
    val login: String,
    val displayName: String,
    val house: String,
    val address: String,
    val phone: String = ""
)