package com.alphito.kcal.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Dates are stored as epoch days (Long) and enums/sets as names – no TypeConverters, cheap
 * indexed range queries, and the mapping to domain models lives in :core:data.
 */

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val name: String,
    val age: Int,
    val gender: String,
    val heightCm: Int,
    val weightKg: Double,
    val goal: String,
    val targetWeightKg: Double?,
    val region: String,
    val budget: String,
    val conditions: String,
    val addStepsToDeficit: Boolean,
    val dailyDeficitKcal: Int,
    val stepsDeficitKcal: Int,
) {
    companion object { const val SINGLETON_ID = 1 }
}

@Entity(tableName = "food")
data class FoodEntity(
    @PrimaryKey val id: String,
    val name: String,
    val portion: String,
    val kcal: Int,
    val pricePkr: Int,
    val role: String,
    val mealTypes: String,
    val imageKey: String,
    val tags: String,
    /** JSON object {lang: name}. */
    @ColumnInfo(defaultValue = "{}") val names: String = "{}",
    @ColumnInfo(defaultValue = "{}") val portions: String = "{}",
    val imageUrl: String? = null,
    /** CSV of region keys; empty = everywhere. */
    @ColumnInfo(defaultValue = "") val regions: String = "",
    @ColumnInfo(defaultValue = "1") val active: Boolean = true,
    // v3 – nutrition & explanation data derived from ingredients by the catalogue build.
    @ColumnInfo(defaultValue = "2") val cost: Int = 2,
    @ColumnInfo(defaultValue = "") val emoji: String = "",
    @ColumnInfo(defaultValue = "0") val protein: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val carbs: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val fat: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val fiber: Double = 0.0,
    @ColumnInfo(defaultValue = "Medium") val gi: String = "Medium",
    /** JSON array [{id, grams, category, names{lang:name}}]. */
    @ColumnInfo(defaultValue = "[]") val ingredients: String = "[]",
)

@Entity(
    tableName = "planned_item",
    indices = [Index("epochDay"), Index("foodId")],
    foreignKeys = [ForeignKey(entity = FoodEntity::class, parentColumns = ["id"], childColumns = ["foodId"], onDelete = ForeignKey.RESTRICT)],
)
data class PlannedItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val mealType: String,
    val position: Int,
    val foodId: String,
    /** Portion multiplier so the day lands on the calorie target. */
    @ColumnInfo(defaultValue = "1.0") val servings: Double = 1.0,
)

@Entity(tableName = "meal_check", primaryKeys = ["epochDay", "mealType"])
data class MealCheckEntity(
    val epochDay: Long,
    val mealType: String,
    val checked: Boolean,
)

@Entity(tableName = "water_log", indices = [Index("epochDay")])
data class WaterLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val amountMl: Int,
    val loggedAt: Long,
)

@Entity(tableName = "weight")
data class WeightEntity(@PrimaryKey val epochDay: Long, val weightKg: Double)

@Entity(tableName = "steps")
data class StepsEntity(@PrimaryKey val epochDay: Long, val steps: Int)

@Entity(tableName = "sugar")
data class SugarEntity(
    @PrimaryKey val epochDay: Long,
    val mgDl: Int,
    /** Fasting / BeforeMeal / AfterMeal / Bedtime – ranges differ by context. */
    @ColumnInfo(defaultValue = "Fasting") val context: String = "Fasting",
)

@Entity(tableName = "dairy_log")
data class DairyLogEntity(
    @PrimaryKey val epochDay: Long,
    val hadDairy: Boolean,
    val reactions: String,
    val note: String,
)

@Entity(tableName = "ibs_log")
data class IbsLogEntity(
    @PrimaryKey val epochDay: Long,
    val symptoms: String,
    val severity: String,
)

/** Row type for joined planned items. */
data class PlannedItemWithFood(
    val id: Long,
    val epochDay: Long,
    val mealType: String,
    val position: Int,
    val servings: Double,
    @androidx.room.Embedded(prefix = "food_") val food: FoodEntity,
)

data class DailyWater(val epochDay: Long, val totalMl: Int)
