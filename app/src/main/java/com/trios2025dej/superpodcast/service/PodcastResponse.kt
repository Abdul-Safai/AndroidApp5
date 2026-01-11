package com.trios2025dej.superpodcast.service

import com.google.gson.annotations.SerializedName

data class PodcastResponse(
    @SerializedName("resultCount")
    val resultCount: Int = 0,

    @SerializedName("results")
    val results: List<ItunesPodcast> = emptyList()
) {
    data class ItunesPodcast(
        @SerializedName("collectionCensoredName")
        val collectionCensoredName: String? = null,

        @SerializedName("collectionName")
        val collectionName: String? = null,

        @SerializedName("artistName")
        val artistName: String? = null,

        @SerializedName("releaseDate")
        val releaseDate: String? = null,

        @SerializedName("feedUrl")
        val feedUrl: String? = null,

        // ✅ Artwork fields (so you never get “unresolved reference” again)
        @SerializedName("artworkUrl30")
        val artworkUrl30: String? = null,

        @SerializedName("artworkUrl60")
        val artworkUrl60: String? = null,

        @SerializedName("artworkUrl100")
        val artworkUrl100: String? = null,

        @SerializedName("artworkUrl600")
        val artworkUrl600: String? = null
    )
}
