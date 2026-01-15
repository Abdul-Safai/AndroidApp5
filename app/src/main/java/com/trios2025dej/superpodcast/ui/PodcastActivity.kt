package com.trios2025dej.superpodcast.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.trios2025dej.superpodcast.adapter.PodcastListAdapter
import com.trios2025dej.superpodcast.databinding.ActivityPodcastBinding
import com.trios2025dej.superpodcast.repository.ItunesRepo
import com.trios2025dej.superpodcast.repository.PodcastRepo
import com.trios2025dej.superpodcast.service.ItunesService
import com.trios2025dej.superpodcast.service.RssFeedService
import com.trios2025dej.superpodcast.viewmodel.SearchViewModel
import kotlinx.coroutines.launch

class PodcastActivity : AppCompatActivity(),
    PodcastListAdapter.PodcastListAdapterListener {

    private lateinit var binding: ActivityPodcastBinding
    private lateinit var adapter: PodcastListAdapter
    private val searchViewModel by viewModels<SearchViewModel>()

    // ✅ PODPLAY (ExoPlayer)
    private var player: ExoPlayer? = null

    // Which feed is currently selected for preview
    private var playingFeedUrl: String? = null

    // Prevent multiple rapid taps from loading multiple feeds
    private var isLoadingPreview: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPodcastBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearchView()

        searchViewModel.repo = ItunesRepo(ItunesService.api)

        setupPlayer()

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
    // SEARCH VIEW
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
                if (newText.isNullOrBlank()) clearSearchResults()
                return false
            }
        })
    }

    // ---------------------------
    // RECYCLER VIEW
    // ---------------------------
    private fun setupRecyclerView() {
        adapter = PodcastListAdapter(emptyList(), this)
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
    // ROW CLICK => DETAILS PAGE
    // ---------------------------
    override fun onShowDetails(item: SearchViewModel.PodcastSummaryViewData) {
        val i = Intent(this, PodcastDetailActivity::class.java).apply {
            putExtra(PodcastDetailActivity.EXTRA_TITLE, item.name ?: "Podcast")
            putExtra(PodcastDetailActivity.EXTRA_AUTHOR, item.author ?: "")
            putExtra(PodcastDetailActivity.EXTRA_FEED_URL, item.feedUrl ?: "")
            putExtra(PodcastDetailActivity.EXTRA_COLLECTION_URL, item.collectionViewUrl ?: "")
        }
        startActivity(i)
    }

    // ---------------------------
    // PLAY BUTTON CLICK => PODPLAY
    // ---------------------------
    override fun onTogglePlay(item: SearchViewModel.PodcastSummaryViewData) {
        val feedUrl = item.feedUrl?.trim().orEmpty()
        if (feedUrl.isBlank()) {
            Toast.makeText(this, "No RSS feed URL for this podcast.", Toast.LENGTH_LONG).show()
            adapter.clearLoadingState()
            return
        }

        val p = player
        if (playingFeedUrl == feedUrl && p != null) {
            // same podcast => toggle pause/play
            togglePlayPause()
            return
        }

        if (isLoadingPreview) {
            // avoid double tap spam
            Toast.makeText(this, "Please wait…", Toast.LENGTH_SHORT).show()
            return
        }

        // starting a new preview
        isLoadingPreview = true
        playingFeedUrl = feedUrl

        // show loading icon first
        // (adapter internally shows loading if you call it before loading finishes)
        // We call setPlayingState after it starts or fails.
        loadFirstEpisodeAndPlay(feedUrl)
    }

    private fun loadFirstEpisodeAndPlay(feedUrl: String) {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@PodcastActivity, "Loading preview…", Toast.LENGTH_SHORT).show()

                val repo = PodcastRepo(RssFeedService.instance)
                val podcast = repo.getPodcast(feedUrl)
                val episodes = podcast?.episodes.orEmpty()

                // Pick first episode that has a real audio url
                val firstPlayable = episodes.firstOrNull { it.mediaUrl.trim().isNotBlank() }
                var audioUrl = firstPlayable?.mediaUrl?.trim().orEmpty()

                // If some feeds return http, try https (many devices block cleartext)
                if (audioUrl.startsWith("http://")) {
                    audioUrl = audioUrl.replaceFirst("http://", "https://")
                }

                if (audioUrl.isBlank()) {
                    adapter.clearLoadingState()
                    adapter.setPlayingState(feedUrl, false)
                    Toast.makeText(this@PodcastActivity, "No playable episode found.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                startStreaming(audioUrl)

            } catch (e: Exception) {
                e.printStackTrace()
                adapter.clearLoadingState()
                adapter.setPlayingState(feedUrl, false)
                Toast.makeText(this@PodcastActivity, "Error loading RSS feed.", Toast.LENGTH_LONG).show()
            } finally {
                isLoadingPreview = false
            }
        }
    }

    // ---------------------------
    // EXOPLAYER
    // ---------------------------

    private fun setupPlayer() {

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("SuperPodcast/1.0 (Android)")
            .setAllowCrossProtocolRedirects(true) // important for some feeds

        val dataSourceFactory = DefaultDataSource.Factory(
            this,
            httpFactory
        )

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .apply {

                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        val key = playingFeedUrl ?: return
                        adapter.clearLoadingState()
                        adapter.setPlayingState(key, isPlaying)
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        val key = playingFeedUrl
                        if (key != null) {
                            adapter.clearLoadingState()
                            adapter.setPlayingState(key, false)
                        }
                        Toast.makeText(
                            this@PodcastActivity,
                            "Audio error: ${error.errorCodeName}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
            }
    }
    private fun startStreaming(url: String) {
        val p = player ?: return
        val key = playingFeedUrl ?: return

        adapter.clearLoadingState()
        adapter.setPlayingState(key, false) // will flip to pause when actually starts

        p.stop()
        p.clearMediaItems()

        val item = MediaItem.fromUri(url)
        p.setMediaItem(item)
        p.prepare()

        // Important: set to true before play() so it starts as soon as it’s ready
        p.playWhenReady = true
        p.play()

        Toast.makeText(this, "Starting preview…", Toast.LENGTH_SHORT).show()
    }

    private fun togglePlayPause() {
        val p = player ?: return
        val key = playingFeedUrl ?: return

        if (p.isPlaying) {
            p.pause()
            adapter.setPlayingState(key, false)
        } else {
            p.play()
            adapter.setPlayingState(key, true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }

    // ---------------------------
    // HELPERS
    // ---------------------------
    private fun clearSearchResults() {
        adapter.updateData(emptyList())
        binding.progressBar.visibility = View.GONE
        binding.emptyText.visibility = View.VISIBLE

        // stop preview
        playingFeedUrl = null
        isLoadingPreview = false
        player?.stop()
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .create()
            .show()
    }
}
