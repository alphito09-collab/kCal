package com.alphito.kcal.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class HeightUnitsTest {
    @Test fun `165 cm is 5 ft 5 in`() = assertEquals(FeetInches(5, 5), HeightUnits.toFeetInches(165))
    @Test fun `5 ft 5 in is 165 cm`() = assertEquals(165, HeightUnits.toCm(5, 5))
    @Test fun `round trip stays within a centimetre`() {
        for (cm in 120..230) {
            val fi = HeightUnits.toFeetInches(cm)
            assert(kotlin.math.abs(HeightUnits.toCm(fi.feet, fi.inches) - cm) <= 1)
        }
    }
}
