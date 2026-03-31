package com.example.district.data.remote.model

data class AdvertResponse(
    val id: Int,
    val title: String,
    val description: String,
    val price: Double,
    val user_id: Int,
    val address: String,
    val house_id: Int,
    val created_at: String,
    val username: String,
    val display_name: String?,
    val phone: String?,
    val telegram: String?,
    val phone_visible: Boolean,
    val telegram_visible: Boolean
)