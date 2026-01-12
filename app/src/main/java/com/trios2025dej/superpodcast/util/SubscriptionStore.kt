package com.trios2025dej.superpodcast.util

import android.content.Context

object SubscriptionStore {

    private const val PREFS = "superpodcast_prefs"
    private const val KEY_SUBS = "subs"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isSubscribed(context: Context, key: String): Boolean {
        val set = prefs(context).getStringSet(KEY_SUBS, emptySet())?.toSet() ?: emptySet()
        return set.contains(key.trim())
    }

    fun subscribe(context: Context, key: String) {
        val cleanKey = key.trim()
        if (cleanKey.isBlank()) return

        val p = prefs(context)
        val current = p.getStringSet(KEY_SUBS, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(cleanKey)

        p.edit().putStringSet(KEY_SUBS, current).apply()
    }

    fun unsubscribe(context: Context, key: String) {
        val cleanKey = key.trim()
        if (cleanKey.isBlank()) return

        val p = prefs(context)
        val current = p.getStringSet(KEY_SUBS, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.remove(cleanKey)

        p.edit().putStringSet(KEY_SUBS, current).apply()
    }

    // ✅ Optional helper (useful later when you want a "Subscriptions" screen)
    fun getAll(context: Context): Set<String> {
        return prefs(context).getStringSet(KEY_SUBS, emptySet())?.toSet() ?: emptySet()
    }
}
