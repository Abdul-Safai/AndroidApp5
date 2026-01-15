package com.trios2025dej.superpodcast.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.trios2025dej.superpodcast.adapter.EpisodeListAdapter
import com.trios2025dej.superpodcast.databinding.ActivityPodcastDetailBinding
import com.trios2025dej.superpodcast.repository.PodcastRepo
import com.trios2025dej.superpodcast.service.RssFeedService
import com.trios2025dej.superpodcast.util.SubscriptionStore
import kotlinx.coroutines.launch

class PodcastDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPodcastDetailBinding

    // Episodes
    private lateinit var episodeAdapter: EpisodeListAdapter

    // Audio
    private var player: MediaPlayer? = null
    private var nowPlayingUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPodcastDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val author = intent.getStringExtra(EXTRA_AUTHOR).orEmpty()
        val feedUrl = intent.getStringExtra(EXTRA_FEED_URL).orEmpty()
        val collectionUrl = intent.getStringExtra(EXTRA_COLLECTION_URL).orEmpty()

        binding.titleText.text = title
        binding.authorText.text = author.ifBlank { "Unknown author" }

        // ✅ Only open collection page (NOT raw RSS XML)
        val openUrl = collectionUrl.trim()

        // ✅ Subscription key (prefer feedUrl because stable)
        val subKey = (if (feedUrl.isNotBlank()) feedUrl else collectionUrl).trim()

        fun refreshSubscribeButton() {
            val subscribed = subKey.isNotBlank() && SubscriptionStore.isSubscribed(this, subKey)
            binding.subscribeBtn.text = if (subscribed) "Unsubscribe" else "Subscribe"
        }

        refreshSubscribeButton()

        // ---------------------------
        // Episodes RecyclerView setup
        // ---------------------------
        setupEpisodesRecycler()

        // ---------------------------
        // Load feed + episodes
        // ---------------------------
        if (feedUrl.isBlank()) {
            Toast.makeText(this, "No feed URL found for this podcast.", Toast.LENGTH_LONG).show()
        } else {
            loadPodcastFeed(feedUrl)
        }

        // ---------------------------
        // Buttons (your existing features)
        // ---------------------------
        binding.openBtn.setOnClickListener {
            if (openUrl.isBlank()) {
                Toast.makeText(this, "No webpage available for this podcast.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(openUrl)))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No app found to open links.", Toast.LENGTH_LONG).show()
            }
        }

        binding.subscribeBtn.setOnClickListener {
            if (subKey.isBlank()) {
                Toast.makeText(this, "Cannot subscribe: no podcast key.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val subscribed = SubscriptionStore.isSubscribed(this, subKey)
            if (subscribed) {
                SubscriptionStore.unsubscribe(this, subKey)
                Toast.makeText(this, "Unsubscribed", Toast.LENGTH_SHORT).show()
            } else {
                SubscriptionStore.subscribe(this, subKey)
                Toast.makeText(this, "Subscribed", Toast.LENGTH_SHORT).show()
            }
            refreshSubscribeButton()
        }

        binding.shareBtn.setOnClickListener {
            val shareText = when {
                openUrl.isNotBlank() -> "$title\n$openUrl"
                else -> title
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(shareIntent, "Share podcast"))
        }

        binding.backBtn.setOnClickListener { finish() }
    }

    // ---------------------------
    // Episodes UI
    // ---------------------------
    private fun setupEpisodesRecycler() {
        episodeAdapter = EpisodeListAdapter(emptyList()) { episode ->
            // ✅ Tap episode => play audio
            val audioUrl = episode.mediaUrl ?: ""   // change if your model uses another name
            playEpisode(audioUrl)
        }

        binding.episodeRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.episodeRecyclerView.adapter = episodeAdapter
        binding.episodeRecyclerView.setHasFixedSize(true)

        val divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        binding.episodeRecyclerView.addItemDecoration(divider)
    }

    private fun loadPodcastFeed(feedUrl: String) {
        // If you have progressBar in layout:
        binding.loadingText.visibility = View.VISIBLE


        lifecycleScope.launch {
            try {
                val repo = PodcastRepo(RssFeedService.instance)
                val podcast = repo.getPodcast(feedUrl)

                if (podcast == null) {
                    Toast.makeText(this@PodcastDetailActivity, "Could not load episodes.", Toast.LENGTH_LONG).show()
                } else {
                    // Show feed title/description if you want:
                    // binding.titleText.text = podcast.feedTitle ?: binding.titleText.text

                    val episodes = podcast.episodes ?: emptyList()
                    episodeAdapter.updateData(episodes)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@PodcastDetailActivity, "Error loading RSS feed.", Toast.LENGTH_LONG).show()
            } finally {
                binding.loadingText.visibility = View.GONE
            }
        }
    }

    // ---------------------------
    // Audio player (MediaPlayer streaming)
    // ---------------------------
    private fun playEpisode(url: String) {
        if (url.isBlank()) {
            Toast.makeText(this, "No audio URL for this episode.", Toast.LENGTH_SHORT).show()
            return
        }

        // same episode already playing
        if (nowPlayingUrl == url && player != null) {
            Toast.makeText(this, "Already playing.", Toast.LENGTH_SHORT).show()
            return
        }

        // stop old
        player?.stop()
        player?.release()
        player = null

        nowPlayingUrl = url

        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { it.start() }
                setOnErrorListener { _, _, _ ->
                    Toast.makeText(this@PodcastDetailActivity, "Audio error.", Toast.LENGTH_SHORT).show()
                    true
                }
                prepareAsync()
            }

            player = mp
            Toast.makeText(this, "Loading audio...", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Could not play this episode.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.stop()
        player?.release()
        player = null
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_AUTHOR = "extra_author"
        const val EXTRA_FEED_URL = "extra_feed_url"
        const val EXTRA_COLLECTION_URL = "extra_collection_url"
    }
}
