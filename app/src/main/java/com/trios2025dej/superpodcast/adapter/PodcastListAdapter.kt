package com.trios2025dej.superpodcast.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.trios2025dej.superpodcast.databinding.RowPodcastBinding
import com.trios2025dej.superpodcast.viewmodel.SearchViewModel

class PodcastListAdapter(
    private var items: List<SearchViewModel.PodcastSummaryViewData>,
    private val listener: PodcastListAdapterListener
) : RecyclerView.Adapter<PodcastListAdapter.VH>() {

    interface PodcastListAdapterListener {
        fun onShowDetails(item: SearchViewModel.PodcastSummaryViewData)
        fun onTogglePlay(item: SearchViewModel.PodcastSummaryViewData)
    }

    // Only 1 item can be loading/playing at a time
    private var loadingFeedUrl: String? = null
    private var playingFeedUrl: String? = null
    private var isPlaying: Boolean = false

    inner class VH(val binding: RowPodcastBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = RowPodcastBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val feedUrl = item.feedUrl?.trim().orEmpty()

        holder.binding.podcastTitleText.text = item.name ?: "Podcast"
        holder.binding.podcastAuthorText.text = item.author ?: ""

        Glide.with(holder.itemView)
            .load(item.imageUrl)
            .into(holder.binding.podcastImage)

        // Row tap -> details
        holder.itemView.setOnClickListener { listener.onShowDetails(item) }

        // Play button logic
        val isLoadingRow = (feedUrl.isNotBlank() && feedUrl == loadingFeedUrl)
        val isPlayingRow = (feedUrl.isNotBlank() && feedUrl == playingFeedUrl && isPlaying)

        holder.binding.playBtn.isEnabled = !isLoadingRow

        holder.binding.playBtn.setImageResource(
            when {
                isLoadingRow -> android.R.drawable.ic_popup_sync
                isPlayingRow -> android.R.drawable.ic_media_pause
                else -> android.R.drawable.ic_media_play
            }
        )

        holder.binding.playBtn.setOnClickListener {
            listener.onTogglePlay(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newList: List<SearchViewModel.PodcastSummaryViewData>) {
        items = newList
        notifyDataSetChanged()
    }

    // ✅ FIX: now accepts boolean (matches PodcastActivity calls)
    fun setLoadingState(feedUrl: String, isLoading: Boolean) {
        loadingFeedUrl = if (isLoading) feedUrl else null
        notifyDataSetChanged()
    }

    fun clearLoadingState() {
        loadingFeedUrl = null
        notifyDataSetChanged()
    }

    fun setPlayingState(feedUrl: String, playing: Boolean) {
        playingFeedUrl = feedUrl
        isPlaying = playing
        notifyDataSetChanged()
    }
}
