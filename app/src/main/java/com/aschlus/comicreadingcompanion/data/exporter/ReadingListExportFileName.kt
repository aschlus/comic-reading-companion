package com.aschlus.comicreadingcompanion.data.exporter

fun readingListExportFileName(
    title: String
): String {
    val safeFileName =
        title
            .lowercase()
            .replace(
                Regex(
                    "[^a-z0-9]+"
                ),
                "_"
            )
            .trim('_')
            .ifBlank {
                "reading_list"
            }

    return "$safeFileName.json"
}