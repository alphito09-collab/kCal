package com.alphito.kcal.core.database.di

import androidx.room.Room
import com.alphito.kcal.core.database.KcalDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    // Opened lazily on first DAO use (Room opens the file on a background query thread),
    // so it never costs anything on the cold-start main thread.
    single {
        Room.databaseBuilder(androidContext(), KcalDatabase::class.java, "kcal.db")
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()
    }
    single { get<KcalDatabase>().userProfileDao() }
    single { get<KcalDatabase>().foodDao() }
    single { get<KcalDatabase>().mealPlanDao() }
    single { get<KcalDatabase>().waterDao() }
    single { get<KcalDatabase>().weightDao() }
    single { get<KcalDatabase>().stepsDao() }
    single { get<KcalDatabase>().healthLogDao() }
}
