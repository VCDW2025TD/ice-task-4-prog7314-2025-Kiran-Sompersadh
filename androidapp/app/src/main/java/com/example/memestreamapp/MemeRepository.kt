package com.example.memestreamapp
import android.content.Context
import com.example.memestreamapp.model.toApiModel
import com.example.memestreamapp.model.toEntity


class MemeRepository(private val memeDao: MemeDao) {

    // Fetch memes: prefer API, fallback to local DB
    suspend fun getUserMemes(userId: String): List<MemeEntity> {
        return try {
            val apiMemes = ApiService.memeApi.getMemesByUser(userId)
            val entities = apiMemes.map { it.toEntity() }

            // Save to local DB
            memeDao.insert(entities)

            entities
        } catch (e: Exception) {
            // API failed, return cached memes
            memeDao.getUserMemes(userId)
        }
    }

    // Add a meme locally (offline)
    suspend fun addMemeOffline(meme: MemeEntity) {
        memeDao.insert(meme)
    }

    // Sync unsynced memes to API
    suspend fun syncPendingMemes() {
        val unsynced = memeDao.getUserMemes("") // modify query for isSynced=false
            .filter { !it.isSynced }

        for (m in unsynced) {
            try {
                val response = ApiService.memeApi.postMeme(m.toApiModel())
                if (response.isSuccessful) {
                    val syncedMeme = response.body()?.toEntity()
                    syncedMeme?.let { memeDao.insert(it) }
                }
            } catch (_: Exception) { }
        }
    }
}