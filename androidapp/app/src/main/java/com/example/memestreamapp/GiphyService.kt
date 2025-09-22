package com.example.memestreamapp

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class GiphyData(val id: String, val images: GiphyImages)
data class GiphyImages(val original: GiphyOriginal)
data class GiphyOriginal(val url: String)
data class GiphyResponse(val data: List<GiphyData>)

interface GiphyApi {
    @GET("trending")
    suspend fun getTrending(
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = 25
    ): GiphyResponse
}

object GiphyService {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.giphy.com/v1/gifs/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: GiphyApi by lazy {
        retrofit.create(GiphyApi::class.java)
    }
}