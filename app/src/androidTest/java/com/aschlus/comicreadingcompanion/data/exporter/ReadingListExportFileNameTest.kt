package com.aschlus.comicreadingcompanion.data.exporter

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReadingListExportFileNameTest {

    @Test
    fun readingListExportFileName_sanitizesTitle() {
        assertEquals(
            "ultimate_marvel_2000_2015.json",
            readingListExportFileName(
                "Ultimate Marvel (2000–2015)"
            )
        )

        assertEquals(
            "spider_man_volume_2.json",
            readingListExportFileName(
                "Spider-Man: Volume 2!"
            )
        )

        assertEquals(
            "reading_list.json",
            readingListExportFileName(
                "---"
            )
        )
    }
}