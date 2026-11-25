package com.alphito.kcal.core.data.repository

import android.content.Context
import android.util.Log
import com.alphito.kcal.core.common.dispatcher.DispatcherProvider
import com.alphito.kcal.core.data.catalog.CatalogJson
import com.alphito.kcal.core.data.mapper.toEntity
import com.alphito.kcal.core.database.dao.FoodDao
import com.alphito.kcal.core.datastore.PreferencesDataSource
import com.alphito.kcal.core.firebase.KcalFirebase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** Result of a catalogue sync, for logging / tests. */
sealed interface CatalogSyncResult {
    data object NotConfigured : CatalogSyncResult
    data object UpToDate : CatalogSyncResult
    data class Updated(val foods: Int, val version: Long) : CatalogSyncResult
    data class Failed(val error: Throwable) : CatalogSyncResult
}

interface CatalogRepository {
    /** Makes sure Room holds at least the bundled catalogue (first run, or after an app update). */
    suspend fun ensureBundled()

    /** [ensureBundled], then pulls the remote catalogue if its version is newer. */
    suspend fun sync(): CatalogSyncResult
}

/**
 * Two sources share one schema (backend/firebase/seed/build_catalog.py):
 *  - `assets/catalog.json`   bundled, so a fresh install plans meals offline
 *  - Firestore `meta/catalog {version}` + `foods/{id}` for updates without an app release
 * Whichever has the higher version wins. Room is replaced atomically; foods that disappear are
 * only *deactivated* because existing plans still reference them.
 */
internal class CatalogRepositoryImpl(
    private val context: Context,
    private val firebase: KcalFirebase,
    private val foodDao: FoodDao,
    private val preferences: PreferencesDataSource,
    private val dispatchers: DispatcherProvider,
) : CatalogRepository {

    private val mutex = Mutex()

    override suspend fun ensureBundled() = withContext(dispatchers.io) {
        mutex.withLock {
            val bundledVersion = context.assets.open(ASSET).bufferedReader().use { r ->
                CharArray(64).let { buf -> String(buf, 0, r.read(buf).coerceAtLeast(0)) }
            }.let(CatalogJson::peekVersion)
            if (foodDao.count() > 0 && bundledVersion <= preferences.catalogVersion()) return@withLock
            val catalog = context.assets.open(ASSET).bufferedReader().use { CatalogJson.parse(it.readText()) }
            if (catalog.foods.isEmpty()) return@withLock
            foodDao.replaceCatalog(catalog.foods.map { it.toEntity() })
            preferences.setCatalogVersion(catalog.version)
        }
    }

    override suspend fun sync(): CatalogSyncResult = withContext(dispatchers.io) {
        ensureBundled()
        if (firebase.uid() == null) return@withContext CatalogSyncResult.NotConfigured
        val db = firebase.firestore() ?: return@withContext CatalogSyncResult.NotConfigured
        runCatching {
            val remoteVersion = db.collection("meta").document("catalog").get().await().getLong("version") ?: 0L
            if (remoteVersion <= preferences.catalogVersion()) return@runCatching CatalogSyncResult.UpToDate
            val foods = db.collection("foods").get().await().documents.mapNotNull { doc ->
                doc.data?.let { CatalogJson.foodFromMap(doc.id, it) }
            }
            if (foods.isEmpty()) return@runCatching CatalogSyncResult.UpToDate // never wipe the plan source
            mutex.withLock {
                foodDao.replaceCatalog(foods.map { it.toEntity() })
                preferences.setCatalogVersion(remoteVersion)
            }
            CatalogSyncResult.Updated(foods.size, remoteVersion)
        }.getOrElse { error ->
            Log.w("CatalogSync", "Catalogue sync failed – keeping local catalogue", error)
            CatalogSyncResult.Failed(error)
        }
    }

    private companion object { const val ASSET = "catalog.json" }
}
