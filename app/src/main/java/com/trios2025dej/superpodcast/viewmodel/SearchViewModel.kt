package com.trios2025dej.superpodcast.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.trios2025dej.superpodcast.repository.ItunesRepo

class SearchViewModel : ViewModel() {

    companion object {
        private const val TAG = "SearchVM"
        private const val MIN_WORDS_IN_TITLE = 3   // advanced/unusual criteria
    }

    // Safer than lateinit (prevents crashes if you forget to set it)
    var repo: ItunesRepo? = null

    data class PodcastItem(
        val title: String,
        val author: String,
        val feedUrl: String,
        val collectionViewUrl: String,
        val imageUrl: String
    )

    private fun wordCount(s: String): Int =
        s.trim().split(Regex("\\s+")).count { it.isNotBlank() }

    suspend fun search(term: String): List<PodcastItem> {
        val safe = term.trim()
        if (safe.isBlank()) return emptyList()

        val repoSafe = repo
        if (repoSafe == null) {
            Log.e(TAG, "❌ repo is NULL. Did you set vm.repo = ItunesRepo(...) ?")
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

            val body = response.body()
            val raw = body?.results.orEmpty()
            Log.i(TAG, "📦 resultCount=${body?.resultCount} size=${raw.size}")

            // Map -> PodcastItem
            val mapped = raw.map { p ->
                val title = p.collectionCensoredName
                    ?: p.collectionName
                    ?: "Podcast"

                val img = p.artworkUrl100
                    ?: p.artworkUrl60
                    ?: p.artworkUrl30
                    ?: p.artworkUrl600
                    ?: ""

                PodcastItem(
                    title = title,
                    author = p.artistName ?: "",
                    feedUrl = p.feedUrl ?: "",
                    collectionViewUrl = p.collectionViewUrl ?: "",
                    imageUrl = img
                )
            }

            // ✅ Advanced criteria: keep only titles with MIN_WORDS_IN_TITLE+ words
            val filtered = mapped.filter { wordCount(it.title) >= MIN_WORDS_IN_TITLE }

            Log.i(TAG, "🔎 after title-word filter (>= $MIN_WORDS_IN_TITLE): ${filtered.size}")

            filtered

        } catch (e: Exception) {
            Log.e(TAG, "💥 error: ${e.message}", e)
            emptyList()
        }
    }
}
