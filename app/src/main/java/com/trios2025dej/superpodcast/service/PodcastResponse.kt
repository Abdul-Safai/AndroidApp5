package com.trios2025dej.superpodcast.service

import com.google.gson.annotations.SerializedName

data class PodcastResponse(
    @SerializedName("resultCount")
    val resultCount: Int = 0,

    @SerializedName("results")
    val results: List<ItunesPodcast> = emptyList()
)

data class ItunesPodcast(
    @SerializedName("collectionCensoredName")
    val collectionCensoredName: String? = null,

    @SerializedName("collectionName")
    val collectionName: String? = null,

    @SerializedName("artistName")
    val artistName: String? = null,

    @SerializedName("feedUrl")
    val feedUrl: String? = null,

    // ✅ this opens a normal iTunes web page (better than RSS XML)
    @SerializedName("collectionViewUrl")
    val collectionViewUrl: String? = null
)
