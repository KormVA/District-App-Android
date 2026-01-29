package com.example.district.models

data class HouseNews(
    val id: Int,
    val title: String,
    val content: String,
    val date: String,
    val isUrgent: Boolean,
    val author: String
)