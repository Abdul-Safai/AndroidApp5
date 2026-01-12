package com.trios2025dej.superpodcast.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SubscriptionsAdapter(
    private var items: List<String>,
    private val onClick: (String) -> Unit,
    private val onLongClick: (String) -> Unit
) : RecyclerView.Adapter<SubscriptionsAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val t: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val url = items[position]
        holder.t.text = url

        holder.itemView.setOnClickListener { onClick(url) }
        holder.itemView.setOnLongClickListener {
            onLongClick(url)
            true
        }
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<String>) {
        items = newItems
        notifyDataSetChanged()
    }
}
