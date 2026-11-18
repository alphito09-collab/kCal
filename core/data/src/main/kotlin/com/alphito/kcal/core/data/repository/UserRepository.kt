package com.alphito.kcal.core.data.repository

import com.alphito.kcal.core.common.dispatcher.DispatcherProvider
import com.alphito.kcal.core.data.mapper.toEntity
import com.alphito.kcal.core.data.mapper.toModel
import com.alphito.kcal.core.database.dao.UserProfileDao
import com.alphito.kcal.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface UserRepository {
    val profile: Flow<UserProfile?>
    suspend fun current(): UserProfile?
    suspend fun save(profile: UserProfile)
    suspend fun update(transform: (UserProfile) -> UserProfile)
}

internal class UserRepositoryImpl(
    private val dao: UserProfileDao,
    private val dispatchers: DispatcherProvider,
) : UserRepository {
    override val profile: Flow<UserProfile?> =
        dao.observe().map { it?.toModel() }.distinctUntilChanged().flowOn(dispatchers.io)

    override suspend fun current(): UserProfile? = withContext(dispatchers.io) { dao.get()?.toModel() }

    override suspend fun save(profile: UserProfile) = withContext(dispatchers.io) { dao.upsert(profile.toEntity()) }

    override suspend fun update(transform: (UserProfile) -> UserProfile) = withContext(dispatchers.io) {
        val current = dao.get()?.toModel() ?: return@withContext
        dao.upsert(transform(current).toEntity())
    }
}
