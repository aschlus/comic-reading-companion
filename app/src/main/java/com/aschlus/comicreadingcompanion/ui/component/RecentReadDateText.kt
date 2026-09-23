package com.aschlus.comicreadingcompanion.ui.component

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

internal fun recentReadText(
    completedAt: Long,
    nowMillis: Long = System.currentTimeMillis()
): String {
    val days =
        recentReadElapsedDays(
            completedAt = completedAt,
            nowMillis = nowMillis
        )

    return when {
        days == 0L ->
            "Read today"

        days == 1L ->
            "Read yesterday"

        days < 7L ->
            "Read $days days ago"

        days < 14L ->
            "Read 1 week ago"

        days < 28L ->
            "Read ${days / 7L} weeks ago"

        else ->
            "Read ${recentReadDate(completedAt)}"
    }
}

internal fun recentReadSectionLabel(
    completedAt: Long,
    nowMillis: Long = System.currentTimeMillis()
): String {
    val days =
        recentReadElapsedDays(
            completedAt = completedAt,
            nowMillis = nowMillis
        )

    return when {
        days == 0L ->
            "TODAY"

        days == 1L ->
            "YESTERDAY"

        days < 7L ->
            "$days DAYS AGO"

        days < 14L ->
            "ONE WEEK AGO"

        days < 28L ->
            "${days / 7L} WEEKS AGO"

        else ->
            recentReadDate(completedAt).uppercase(Locale.getDefault())
    }
}

internal fun recentReadFinishedText(
    completedAt: Long
): String =
    "Finished ${recentReadDate(completedAt)}"

private fun recentReadElapsedDays(
    completedAt: Long,
    nowMillis: Long
): Long {
    val elapsedMillis = (nowMillis - completedAt).coerceAtLeast(0L)

    return TimeUnit.MILLISECONDS.toDays(elapsedMillis)
}

private fun recentReadDate(
    completedAt: Long
): String {
    val formatter =
        SimpleDateFormat(
            "MMM d, yyyy",
            Locale.getDefault()
        )

    return formatter.format(Date(completedAt))
}