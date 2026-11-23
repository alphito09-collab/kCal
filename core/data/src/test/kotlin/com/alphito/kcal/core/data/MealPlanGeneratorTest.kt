package com.alphito.kcal.core.data

import com.alphito.kcal.core.data.catalog.CatalogJson
import com.alphito.kcal.core.data.domain.MealInsights
import com.alphito.kcal.core.data.domain.MealPlanGenerator
import com.alphito.kcal.core.model.BudgetTier
import com.alphito.kcal.core.model.EnergyCalculator
import com.alphito.kcal.core.model.FoodRole
import com.alphito.kcal.core.model.FoodTag
import com.alphito.kcal.core.model.Gender
import com.alphito.kcal.core.model.Glycemic
import com.alphito.kcal.core.model.HealthCondition
import com.alphito.kcal.core.model.MealReason
import com.alphito.kcal.core.model.MealType
import com.alphito.kcal.core.model.PlannedItem
import com.alphito.kcal.core.model.Regions
import com.alphito.kcal.core.model.UserProfile
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import kotlin.math.abs

/** Runs the generator against the real bundled catalogue (src/main/assets/catalog.json). */
class MealPlanGeneratorTest {
    private val catalog = CatalogJson.parse(File("src/main/assets/catalog.json").readText())
    private val foods = catalog.foods
    private val byId = foods.associateBy { it.id }
    private val generator = MealPlanGenerator()
    private val ayesha = UserProfile("Ayesha", 24, Gender.Female, 165, 68.0)
    private val monday = LocalDate.of(2026, 11, 16)
    private val conditionSets = listOf(
        emptySet(), setOf(HealthCondition.Diabetes), setOf(HealthCondition.Ibs), setOf(HealthCondition.LactoseIntolerant),
        HealthCondition.entries.toSet(),
    )

    private fun plan(profile: UserProfile, days: Int = 7) =
        generator.generate(profile, foods, monday, days, EnergyCalculator.dailyIntakeTarget(profile))

    @Test fun `catalogue parses with nutrition and translations`() {
        assertTrue(foods.size >= 150)
        assertTrue(catalog.version >= 2)
        foods.forEach { f ->
            assertEquals("${f.id} names", 12, f.names.size)
            assertTrue("${f.id} ingredients", f.ingredients.isNotEmpty())
            assertTrue("${f.id} kcal", f.kcal >= 0)
        }
    }

    @Test fun `every supported country gets four meals a day for every condition`() {
        Regions.supportedCountries.forEach { country ->
            conditionSets.forEach { conditions ->
                BudgetTier.entries.forEach { budget ->
                    val profile = ayesha.copy(region = country, conditions = conditions.toPersistentSet(), budget = budget)
                    val items = plan(profile)
                    (0 until 7).forEach { d ->
                        val date = monday.plusDays(d.toLong())
                        MealType.entries.forEach { meal ->
                            assertTrue("$country $conditions $budget $date $meal", items.any { it.date == date && it.mealType == meal })
                        }
                    }
                }
            }
        }
    }

    @Test fun `health conditions are never violated`() {
        listOf("PK", "IN", "SA", "EG", "TR", "DE", "RU", "CN", "ID", "MX", "US", "NG", "KE").forEach { country ->
            conditionSets.forEach { conditions ->
                val profile = ayesha.copy(region = country, conditions = conditions.toPersistentSet(), budget = BudgetTier.Low)
                plan(profile).forEach { item ->
                    val f = byId.getValue(item.foodId)
                    assertTrue("$country ${f.id} unsafe for $conditions", generator.isSafe(f, profile))
                    if (HealthCondition.Diabetes in conditions) {
                        assertFalse(FoodTag.HighSugar in f.tags); assertFalse(FoodTag.AddedSugar in f.tags)
                        assertFalse(f.glycemic == Glycemic.High)
                    }
                    if (HealthCondition.LactoseIntolerant in conditions) assertFalse(FoodTag.Dairy in f.tags)
                    if (HealthCondition.Ibs in conditions) assertFalse(FoodTag.HighFodmap in f.tags || FoodTag.Spicy in f.tags)
                }
            }
        }
    }

    @Test fun `days land close to the calorie target`() {
        listOf("PK", "DE", "NG", "CN", "MX", "US").forEach { country ->
            listOf(ayesha, ayesha.copy(gender = Gender.Male, weightKg = 90.0, heightCm = 180)).forEach { person ->
                val profile = person.copy(region = country)
                val target = EnergyCalculator.dailyIntakeTarget(profile)
                plan(profile).groupBy { it.date }.forEach { (date, items) ->
                    val kcal = items.sumOf { byId.getValue(it.foodId).kcal * it.servings }
                    assertTrue("$country $date $kcal vs $target", abs(kcal - target) <= target * 0.25)
                }
            }
        }
    }

