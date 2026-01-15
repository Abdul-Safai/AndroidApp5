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

    // Which podcast is currently playing (key = feedUrl)
    private var playingFeedUrl: String? = null
    private var isPlaying: Boolean = false

    // Optional: show loading state while fetching RSS/audio url
    private var loadingFeedUrl: String? = null

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

        holder.binding.podcastTitleText.text = item.name ?: "Podcast"
        holder.binding.podcastAuthorText.text = item.author ?: ""

        Glide.with(holder.itemView)
            .load(item.imageUrl)
            .into(holder.binding.podcastImage)

        // Row click => details screen
        holder.itemView.setOnClickListener {
            listener.onShowDetails(item)
        }

        // Play button click => play/pause preview
        holder.binding.playBtn.setOnClickListener {
            // show loading state immediately (PodcastActivity will call setPlayingState after)
            val feedUrl = item.feedUrl?.trim().orEmpty()
            if (feedUrl.isNotBlank()) {
                loadingFeedUrl = feedUrl
                notifyDataSetChanged()
            }
            listener.onTogglePlay(item)
        }

        // ----------------------------
        // Icon state for this row
        // ----------------------------
        val feedUrl = item.feedUrl?.trim().orEmpty()

        when {
            feedUrl.isNotBlank() && feedUrl == loadingFeedUrl -> {
                // loading icon
                holder.binding.playBtn.setImageResource(android.R.drawable.ic_popup_sync)
                holder.binding.playBtn.isEnabled = false
            }

            feedUrl.isNotBlank() && feedUrl == playingFeedUrl && isPlaying -> {
                // pause icon
                holder.binding.playBtn.setImageResource(android.R.drawable.ic_media_pause)
                holder.binding.playBtn.isEnabled = true
            }

            else -> {
                // play icon
                holder.binding.playBtn.setImageResource(android.R.drawable.ic_media_play)
                holder.binding.playBtn.isEnabled = true
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newList: List<SearchViewModel.PodcastSummaryViewData>) {
        items = newList
        // reset playing/loading when results change
        playingFeedUrl = null
        isPlaying = false
        loadingFeedUrl = null
        notifyDataSetChanged()
    }

    /**
     * Called by PodcastActivity when playback changes.
     * - If isPlaying=true => show pause icon on that row
     * - If isPlaying=false => show play icon
     */
    fun setPlayingState(feedUrl: String, isPlayingNow: Boolean) {
        playingFeedUrl = feedUrl
        isPlaying = isPlayingNow
        loadingFeedUrl = null // stop loading icon
        notifyDataSetChanged()
    }

    /**
     * If PodcastActivity hits an error, call this to clear loading state.
     */
    fun clearLoadingState() {
        loadingFeedUrl = null
        notifyDataSetChanged()
    }
}
