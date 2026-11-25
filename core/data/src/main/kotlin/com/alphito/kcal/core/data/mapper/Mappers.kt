package com.alphito.kcal.core.data.mapper

import com.alphito.kcal.core.database.entity.FoodEntity
import com.alphito.kcal.core.database.entity.PlannedItemWithFood
import com.alphito.kcal.core.database.entity.UserProfileEntity
import com.alphito.kcal.core.model.BudgetTier
import com.alphito.kcal.core.model.Food
import com.alphito.kcal.core.model.FoodRole
import com.alphito.kcal.core.model.FoodTag
import com.alphito.kcal.core.model.Gender
import com.alphito.kcal.core.model.Goal
import com.alphito.kcal.core.model.HealthCondition
import com.alphito.kcal.core.model.MealType
import com.alphito.kcal.core.model.PlannedItem
import com.alphito.kcal.core.model.UserProfile
import com.alphito.kcal.core.model.Glycemic
import com.alphito.kcal.core.model.Regions
import com.alphito.kcal.core.data.catalog.CatalogJson
import kotlinx.collections.immutable.toImmutableSet
import java.time.LocalDate

internal inline fun <reified E : Enum<E>> String.toEnumSet(): Set<E> =
    split(',').filter { it.isNotBlank() }.mapNotNull { name -> enumValues<E>().firstOrNull { it.name == name } }.toSet()

internal fun Iterable<Enum<*>>.joinNames(): String = joinToString(",") { it.name }

internal inline fun <reified E : Enum<E>> String.toEnum(default: E): E =
    enumValues<E>().firstOrNull { it.name == this } ?: default

internal fun UserProfileEntity.toModel() = UserProfile(
    name = name, age = age, gender = gender.toEnum(Gender.Other), heightCm = heightCm, weightKg = weightKg,
    goal = goal.toEnum(Goal.LoseWeight), targetWeightKg = targetWeightKg, region = Regions.normalize(region),
    budget = budget.toEnum(BudgetTier.Medium), conditions = conditions.toEnumSet<HealthCondition>().toImmutableSet(),
    addStepsToDeficit = addStepsToDeficit, dailyDeficitKcal = dailyDeficitKcal, stepsDeficitKcal = stepsDeficitKcal,
)

internal fun UserProfile.toEntity() = UserProfileEntity(
    name = name.trim(), age = age, gender = gender.name, heightCm = heightCm, weightKg = weightKg, goal = goal.name,
    targetWeightKg = targetWeightKg, region = region, budget = budget.name, conditions = conditions.joinNames(),
    addStepsToDeficit = addStepsToDeficit, dailyDeficitKcal = dailyDeficitKcal, stepsDeficitKcal = stepsDeficitKcal,
)

internal fun FoodEntity.toModel() = Food(
    id = id, name = name, kcal = kcal, role = role.toEnum(FoodRole.Main),
    mealTypes = mealTypes.toEnumSet<MealType>().toImmutableSet(), imageKey = imageKey,
    tags = tags.toEnumSet<FoodTag>().toImmutableSet(),
    names = CatalogJson.parseStringMap(names), imageUrl = imageUrl,
    regions = regions.split(',').filter { it.isNotBlank() }.toImmutableSet(), active = active,
    cost = cost, emoji = emoji, proteinG = protein, carbsG = carbs, fatG = fat, fiberG = fiber,
    glycemic = gi.toEnum(Glycemic.Medium), ingredients = CatalogJson.parseIngredients(ingredients),
)

internal fun Food.toEntity() = FoodEntity(
    id = id, name = name, portion = "", kcal = kcal, pricePkr = 0, role = role.name,
    mealTypes = mealTypes.joinNames(), imageKey = imageKey, tags = tags.joinNames(),
    names = CatalogJson.stringMapToJson(names), portions = "{}", imageUrl = imageUrl,
    regions = regions.joinToString(","), active = active,
    cost = cost, emoji = emoji, protein = proteinG, carbs = carbsG, fat = fatG, fiber = fiberG,
    gi = glycemic.name, ingredients = CatalogJson.ingredientsToJson(ingredients),
)

internal fun PlannedItemWithFood.toModel() = PlannedItem(
    id = id, date = LocalDate.ofEpochDay(epochDay), mealType = mealType.toEnum(MealType.Lunch),
    position = position, food = food.toModel(), servings = servings,
)
