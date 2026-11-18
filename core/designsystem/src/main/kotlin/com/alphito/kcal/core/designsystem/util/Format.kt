package com.alphito.kcal.core.designsystem.util

import java.text.NumberFormat
import java.util.Locale

/** Locale-aware grouping ("1,130"). Formatters are cached per locale – they are costly to build. */
object KcalFormat {
    private val intFormats = HashMap<Locale, NumberFormat>()
    private val decimalFormats = HashMap<Locale, NumberFormat>()

    fun int(value: Int, locale: Locale = Locale.getDefault()): String =
        synchronized(intFormats) { intFormats.getOrPut(locale) { NumberFormat.getIntegerInstance(locale) } }.format(value)

    fun decimal1(value: Double, locale: Locale = Locale.getDefault()): String =
        synchronized(decimalFormats) {
            decimalFormats.getOrPut(locale) {
                NumberFormat.getNumberInstance(locale).apply { minimumFractionDigits = 1; maximumFractionDigits = 1 }
            }
        }.format(value)

    /** "−0.6" with a real minus sign, "+0.4" otherwise. */
    fun signedDecimal1(value: Double): String =
        (if (value < 0) "−" else if (value > 0) "+" else "") + decimal1(kotlin.math.abs(value))
}
