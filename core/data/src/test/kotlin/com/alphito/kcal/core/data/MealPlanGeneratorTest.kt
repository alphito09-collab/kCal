package com.alphito.kcal.core.data

import com.alphito.kcal.core.data.catalog.FoodCatalog
import com.alphito.kcal.core.data.domain.MealPlanGenerator
import com.alphito.kcal.core.model.BudgetTier
import com.alphito.kcal.core.model.Gender
import com.alphito.kcal.core.model.HealthCondition
import com.alphito.kcal.core.model.MealType
import com.alphito.kcal.core.model.UserProfile
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MealPlanGeneratorTest {
    private val generator = MealPlanGenerator()
    private val profile = UserProfile("Ayesha", 24, Gender.Female, 165, 68.0)
    private val tuesday = LocalDate.of(2026, 11, 17)

    @Test fun `tuesday matches the reference plan`() {
        val items = generator.generate(profile, FoodCatalog.foods, tuesday, days = 1)
        fun ids(type: MealType) = items.filter { it.mealType == type }.map { it.foodId }
        assertEquals(listOf("anda_paratha", "lassi"), ids(MealType.Breakfast))
        assertEquals(listOf("daal_chawal", "salad"), ids(MealType.Lunch))
        assertEquals(listOf("fruit_chaat"), ids(MealType.Snack))
        assertEquals(listOf("grilled_chicken", "roti"), ids(MealType.Dinner))
    }

    @Test fun `lactose intolerance removes dairy`() {
        val items = generator.generate(profile.copy(conditions = persistentSetOf(HealthCondition.LactoseIntolerant)), FoodCatalog.foods, tuesday)
        assertFalse(items.any { it.foodId == "lassi" })
    }

    @Test fun `diabetes removes high sugar snacks but keeps a snack`() {
        val items = generator.generate(profile.copy(conditions = persistentSetOf(HealthCondition.Diabetes)), FoodCatalog.foods, tuesday)
        assertFalse(items.any { it.foodId == "fruit_chaat" })
        assertTrue(items.any { it.mealType == MealType.Snack })
    }

    @Test fun `low budget excludes expensive items`() {
        val items = generator.generate(profile.copy(budget = BudgetTier.Low), FoodCatalog.foods, tuesday, days = 7)
        val prices = FoodCatalog.foods.associate { it.id to it.pricePkr }
        assertTrue(items.all { prices.getValue(it.foodId) <= MealPlanGenerator.LOW_BUDGET_MAX_ITEM_PKR })
    }

    @Test fun `swap candidates rank closest calories first`() {
        val daal = FoodCatalog.foods.first { it.id == "daal_chawal" }
        val candidates = generator.swapCandidates(daal, MealType.Lunch, profile, FoodCatalog.foods)
        assertEquals("chickpea_curry_rice", candidates.first().food.id)
        assertTrue(candidates.first().bestMatch)
        assertFalse(candidates.any { it.food.id == "daal_chawal" })
    }
}
