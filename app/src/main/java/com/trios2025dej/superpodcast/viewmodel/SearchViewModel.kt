package com.trios2025dej.superpodcast.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.trios2025dej.superpodcast.repository.ItunesRepo

class SearchViewModel : ViewModel() {

    companion object {
        private const val TAG = "SearchVM"
        private const val MIN_WORDS_IN_TITLE = 3
    }

    // Set this in PodcastActivity:
    // searchViewModel.repo = ItunesRepo(ItunesService.instance)
    var repo: ItunesRepo? = null

    // ✅ Matches the teacher-style "summary" object the list screen uses
    data class PodcastSummaryViewData(
        var name: String = "",
        var author: String = "",
        var lastUpdated: String = "",
        var imageUrl: String = "",
        var feedUrl: String = "",
        var collectionViewUrl: String = ""
    )

    private fun wordCount(s: String): Int =
        s.trim().split(Regex("\\s+")).count { it.isNotBlank() }

    suspend fun search(term: String): List<PodcastSummaryViewData> {
        val safe = term.trim()
        if (safe.isBlank()) return emptyList()

        val repoSafe = repo
        if (repoSafe == null) {
            Log.e(TAG, "❌ repo is NULL. Did you set searchViewModel.repo = ItunesRepo(...) ?")
            return emptyList()
        }

        return try {
            Log.i(TAG, "🌐 searching term='$safe'")

            val response = repoSafe.search(safe)
            Log.i(TAG, "✅ code=${response.code()} success=${response.isSuccessful}")

            if (!response.isSuccessful) {
                Log.e(TAG, "❌ API failed: ${response.code()} ${response.message()}")
                return emptyList()
            }

            val raw = response.body()?.results.orEmpty()
            Log.i(TAG, "📦 results size=${raw.size}")

            val mapped = raw.map { p ->
                val title = p.collectionCensoredName
                    ?: p.collectionName
                    ?: "Podcast"

                val img = p.artworkUrl100
                    ?: p.artworkUrl60
                    ?: p.artworkUrl30
                    ?: p.artworkUrl600
                    ?: ""

                PodcastSummaryViewData(
                    name = title,
                    author = p.artistName ?: "",
                    lastUpdated = p.releaseDate ?: "",   // keep raw; you can format later if you want
                    imageUrl = img,
                    feedUrl = p.feedUrl ?: "",
                    collectionViewUrl = p.collectionViewUrl ?: ""
                )
            }

            // ✅ Your advanced criteria stays
            val filtered = mapped.filter { wordCount(it.name) >= MIN_WORDS_IN_TITLE }
            Log.i(TAG, "🔎 after title-word filter (>= $MIN_WORDS_IN_TITLE): ${filtered.size}")

            filtered
        } catch (e: Exception) {
            Log.e(TAG, "💥 error: ${e.message}", e)
            emptyList()
        }
    }
}
