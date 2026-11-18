package com.alphito.kcal.core.data.repository

import com.alphito.kcal.core.common.dispatcher.DispatcherProvider
import com.alphito.kcal.core.database.KcalDatabase
import com.alphito.kcal.core.datastore.PreferencesDataSource
import kotlinx.coroutines.withContext

/** Account-level operations that span every store (sign-out / delete wipes local data). */
interface AccountRepository {
    suspend fun deleteAllData()
}

internal class AccountRepositoryImpl(
    private val database: KcalDatabase,
    private val preferences: PreferencesDataSource,
    private val dispatchers: DispatcherProvider,
) : AccountRepository {
    override suspend fun deleteAllData(): Unit = withContext(dispatchers.io) {
        database.clearAllTables()
        preferences.clear()
        Unit
    }
}
