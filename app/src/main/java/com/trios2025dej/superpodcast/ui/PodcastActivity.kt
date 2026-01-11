package com.trios2025dej.superpodcast.ui

import android.content.Intent
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
        private const val TAG = "PodcastActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPodcastBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vm.repo = ItunesRepo(ItunesService.api)

        adapter = PodcastListAdapter(emptyList()) { item ->
            Log.i(TAG, "Clicked: ${item.title}")
            val i = Intent(this, PodcastDetailActivity::class.java).apply {
                putExtra(PodcastDetailActivity.EXTRA_TITLE, item.title)
                putExtra(PodcastDetailActivity.EXTRA_AUTHOR, item.author)
                putExtra(PodcastDetailActivity.EXTRA_FEED_URL, item.feedUrl)
                putExtra(PodcastDetailActivity.EXTRA_COLLECTION_URL, item.collectionViewUrl)
            }
            startActivity(i)
        }

        binding.podcastRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.podcastRecyclerView.adapter = adapter

        // If you have SearchView in XML:
        binding.searchView.isIconified = false
        binding.searchView.isSubmitButtonEnabled = true
        binding.searchView.queryHint = "Search podcasts (ted, bbc, news)"

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                val term = query?.trim().orEmpty()
                if (term.isNotBlank()) performSearch(term)
                binding.searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val term = newText?.trim().orEmpty()

                // Clear list when empty
                if (term.isBlank()) {
                    searchJob?.cancel()
                    adapter.update(emptyList())
                    binding.progressBar.visibility = View.GONE
                    return true
                }

                // debounce typing
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(500)
                    if (term.isNotBlank()) performSearch(term)
                }
                return true
            }
        })

        // ✅ Start with empty list (NO auto search)
        adapter.update(emptyList())
    }

    private fun performSearch(term: String) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val results = vm.search(term)
                adapter.update(results)
            } catch (e: Exception) {
                Log.e(TAG, "Search failed", e)
                adapter.update(emptyList())
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}
