package com.trios2025dej.superpodcast.model

import java.util.Date

data class Podcast(
    val feedUrl: String = "",
    val feedTitle: String = "",
    val feedDesc: String = "",
    val imageUrl: String = "",
    val lastUpdated: Date = Date(),
    val episodes: List<Episode> = emptyList()
)
