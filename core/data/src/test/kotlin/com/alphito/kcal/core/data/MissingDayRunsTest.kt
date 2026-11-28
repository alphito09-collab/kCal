package com.alphito.kcal.core.data

import com.alphito.kcal.core.data.repository.missingDayRuns
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Which days of the week get a generated plan: only those without a saved one. */
class MissingDayRunsTest {
    private val monday = LocalDate.of(2026, 11, 23)
    private val sunday = monday.plusDays(6)
    private fun days(vararg offsets: Int) = offsets.map { monday.plusDays(it.toLong()).toEpochDay() }.toSet()

    @Test fun `onboarding mid-week leaves the earlier days to fill`() {
        // regenerate(today = Wednesday) planned Wed–Sun only.
        assertEquals(listOf(monday to monday.plusDays(1)), missingDayRuns(monday, sunday, days(2, 3, 4, 5, 6)))
    }

    @Test fun `a fully planned week is left untouched`() {
        assertEquals(emptyList<Any>(), missingDayRuns(monday, sunday, days(0, 1, 2, 3, 4, 5, 6)))
    }

    @Test fun `an empty week is one run`() {
        assertEquals(listOf(monday to sunday), missingDayRuns(monday, sunday, emptySet()))
    }

    @Test fun `gaps between saved days are separate runs and saved days are skipped`() {
        assertEquals(
            listOf(monday to monday, monday.plusDays(2) to monday.plusDays(3), sunday to sunday),
            missingDayRuns(monday, sunday, days(1, 4, 5)),
        )
    }

    @Test fun `days outside the range are ignored`() {
        val outside = setOf(monday.minusDays(1).toEpochDay(), sunday.plusDays(1).toEpochDay())
        assertEquals(listOf(monday to sunday), missingDayRuns(monday, sunday, outside))
    }
}
