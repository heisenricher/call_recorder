package com.callrecorder.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeUtils {

    fun formatDuration(seconds: Long): String {
        val mins = seconds / 60
        val remainingSecs = seconds % 60
        val hours = mins / 60
        val remainingMins = mins % 60

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, remainingMins, remainingSecs)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", remainingMins, remainingSecs)
        }
    }

    fun formatTimestamp(epochMs: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - epochMs

        // If today, show time
        val date = Date(epochMs)
        val todayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val isToday = todayFormat.format(Date(now)) == todayFormat.format(date)

        return if (isToday) {
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            "Today, ${timeFormat.format(date)}"
        } else {
            val fullFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            fullFormat.format(date)
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1.0 -> String.format(Locale.getDefault(), "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.getDefault(), "%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}
