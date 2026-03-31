package com.example.district.data.remote.model

data class ProfileUpdate(
    val phone: String? = null,
    val telegram: String? = null,
    val phoneVisible: Boolean? = null,
    val telegramVisible: Boolean? = null,
    val address: String? = null
)