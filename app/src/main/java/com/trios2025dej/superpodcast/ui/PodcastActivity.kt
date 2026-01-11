package com.trios2025dej.superpodcast.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.trios2025dej.superpodcast.adapter.PodcastListAdapter
import com.trios2025dej.superpodcast.databinding.ActivityPodcastBinding
import com.trios2025dej.superpodcast.repository.ItunesRepo
import com.trios2025dej.superpodcast.service.ItunesService
import com.trios2025dej.superpodcast.viewmodel.SearchViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PodcastActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPodcastBinding
    private val vm by viewModels<SearchViewModel>()
    private lateinit var adapter: PodcastListAdapter

    private var searchJob: Job? = null

    companion object {
        private const val TAG = "SearchVM"
        private const val DEBOUNCE_MS = 500L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPodcastBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // repo
        vm.repo = ItunesRepo(ItunesService.api)

        // recycler
        adapter = PodcastListAdapter(emptyList()) { /* click */ }
        binding.podcastRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.podcastRecyclerView.adapter = adapter

        // ✅ Start with EMPTY list (no results on launch)
        clearResults()

        // Search view behavior
        binding.searchView.isIconified = false
        binding.searchView.isSubmitButtonEnabled = true

        // ✅ This makes the X button clear the text AND we clear the list too
        binding.searchView.setOnCloseListener {
            Log.i(TAG, "❎ onClose (X clicked)")
            clearResults()
            false // allow SearchView to also do its default close behavior
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                val term = query?.trim().orEmpty()
                Log.i(TAG, "✅ SUBMIT term='$term'")

                if (term.isBlank()) {
                    clearResults()
                    return true
                }

                performSearch(term)
                binding.searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val term = newText?.trim().orEmpty()

                // ✅ If user cleared text (or clicked X), clear results immediately
                if (term.isBlank()) {
                    searchJob?.cancel()
                    clearResults()
                    return true
                }

                // ✅ Debounce typing
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(DEBOUNCE_MS)
                    performSearch(term)
                }
                return true
            }
        })

        Log.i(TAG, "✅ PodcastActivity created (NO auto-search)")
    }

    private fun performSearch(term: String) {
        binding.progressBar.visibility = View.VISIBLE
        Log.i(TAG, "➡️ performSearch('$term')")

        lifecycleScope.launch {
            try {
                val results = vm.search(term)
                Log.i(TAG, "⬅️ results.size=${results.size}")
                adapter.update(results)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Search failed", e)
                clearResults()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun clearResults() {
        binding.progressBar.visibility = View.GONE
        adapter.update(emptyList())
        // Optional: scroll to top
        binding.podcastRecyclerView.scrollToPosition(0)
    }
}
