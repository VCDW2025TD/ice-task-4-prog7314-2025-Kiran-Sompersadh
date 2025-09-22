package com.example.memestreamapp

data class Meme(
    val id: String? = null,   // Optional because your API might auto-generate it
    val userId: String,
    val imageUrl: String,
    val caption: String,
    val lat: Double,
    val lng: Double,
    val timestamp: String
)