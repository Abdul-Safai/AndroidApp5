package com.trios2025dej.superpodcast.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.trios2025dej.superpodcast.repository.ItunesRepo

class SearchViewModel : ViewModel() {

    lateinit var repo: ItunesRepo

    data class PodcastItem(
        val title: String,
        val author: String,
        val openUrl: String // ✅ we will open iTunes page first (better than RSS XML)
    )

    suspend fun search(term: String): List<PodcastItem> {
        val safe = term.trim()
        if (safe.isBlank()) return emptyList()

        return try {
            Log.i("SearchVM", "🌐 searching term='$safe'")

            val response = repo.search(safe)

            Log.i("SearchVM", "✅ code=${response.code()} success=${response.isSuccessful}")

            if (!response.isSuccessful) return emptyList()

            val body = response.body()
            Log.i("SearchVM", "📦 resultCount=${body?.resultCount} size=${body?.results?.size}")

            body?.results?.map { p ->
                val title = p.collectionCensoredName ?: p.collectionName ?: "Podcast"
                val author = p.artistName ?: ""

                // ✅ Prefer normal web page. If missing, fallback to RSS feedUrl.
                val url = p.collectionViewUrl ?: p.feedUrl ?: ""

                PodcastItem(title = title, author = author, openUrl = url)
            } ?: emptyList()

        } catch (e: Exception) {
            Log.e("SearchVM", "💥 error: ${e.message}", e)
            emptyList()
        }
    }
}
