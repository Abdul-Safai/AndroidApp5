package com.trios2025dej.superpodcast.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {

    fun xmlDateToDate(dateString: String?): Date {
        val date = dateString ?: return Date()
        return try {
            val inFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.getDefault())
            inFormat.parse(date) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }
}
