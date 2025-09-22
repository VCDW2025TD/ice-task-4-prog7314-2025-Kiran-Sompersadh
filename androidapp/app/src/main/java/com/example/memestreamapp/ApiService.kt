package com.example.memestreamapp

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiService {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // ✅ Custom OkHttpClient with longer timeout
    private val okHttpClient = OkHttpClient.Builder()
        .callTimeout(60, TimeUnit.SECONDS)   // total call timeout
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://meamstreamicetas1api.onrender.com/") // must end with /
        .client(okHttpClient) // attach custom client
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val memeApi: MemeApi by lazy {
        retrofit.create(MemeApi::class.java)
    }
}