package com.trios2025dej.superpodcast.ui

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.recyclerview.widget.LinearLayoutManager
import com.trios2025dej.superpodcast.R
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

    // PODPLAY (ExoPlayer)
    private var player: ExoPlayer? = null
    private var playingFeedUrl: String? = null
    private var isLoadingPreview: Boolean = false

    // Optional mic button (only works if it exists in your XML)
    private val micBtn by lazy { binding.root.findViewById<ImageView?>(R.id.micBtn) }

    private val voiceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull().orEmpty()
            if (spoken.isNotBlank()) {
                binding.searchView.setQuery(spoken, true) // submit
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPodcastBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearchView()

        searchViewModel.repo = ItunesRepo(ItunesService.api)

        setupPlayer()
        styleSearchBar()

        micBtn?.setOnClickListener { startVoiceSearch() }

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

        // ✅ keep it expanded + keep search icon visible
        binding.searchView.setIconifiedByDefault(false)
        binding.searchView.isIconified = false

        // ✅ tap anywhere on the search bar => focus typing
        binding.searchView.setOnClickListener {
            binding.searchView.isIconified = false
            binding.searchView.requestFocus()
        }

        // ✅ if user taps the text area, keep it expanded
        binding.searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.searchView.isIconified = false
        }

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
            togglePlayPause()
            return
        }

        if (isLoadingPreview) {
            Toast.makeText(this, "Please wait…", Toast.LENGTH_SHORT).show()
            return
        }

        isLoadingPreview = true
        playingFeedUrl = feedUrl

        adapter.setLoadingState(feedUrl, true)
        loadFirstEpisodeAndPlay(feedUrl)
    }

    private fun loadFirstEpisodeAndPlay(feedUrl: String) {
        lifecycleScope.launch {
            try {
                val repo = PodcastRepo(RssFeedService.instance)
                val podcast = repo.getPodcast(feedUrl)
                val episodes = podcast?.episodes.orEmpty()

                val firstPlayable = episodes.firstOrNull { it.mediaUrl.trim().isNotBlank() }
                var audioUrl = firstPlayable?.mediaUrl?.trim().orEmpty()

                // Try https if feed returns http
                if (audioUrl.startsWith("http://")) {
                    audioUrl = audioUrl.replaceFirst("http://", "https://")
                }

                if (audioUrl.isBlank()) {
                    adapter.setLoadingState(feedUrl, false)
                    adapter.setPlayingState(feedUrl, false)
                    Toast.makeText(this@PodcastActivity, "No playable episode found.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                startStreaming(audioUrl)

            } catch (e: Exception) {
                e.printStackTrace()
                adapter.setLoadingState(feedUrl, false)
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
            .setAllowCrossProtocolRedirects(true)

        val dataSourceFactory = DefaultDataSource.Factory(this, httpFactory)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .apply {
                addListener(object : Player.Listener {

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        val key = playingFeedUrl ?: return
                        adapter.setLoadingState(key, false)
                        adapter.setPlayingState(key, isPlaying)
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        val key = playingFeedUrl
                        if (key != null) {
                            adapter.setLoadingState(key, false)
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

        adapter.setLoadingState(key, false)
        adapter.setPlayingState(key, false)

        p.stop()
        p.clearMediaItems()

        p.setMediaItem(MediaItem.fromUri(url))
        p.prepare()
        p.playWhenReady = true
        p.play()
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

    private fun clearSearchResults() {
        adapter.updateData(emptyList())
        binding.progressBar.visibility = View.GONE
        binding.emptyText.visibility = View.VISIBLE

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

    // ---------------------------
    // STYLE SEARCH BAR (icon visible)
    // ---------------------------
    private fun styleSearchBar() {
        val searchText = binding.searchView.findViewById<TextView>(androidx.appcompat.R.id.search_src_text)
        searchText.setTextColor(Color.parseColor("#1F2330"))
        searchText.setHintTextColor(Color.parseColor("#7A8194"))
        searchText.textSize = 15f

        val searchIcon = binding.searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)
        searchIcon.visibility = View.VISIBLE
        searchIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#2D6BFF"))

        val closeIcon = binding.searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        closeIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#7A8194"))

        micBtn?.imageTintList = ColorStateList.valueOf(Color.parseColor("#2D6BFF"))
    }

    private fun startVoiceSearch() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a podcast name…")
            }
            voiceLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Voice search not supported on this device.", Toast.LENGTH_LONG).show()
        }
    }
}
