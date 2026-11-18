package com.alphito.kcal.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.alphito.kcal.core.database.dao.FoodDao
import com.alphito.kcal.core.database.dao.HealthLogDao
import com.alphito.kcal.core.database.dao.MealPlanDao
import com.alphito.kcal.core.database.dao.StepsDao
import com.alphito.kcal.core.database.dao.UserProfileDao
import com.alphito.kcal.core.database.dao.WaterDao
import com.alphito.kcal.core.database.dao.WeightDao
import com.alphito.kcal.core.database.entity.DairyLogEntity
import com.alphito.kcal.core.database.entity.FoodEntity
import com.alphito.kcal.core.database.entity.IbsLogEntity
import com.alphito.kcal.core.database.entity.MealCheckEntity
import com.alphito.kcal.core.database.entity.PlannedItemEntity
import com.alphito.kcal.core.database.entity.StepsEntity
import com.alphito.kcal.core.database.entity.SugarEntity
import com.alphito.kcal.core.database.entity.UserProfileEntity
import com.alphito.kcal.core.database.entity.WaterLogEntity
import com.alphito.kcal.core.database.entity.WeightEntity

@Database(
    entities = [
        UserProfileEntity::class, FoodEntity::class, PlannedItemEntity::class, MealCheckEntity::class,
        WaterLogEntity::class, WeightEntity::class, StepsEntity::class, SugarEntity::class,
        DairyLogEntity::class, IbsLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class KcalDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun foodDao(): FoodDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun waterDao(): WaterDao
    abstract fun weightDao(): WeightDao
    abstract fun stepsDao(): StepsDao
    abstract fun healthLogDao(): HealthLogDao
}