    @Test fun `diabetes caps carbohydrate per meal`() {
        val profile = ayesha.copy(conditions = persistentSetOf(HealthCondition.Diabetes), gender = Gender.Male, weightKg = 95.0)
        plan(profile).groupBy { it.date to it.mealType }.forEach { (key, items) ->
            val carbs = items.sumOf { byId.getValue(it.foodId).carbsG * it.servings }
            val cap = if (key.second == MealType.Snack) MealPlanGenerator.SNACK_CARB_CAP_G else MealPlanGenerator.MEAL_CARB_CAP_G
            val atMinimum = items.all { it.servings <= MealPlanGenerator.MIN_SERVINGS || byId.getValue(it.foodId).role == FoodRole.Drink }
            assertTrue("$key carbs $carbs", carbs <= cap + 0.5 || atMinimum)
        }
    }

    @Test fun `low budget plans budget dishes for pakistan`() {
        plan(ayesha.copy(budget = BudgetTier.Low)).forEach { assertEquals(it.foodId, 1, byId.getValue(it.foodId).cost) }
    }

    @Test fun `plans prefer local dishes over other cuisines`() {
        listOf("PK", "NG", "MX", "CN", "TR").forEach { country ->
            val items = plan(ayesha.copy(region = country))
            assertTrue(country, items.all { Regions.isAvailable(byId.getValue(it.foodId), country) })
            assertTrue(country, items.count { Regions.isLocal(byId.getValue(it.foodId), country) } >= items.size / 3)
        }
    }

    @Test fun `mains do not repeat on consecutive days and lunch differs from dinner`() {
        val items = plan(ayesha, days = 14)
        val mains = items.filter { byId.getValue(it.foodId).role in setOf(FoodRole.Main, FoodRole.BreakfastMain) }
        mains.groupBy { it.foodId }.forEach { (id, uses) ->
            val days = uses.map { it.date.toEpochDay() }.distinct().sorted()
            days.zipWithNext().forEach { (a, b) -> assertTrue("$id repeated $a $b", b - a > 1) }
        }
        items.groupBy { it.date }.forEach { (_, day) ->
            val lunch = day.filter { it.mealType == MealType.Lunch }.map { it.foodId }.toSet()
            val dinner = day.filter { it.mealType == MealType.Dinner }.map { it.foodId }.toSet()
            assertTrue((lunch intersect dinner).isEmpty())
        }
    }

    @Test fun `regenerating the same week gives the same plan`() {
        assertEquals(plan(ayesha), plan(ayesha))
    }

    @Test fun `edge cases do not crash`() {
        assertTrue(generator.generate(ayesha, emptyList(), monday).isEmpty())
        assertTrue(generator.generate(ayesha, foods, monday, days = 0).isEmpty())
        assertTrue(generator.generate(ayesha, foods, monday, days = 1, dailyTargetKcal = 0).all { it.servings == 1.0 })
        assertTrue(plan(ayesha.copy(region = "ZZ")).isNotEmpty()) // unknown country → still planned
    }

    @Test fun `swap keeps calories and stays safe`() {
        val profile = ayesha.copy(conditions = persistentSetOf(HealthCondition.LactoseIntolerant))
        val daal = PlannedItem(1, monday, MealType.Lunch, 0, byId.getValue("daal_chawal"), servings = 1.25)
        val candidates = generator.swapCandidates(daal, profile, foods)
        assertTrue(candidates.size >= 3)
        assertTrue(candidates.first().bestMatch)
        assertFalse(candidates.any { it.food.id == "daal_chawal" })
        assertTrue(candidates.all { generator.isSafe(it.food, profile) })
        assertTrue(abs(candidates.first().kcal - daal.kcal) <= 60)
    }

    @Test fun `insights explain the meal for the user's condition and goal`() {
        val profile = ayesha.copy(conditions = persistentSetOf(HealthCondition.Diabetes))
        val item = PlannedItem(1, monday, MealType.Lunch, 0, byId.getValue("daal_chawal"))
        val reasons = MealInsights.reasons(item, profile)
        assertTrue(MealReason.GentleOnBloodSugar in reasons)
        assertTrue(reasons.any { it is MealReason.FitsTarget })
        assertTrue(MealReason.LocalDish in reasons)
        assertTrue(reasons.any { it is MealReason.HighFiber })
    }

    private fun Set<HealthCondition>.toPersistentSet() = persistentSetOf<HealthCondition>().addAll(this)
}
