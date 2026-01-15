package com.trios2025dej.superpodcast.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.trios2025dej.superpodcast.R
import com.trios2025dej.superpodcast.model.Episode
import java.text.SimpleDateFormat
import java.util.Locale

class EpisodeListAdapter(
    private var episodes: List<Episode>,
    private val onEpisodeClick: (Episode) -> Unit
) : RecyclerView.Adapter<EpisodeListAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.CANADA)

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        val descTextView: TextView = itemView.findViewById(R.id.descTextView)
        val durationTextView: TextView = itemView.findViewById(R.id.durationTextView)
        val releaseDateTextView: TextView = itemView.findViewById(R.id.releaseDateTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_episode, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ep = episodes[position]

        holder.titleTextView.text = ep.title.ifBlank { "Episode" }
        holder.descTextView.text = ep.description
        holder.durationTextView.text = "Duration: ${ep.duration.ifBlank { "N/A" }}"

        // ✅ FIX: Date -> String
        holder.releaseDateTextView.text = dateFormat.format(ep.releaseDate)

        holder.itemView.setOnClickListener { onEpisodeClick(ep) }
    }

    override fun getItemCount(): Int = episodes.size

    fun updateData(newEpisodes: List<Episode>) {
        episodes = newEpisodes
        notifyDataSetChanged()
    }
}
