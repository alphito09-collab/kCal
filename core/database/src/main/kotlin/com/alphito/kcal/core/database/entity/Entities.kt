package com.alphito.kcal.core.database.entity

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
data class SugarEntity(@PrimaryKey val epochDay: Long, val mgDl: Int)

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
    @androidx.room.Embedded(prefix = "food_") val food: FoodEntity,
)

data class DailyWater(val epochDay: Long, val totalMl: Int)
