package com.trios2025dej.superpodcast.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.trios2025dej.superpodcast.databinding.ActivityPodcastDetailBinding
import com.trios2025dej.superpodcast.util.SubscriptionStore

class PodcastDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPodcastDetailBinding

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

        // Use feedUrl if available, otherwise fallback to collectionViewUrl
        val openUrl = (if (feedUrl.isNotBlank()) feedUrl else collectionUrl).trim()

        // Use a stable key for subscribe (prefer feedUrl, else collectionUrl)
        val subKey = (if (feedUrl.isNotBlank()) feedUrl else collectionUrl).trim()

        fun refreshSubscribeButton() {
            val subscribed = subKey.isNotBlank() && SubscriptionStore.isSubscribed(this, subKey)
            binding.subscribeBtn.text = if (subscribed) "Unsubscribe" else "Subscribe"
        }

        refreshSubscribeButton()

        binding.openBtn.setOnClickListener {
            if (openUrl.isBlank()) {
                Toast.makeText(this, "No link available for this podcast.", Toast.LENGTH_LONG).show()
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
                Toast.makeText(this, "Cannot subscribe: no podcast link key.", Toast.LENGTH_LONG).show()
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

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_AUTHOR = "extra_author"
        const val EXTRA_FEED_URL = "extra_feed_url"
        const val EXTRA_COLLECTION_URL = "extra_collection_url"
    }
}
