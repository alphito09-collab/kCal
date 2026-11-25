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
    version = 3,
    exportSchema = true,
)
abstract class KcalDatabase : RoomDatabase() {
    companion object {
        /** v2: remote food catalogue (localized names/portions, image URL, regions, active flag). */
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE food ADD COLUMN names TEXT NOT NULL DEFAULT '{}'")
                db.execSQL("ALTER TABLE food ADD COLUMN portions TEXT NOT NULL DEFAULT '{}'")
                db.execSQL("ALTER TABLE food ADD COLUMN imageUrl TEXT")
                db.execSQL("ALTER TABLE food ADD COLUMN regions TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE food ADD COLUMN active INTEGER NOT NULL DEFAULT 1")
            }
        }

        /**
         * v3: global catalogue (nutrition, glycaemic level, cost tier, ingredients), portion
         * servings per planned item, sugar reading context, and country-code regions.
         */
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE food ADD COLUMN cost INTEGER NOT NULL DEFAULT 2")
                db.execSQL("ALTER TABLE food ADD COLUMN emoji TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE food ADD COLUMN protein REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE food ADD COLUMN carbs REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE food ADD COLUMN fat REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE food ADD COLUMN fiber REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE food ADD COLUMN gi TEXT NOT NULL DEFAULT 'Medium'")
                db.execSQL("ALTER TABLE food ADD COLUMN ingredients TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE planned_item ADD COLUMN servings REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE sugar ADD COLUMN context TEXT NOT NULL DEFAULT 'Fasting'")
                // City-based regions ("Pakistan — Lahore") become ISO country codes.
                db.execSQL("UPDATE user_profile SET region = 'PK' WHERE length(region) != 2")
            }
        }
    }

    abstract fun userProfileDao(): UserProfileDao
    abstract fun foodDao(): FoodDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun waterDao(): WaterDao
    abstract fun weightDao(): WeightDao
    abstract fun stepsDao(): StepsDao
    abstract fun healthLogDao(): HealthLogDao
}
