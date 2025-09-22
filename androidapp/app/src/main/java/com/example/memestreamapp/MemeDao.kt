package com.example.memestreamapp

import androidx.room.*


@Dao
interface MemeDao {

    @Query("SELECT * FROM memes WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getUserMemes(userId: String): List<MemeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memes: List<MemeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meme: MemeEntity)

    @Delete
    suspend fun delete(meme: MemeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeme(meme: MemeEntity)  // ✅ This is what your fragment is calling

}