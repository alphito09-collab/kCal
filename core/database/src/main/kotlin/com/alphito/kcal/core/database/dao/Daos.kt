package com.alphito.kcal.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.alphito.kcal.core.database.entity.DailyWater
import com.alphito.kcal.core.database.entity.DairyLogEntity
import com.alphito.kcal.core.database.entity.FoodEntity
import com.alphito.kcal.core.database.entity.IbsLogEntity
import com.alphito.kcal.core.database.entity.MealCheckEntity
import com.alphito.kcal.core.database.entity.PlannedItemEntity
import com.alphito.kcal.core.database.entity.PlannedItemWithFood
import com.alphito.kcal.core.database.entity.StepsEntity
import com.alphito.kcal.core.database.entity.SugarEntity
import com.alphito.kcal.core.database.entity.UserProfileEntity
import com.alphito.kcal.core.database.entity.WaterLogEntity
import com.alphito.kcal.core.database.entity.WeightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observe(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun get(): UserProfileEntity?

    @Upsert
    suspend fun upsert(entity: UserProfileEntity)
}

@Dao
interface FoodDao {
    @Query("SELECT COUNT(*) FROM food")
    suspend fun count(): Int

    @Query("SELECT * FROM food")
    suspend fun all(): List<FoodEntity>

    @Query("SELECT * FROM food WHERE active = 1")
    suspend fun active(): List<FoodEntity>

    /** Foods eaten (checked meals) on the given days – used by health-log insights. */
    @Query(
        """SELECT DISTINCT f.* FROM food f
           INNER JOIN planned_item p ON p.foodId = f.id
           INNER JOIN meal_check c ON c.epochDay = p.epochDay AND c.mealType = p.mealType AND c.checked = 1
           WHERE p.epochDay IN (:days)""",
    )
    suspend fun eatenOn(days: List<Long>): List<FoodEntity>

    @Query("UPDATE food SET active = 0 WHERE id NOT IN (:keepIds)")
    suspend fun deactivateAllExcept(keepIds: List<String>)

    @Transaction
    suspend fun replaceCatalog(foods: List<FoodEntity>) {
        insertAll(foods)
        deactivateAllExcept(foods.map { it.id })
    }

    @Query("SELECT * FROM food WHERE id = :id")
    suspend fun byId(id: String): FoodEntity?

    /**
     * Upsert, never REPLACE: SQLite's REPLACE deletes the old row first, which violates the
     * planned_item → food foreign key as soon as a meal plan references the food.
     */
    @Upsert
    suspend fun insertAll(foods: List<FoodEntity>)
}

private const val ITEM_WITH_FOOD = """
    SELECT p.id, p.epochDay, p.mealType, p.position, p.servings,
           f.id AS food_id, f.name AS food_name, f.portion AS food_portion, f.kcal AS food_kcal,
           f.pricePkr AS food_pricePkr, f.role AS food_role, f.mealTypes AS food_mealTypes,
           f.imageKey AS food_imageKey, f.tags AS food_tags, f.names AS food_names,
           f.portions AS food_portions, f.imageUrl AS food_imageUrl, f.regions AS food_regions,
           f.active AS food_active, f.cost AS food_cost, f.emoji AS food_emoji, f.protein AS food_protein,
           f.carbs AS food_carbs, f.fat AS food_fat, f.fiber AS food_fiber, f.gi AS food_gi,
           f.ingredients AS food_ingredients
    FROM planned_item p INNER JOIN food f ON f.id = p.foodId
"""

@Dao
interface MealPlanDao {
    @Query("$ITEM_WITH_FOOD WHERE p.epochDay BETWEEN :from AND :to ORDER BY p.epochDay, p.position")
    fun observeRange(from: Long, to: Long): Flow<List<PlannedItemWithFood>>

    @Query("$ITEM_WITH_FOOD WHERE p.id = :id")
    fun observeItem(id: Long): Flow<PlannedItemWithFood?>

    @Query("$ITEM_WITH_FOOD WHERE p.id = :id")
    suspend fun getItem(id: Long): PlannedItemWithFood?

    @Query("SELECT * FROM meal_check WHERE epochDay BETWEEN :from AND :to")
    fun observeChecks(from: Long, to: Long): Flow<List<MealCheckEntity>>

    @Query("SELECT DISTINCT epochDay FROM meal_check WHERE checked = 1 ORDER BY epochDay DESC")
    fun observeCheckedDays(): Flow<List<Long>>

    @Upsert
    suspend fun upsertCheck(check: MealCheckEntity)

    @Query("UPDATE planned_item SET foodId = :foodId, servings = :servings WHERE id = :itemId")
    suspend fun updateFood(itemId: Long, foodId: String, servings: Double)

    @Query("DELETE FROM planned_item WHERE epochDay >= :from")
    suspend fun deleteFrom(from: Long)

    @Insert
    suspend fun insertItems(items: List<PlannedItemEntity>)

    @Transaction
    suspend fun replaceFrom(from: Long, items: List<PlannedItemEntity>) {
        deleteFrom(from)
        insertItems(items)
    }

    @Query("SELECT COUNT(*) FROM meal_check WHERE epochDay = :day AND checked = 1")
    suspend fun checkedCount(day: Long): Int

    @Query("SELECT MAX(epochDay) FROM planned_item")
    suspend fun lastPlannedDay(): Long?
}

@Dao
interface WaterDao {
    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM water_log WHERE epochDay = :day")
    fun observeTotal(day: Long): Flow<Int>

    @Query("SELECT epochDay, SUM(amountMl) AS totalMl FROM water_log WHERE epochDay BETWEEN :from AND :to GROUP BY epochDay")
    fun observeRange(from: Long, to: Long): Flow<List<DailyWater>>

    @Insert
    suspend fun insert(log: WaterLogEntity)
}

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight ORDER BY epochDay ASC")
    fun observeAll(): Flow<List<WeightEntity>>

    @Upsert
    suspend fun upsert(entity: WeightEntity)
}

@Dao
interface StepsDao {
    @Query("SELECT * FROM steps WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay")
    fun observeRange(from: Long, to: Long): Flow<List<StepsEntity>>

    @Query("SELECT steps FROM steps WHERE epochDay = :day")
    fun observeDay(day: Long): Flow<Int?>

    @Upsert
    suspend fun upsert(entity: StepsEntity)

    @Query("UPDATE steps SET steps = steps + :delta WHERE epochDay = :day")
    suspend fun increment(day: Long, delta: Int): Int

    @Transaction
    suspend fun add(day: Long, delta: Int) {
        if (increment(day, delta) == 0) upsert(StepsEntity(day, delta))
    }
}

@Dao
interface HealthLogDao {
    @Query("SELECT * FROM sugar WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay")
    fun observeSugar(from: Long, to: Long): Flow<List<SugarEntity>>

    @Upsert suspend fun upsertSugar(entity: SugarEntity)

    @Query("SELECT * FROM dairy_log ORDER BY epochDay DESC LIMIT :limit")
    fun observeDairy(limit: Int): Flow<List<DairyLogEntity>>

    @Upsert suspend fun upsertDairy(entity: DairyLogEntity)

    @Query("SELECT * FROM ibs_log WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay")
    fun observeIbs(from: Long, to: Long): Flow<List<IbsLogEntity>>

    @Upsert suspend fun upsertIbs(entity: IbsLogEntity)
}
