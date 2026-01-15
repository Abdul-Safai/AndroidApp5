package com.trios2025dej.superpodcast.repository

import com.trios2025dej.superpodcast.model.Episode
import com.trios2025dej.superpodcast.model.Podcast
import com.trios2025dej.superpodcast.service.RssFeedService
import com.trios2025dej.superpodcast.util.DateUtils

class PodcastRepo(private val rssService: RssFeedService) {

    suspend fun getPodcast(feedUrl: String, imageUrl: String = ""): Podcast? {
        val rss = rssService.getFeed(feedUrl) ?: return null

        val episodes: List<Episode> = rss.episodes.orEmpty().map { e ->
            Episode(
                guid = e.guid ?: "",
                title = e.title ?: "",
                description = e.description ?: "",
                mediaUrl = e.url ?: "",
                mimeType = e.type ?: "",
                releaseDate = DateUtils.xmlDateToDate(e.pubDate ?: "") ?: java.util.Date(),
                duration = e.duration ?: ""
            )
        }

        return Podcast(
            feedUrl = feedUrl,
            feedTitle = rss.title ?: "",
            feedDesc = rss.description ?: "",
            imageUrl = imageUrl,
            lastUpdated = rss.lastUpdated ?: java.util.Date(),
            episodes = episodes
        )
    }
}
