package com.example.district.data.remote.model

data class ProfileUpdate(
    val phone: String? = null,
    val telegram: String? = null,
    val phone_visible: Boolean? = null,
    val telegram_visible: Boolean? = null,
    val address: String? = null
)