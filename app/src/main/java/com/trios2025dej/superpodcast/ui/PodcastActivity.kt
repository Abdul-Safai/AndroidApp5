package com.trios2025dej.superpodcast.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPodcastBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vm.repo = ItunesRepo(ItunesService.api)

        adapter = PodcastListAdapter(emptyList()) { item ->
            openLink(item.openUrl, item.title)
        }

        binding.podcastRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.podcastRecyclerView.adapter = adapter

        // Start empty (no list until search)
        adapter.update(emptyList())
        binding.progressBar.visibility = View.GONE

        binding.searchView.isIconified = false
        binding.searchView.isSubmitButtonEnabled = true

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                val term = query?.trim().orEmpty()
                if (term.isNotBlank()) performSearch(term)
                binding.searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val term = newText?.trim().orEmpty()

                // ✅ This makes the X button work (clears list)
                if (term.isBlank()) {
                    searchJob?.cancel()
                    adapter.update(emptyList())
                    binding.progressBar.visibility = View.GONE
                    return true
                }

                // debounce search while typing
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(450)
                    if (term.isNotBlank()) performSearch(term)
                }
                return true
            }
        })
    }

    private fun performSearch(term: String) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val results = vm.search(term)
            adapter.update(results)
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun openLink(url: String, title: String) {
        val clean = url.trim()

        if (clean.isBlank()) {
            Toast.makeText(this, "No link found for: $title", Toast.LENGTH_LONG).show()
            return
        }

        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(clean)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No browser app found.", Toast.LENGTH_LONG).show()
        }
    }
}
BBC