package com.example.memestreamapp

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface MemeApi {
    @Multipart
    @POST("memes/upload")
    suspend fun uploadMeme(
        @Part image: MultipartBody.Part,
        @Part("userId") userId: RequestBody,
        @Part("caption") caption: RequestBody,
        @Part("lat") lat: RequestBody,
        @Part("lng") lng: RequestBody
    ): Response<Meme>


    @GET("memes")
    suspend fun getAllMemes(): List<Meme>

    @GET("memes")
    suspend fun getMemesByUser(@Query("userId") userId: String): List<Meme>

    @POST("memes")
    suspend fun postMeme(@Body meme: Meme): retrofit2.Response<Meme>
}

data class UploadResponse(
    val filename: String,
    val url: String  // URL that points to the uploaded image in GridFS
)