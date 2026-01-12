package com.trios2025dej.superpodcast.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.trios2025dej.superpodcast.adapter.SubscriptionsAdapter
import com.trios2025dej.superpodcast.databinding.ActivitySubscriptionsBinding
import com.trios2025dej.superpodcast.util.SubscriptionStore

class SubscriptionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubscriptionsBinding
    private lateinit var adapter: SubscriptionsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySubscriptionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = SubscriptionsAdapter(
            items = emptyList(),
            onClick = { url -> openLink(url) },
            onLongClick = { url ->
                SubscriptionStore.unsubscribe(this, url)
                Toast.makeText(this, "Removed subscription", Toast.LENGTH_SHORT).show()
                loadSubs()
            }
        )

        binding.subRecycler.layoutManager = LinearLayoutManager(this)
        binding.subRecycler.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        loadSubs()
    }

    private fun loadSubs() {
        val subs = SubscriptionStore.getAll(this).toList().sorted()
        adapter.update(subs)
        binding.emptyText.visibility = if (subs.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        binding.subRecycler.visibility = if (subs.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun openLink(url: String) {
        val clean = url.trim()
        if (clean.isBlank()) {
            Toast.makeText(this, "No link to open.", Toast.LENGTH_LONG).show()
            return
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(clean))
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No app found to open this link.", Toast.LENGTH_LONG).show()
        }
    }
}
