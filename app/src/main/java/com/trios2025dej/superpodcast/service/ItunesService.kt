package com.trios2025dej.superpodcast.service

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ItunesService {

    private const val BASE_URL = "https://itunes.apple.com/"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder().build()
    }

    val api: ItunesApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ItunesApi::class.java)
    }
}
