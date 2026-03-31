package com.example.district.data.remote.model

data class Profile(
    val id: Int,
    val username: String,
    val displayName: String?,
    val address: String,
    val phone: String?,
    val telegram: String?,
    val phoneVisible: Boolean,
    val telegramVisible: Boolean
)