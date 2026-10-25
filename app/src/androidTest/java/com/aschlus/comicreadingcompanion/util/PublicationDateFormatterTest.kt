
package com.aschlus.comicreadingcompanion.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class PublicationDateFormatterTest {

    @Test
    fun formatsYearMonthInputAsMonthAndYear() {
        val expected = expectedMonthAndYear("2026-10")

        assertEquals(
            expected,
            formatPublicationDate("2026-10")
        )
    }

    @Test
    fun formatsFullDateInputAsMonthAndYear() {
        val expected = expectedMonthAndYear("2026-10")

        assertEquals(
            expected,
            formatPublicationDate("2026-10-24")
        )
    }

    @Test
    fun formatsJanuaryCorrectly() {
        val expected = expectedMonthAndYear("2026-01")

        assertEquals(
            expected,
            formatPublicationDate("2026-01")
        )
    }

    @Test
    fun preservesOriginalStringWhenDateIsUnrecognized() {
        val invalidDate = "not-a-date"

        assertEquals(
            invalidDate,
            formatPublicationDate(invalidDate)
        )
    }

    private fun expectedMonthAndYear(
        yearMonth: String
    ): String {
        return YearMonth
            .parse(yearMonth)
            .format(
                DateTimeFormatter.ofPattern(
                    "MMM yyyy",
                    Locale.getDefault()
                )
            )
    }
}
