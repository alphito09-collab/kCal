package com.alphito.kcal.core.common.time

import java.time.LocalDate
import java.time.ZoneId

/** Abstracted clock so date-dependent logic (streaks, weeks) is testable. */
fun interface AppClock {
    fun today(): LocalDate
}

object SystemAppClock : AppClock {
    override fun today(): LocalDate = LocalDate.now(ZoneId.systemDefault())
}
