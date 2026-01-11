package com.trios2025dej.superpodcast.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.trios2025dej.superpodcast.repository.ItunesRepo

class SearchViewModel : ViewModel() {

    lateinit var repo: ItunesRepo

    data class PodcastItem(
        val title: String,
        val author: String,
        val feedUrl: String,
        val collectionViewUrl: String
    )

    private fun wordCount(s: String): Int =
        s.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size

    suspend fun search(term: String): List<PodcastItem> {
        val safe = term.trim()
        if (safe.isBlank()) return emptyList()

        return try {
            Log.i("SearchVM", "🌐 searching term='$safe'")

            val response = repo.search(safe)
            Log.i("SearchVM", "✅ code=${response.code()} success=${response.isSuccessful}")

            if (!response.isSuccessful) return emptyList()

            val body = response.body()
            val raw = body?.results ?: emptyList()
            Log.i("SearchVM", "📦 resultCount=${body?.resultCount} size=${raw.size}")

            // ✅ Map
            val mapped = raw.map { p ->
                PodcastItem(
                    title = p.collectionCensoredName ?: p.collectionName ?: "Podcast",
                    author = p.artistName ?: "",
                    feedUrl = p.feedUrl ?: "",
                    collectionViewUrl = p.collectionViewUrl ?: ""
                )
            }

            // ✅ ADVANCED / UNUSUAL CRITERIA:
            // Filter results to podcasts whose TITLE has at least 3 words.
            val minWords = 3
            val filtered = mapped.filter { wordCount(it.title) >= minWords }

            Log.i("SearchVM", "🔎 after wordCount filter (>= $minWords): ${filtered.size}")

            filtered

        } catch (e: Exception) {
            Log.e("SearchVM", "💥 error: ${e.message}", e)
            emptyList()
        }
    }
}
