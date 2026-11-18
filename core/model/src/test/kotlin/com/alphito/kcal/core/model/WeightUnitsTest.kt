package com.alphito.kcal.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WeightUnitsTest {
    @Test fun `68 kg shows as 149_9 lb`() = assertEquals(149.9, WeightUnits.display(68.0, inLb = true), 0.0)
    @Test fun `kg display is unchanged`() = assertEquals(68.4, WeightUnits.display(68.4, inLb = false), 0.0)
    @Test fun `150 lb is 68_04 kg`() = assertEquals(68.04, WeightUnits.toKg(150.0, inLb = true), 0.01)
    @Test fun `round trip through display stays within 0_1 lb`() {
        var kg = 30.0
        while (kg < 300.0) {
            val lb = WeightUnits.display(kg, true)
            assertEquals(kg, WeightUnits.toKg(lb, true), 0.05)
            kg += 0.7
        }
    }
}
