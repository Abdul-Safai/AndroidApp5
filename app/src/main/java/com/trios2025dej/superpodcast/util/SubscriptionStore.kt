package com.trios2025dej.superpodcast.util

import android.content.Context

object SubscriptionStore {

    private const val PREFS = "superpodcast_prefs"
    private const val KEY_SUBS = "subs"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isSubscribed(context: Context, key: String): Boolean {
        val set = prefs(context).getStringSet(KEY_SUBS, emptySet()) ?: emptySet()
        return set.contains(key)
    }

    fun subscribe(context: Context, key: String) {
        val p = prefs(context)
        val set = (p.getStringSet(KEY_SUBS, emptySet()) ?: emptySet()).toMutableSet()
        set.add(key)
        p.edit().putStringSet(KEY_SUBS, set).apply()
    }

    fun unsubscribe(context: Context, key: String) {
        val p = prefs(context)
        val set = (p.getStringSet(KEY_SUBS, emptySet()) ?: emptySet()).toMutableSet()
        set.remove(key)
        p.edit().putStringSet(KEY_SUBS, set).apply()
    }
}
