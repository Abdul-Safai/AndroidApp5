package com.trios2025dej.superpodcast.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
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
import kotlinx.coroutines.launch

class PodcastActivity : AppCompatActivity(),
    PodcastListAdapter.PodcastListAdapterListener {

    private lateinit var binding: ActivityPodcastBinding
    private lateinit var adapter: PodcastListAdapter

    private val searchViewModel by viewModels<SearchViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPodcastBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Your layout does NOT have a toolbar, so DON'T call setSupportActionBar()

        setupRecyclerView()
        setupSearchView()

        // ✅ Your ItunesService doesn't have create(), so use instance
        searchViewModel.repo = ItunesRepo(ItunesService.api)
        onBackPressedDispatcher.addCallback(this) {
            if (adapter.itemCount > 0) {
                clearSearchResults()
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    // ---------------------------
    // SEARCH VIEW (from layout)
    // ---------------------------
    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                val q = query?.trim().orEmpty()
                if (q.isNotBlank()) performSearch(q)
                binding.searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // optional: if user clears search, clear results too
                if (newText.isNullOrBlank()) {
                    clearSearchResults()
                }
                return false
            }
        })
    }

    // ---------------------------
    // RECYCLER VIEW
    // ---------------------------
    private fun setupRecyclerView() {
        adapter = PodcastListAdapter(emptyList(), this) // (items, listener)
        binding.podcastRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.podcastRecyclerView.adapter = adapter
    }

    // ---------------------------
    // SEARCH
    // ---------------------------
    private fun performSearch(term: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyText.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val results = searchViewModel.search(term)
                adapter.updateData(results)

                binding.emptyText.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                e.printStackTrace()
                showError("Unable to load search results. Please try again.")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    // ---------------------------
    // ADAPTER CLICK LISTENER
    // ---------------------------
    // ✅ This MUST match your adapter interface type
    override fun onShowDetails(podcast: SearchViewModel.PodcastSummaryViewData) {
        val url = podcast.collectionViewUrl ?: ""
        if (url.isNotBlank()) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } else {
            showError("No podcast link available.")
        }
    }

    // ---------------------------
    // HELPERS
    // ---------------------------
    private fun clearSearchResults() {
        adapter.updateData(emptyList())
        binding.progressBar.visibility = View.GONE
        binding.emptyText.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null) // ✅ avoids ok_button missing
            .create()
            .show()
    }
}
