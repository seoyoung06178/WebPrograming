package com.example.webprograming.model

data class TravelRecord(
    val id: Long = 0,
    val title: String,
    val visitDate: String,
    val memo: String = "",
    val photoPath: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val createdAt: String = ""
)
