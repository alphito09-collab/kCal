package com.alphito.kcal.core.model

/** Meal-plan streak rules, kept pure so they're unit-testable. Days are epoch days. */
object Streak {
    /**
     * Days on which every planned meal is checked. [planned] and [checked] are (epochDay, mealType)
     * pairs; a day with nothing planned is never complete.
     */
    fun completedDays(planned: Collection<Pair<Long, String>>, checked: Collection<Pair<Long, String>>): Set<Long> {
        val done = checked.toHashSet()
        return planned.groupBy({ it.first }, { it.second })
            .filter { (day, meals) -> meals.all { (day to it) in done } }
            .keys
    }

    /**
     * Consecutive *completed* days (every planned meal checked) ending today if today is complete,
     * otherwise ending yesterday, so an unfinished today doesn't break the streak yet. Days before
     * [startDay] (the onboarding day) never count; without one, nothing counts.
     */
    fun count(completedDays: Collection<Long>, today: Long, startDay: Long?): Int {
        if (startDay == null) return 0
        val days = completedDays.filterTo(HashSet()) { it in startDay..today }
        var cursor = if (today in days) today else today - 1
        var streak = 0
        while (cursor in days) { streak++; cursor-- }
        return streak
    }
}
