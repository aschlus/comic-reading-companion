package com.aschlus.comicreadingcompanion.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val publicationDateFormatter =
    DateTimeFormatter.ofPattern(
        "MMM yyyy",
        Locale.getDefault()
    )

fun formatPublicationDate(
    publicationDate: String
): String {
    return try {
        YearMonth
            .parse(publicationDate)
            .format(publicationDateFormatter)
    } catch (_: Exception) {
        try {
            YearMonth
                .from(LocalDate.parse(publicationDate))
                .format(publicationDateFormatter)
        } catch (_: Exception) {
            publicationDate
        }
    }
}