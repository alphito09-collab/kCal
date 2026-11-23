package com.alphito.kcal.mealplanner.padometer

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.work.WorkerParameters
import com.alphito.kcal.mealplanner.padometer.di.kcalModules
import kotlinx.coroutines.CoroutineScope
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify

/** Fails the build if any constructor-injected definition in the graph cannot be resolved. */
@OptIn(KoinExperimentalAPI::class)
class KoinGraphTest {
    @Test
    fun `dependency graph is complete`() {
        module { includes(kcalModules) }.verify(
            extraTypes = listOf(
                Context::class, Application::class, SavedStateHandle::class, CoroutineScope::class,
                Boolean::class, Long::class,
                // Supplied by WorkManager at runtime (Koin workerOf).
                WorkerParameters::class,
            ),
        )
    }
}
