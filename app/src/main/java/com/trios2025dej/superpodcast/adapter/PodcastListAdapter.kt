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
    }

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

        holder.itemView.setOnClickListener { listener.onShowDetails(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newList: List<SearchViewModel.PodcastSummaryViewData>) {
        items = newList
        notifyDataSetChanged()
    }
}
