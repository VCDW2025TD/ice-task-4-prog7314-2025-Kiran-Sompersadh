package com.example.memestreamapp.model

import com.example.memestreamapp.Meme
import com.example.memestreamapp.MemeEntity
import java.util.UUID

// Converts API Meme model to Room entity
fun Meme.toEntity(): MemeEntity =
    MemeEntity(
        id = this.id ?: UUID.randomUUID().toString(), // generate UUID if id is null
        userId = this.userId,
        caption = this.caption,
        imageUrl = this.imageUrl,
        lat = this.lat,
        lng = this.lng,
        timestamp = this.timestamp,
        isSynced = true
    )

// Converts Room entity back to API model
fun MemeEntity.toApiModel(): Meme =
    Meme(
        id = this.id,
        userId = this.userId,
        caption = this.caption,
        imageUrl = this.imageUrl,
        lat = this.lat,
        lng = this.lng,
        timestamp = this.timestamp.toString()
    )
