package com.example.memestreamapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memes")
data class MemeEntity(
    @PrimaryKey val id: String,        // _id from API
    val userId: String,
    val caption: String,
    val imageUrl: String,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val timestamp: String? = null,
    val isSynced: Boolean = true       // mark if it’s uploaded
)