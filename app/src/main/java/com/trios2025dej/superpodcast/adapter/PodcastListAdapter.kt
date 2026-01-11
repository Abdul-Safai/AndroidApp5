package com.trios2025dej.superpodcast.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.trios2025dej.superpodcast.viewmodel.SearchViewModel

class PodcastListAdapter(
    private var items: List<SearchViewModel.PodcastItem>,
    private val onClick: (SearchViewModel.PodcastItem) -> Unit
) : RecyclerView.Adapter<PodcastListAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(android.R.id.text1)
        val subtitle: TextView = view.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.subtitle.text = item.author
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size

    fun update(data: List<SearchViewModel.PodcastItem>) {
        items = data
        notifyDataSetChanged()
    }
}
