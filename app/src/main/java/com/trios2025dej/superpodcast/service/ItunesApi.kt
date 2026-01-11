package com.trios2025dej.superpodcast.service

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ItunesApi {

    @GET("search")
    suspend fun searchPodcasts(
        @Query("term") term: String,
        @Query("media") media: String = "podcast"
    ): Response<PodcastResponse>
}
