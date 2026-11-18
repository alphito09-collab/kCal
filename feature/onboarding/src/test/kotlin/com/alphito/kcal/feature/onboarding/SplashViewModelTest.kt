package com.alphito.kcal.feature.onboarding

import com.alphito.kcal.core.data.repository.PreferencesRepository
import com.alphito.kcal.core.data.repository.UserRepository
import com.alphito.kcal.core.model.AppLanguage
import com.alphito.kcal.core.model.AppPreferences
import com.alphito.kcal.core.model.PremiumPlan
import com.alphito.kcal.core.model.UserProfile
import com.alphito.kcal.feature.onboarding.presentation.mvi.LaunchMode
import com.alphito.kcal.feature.onboarding.presentation.mvi.SplashViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class SplashViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `each tagline is shown exactly once, then the last one stays`() = runTest(dispatcher) {
        val vm = SplashViewModel(FakePreferences(), FakeUsers(), Random(7))
        val seen = mutableListOf(vm.state.value.taglineIndex)
        repeat(5) {
            advanceTimeBy(SplashViewModel.TAGLINE_INTERVAL_MS)
            runCurrent()
            val current = vm.state.value.taglineIndex
            if (current != seen.last()) seen += current
        }
        assertEquals(listOf(0, 1, 2), seen.sorted())
        advanceUntilIdle()
        assertEquals(seen.last(), vm.state.value.taglineIndex)
    }

    @Test
    fun `first session shows Get Started`() = runTest(dispatcher) {
        val vm = SplashViewModel(FakePreferences(), FakeUsers(), Random(1))
        advanceUntilIdle()
        assertEquals(LaunchMode.FirstLaunch, vm.state.value.mode)
        assertEquals(true, vm.state.value.showGetStarted)
    }

    @Test
    fun `returning user gets the progress splash`() = runTest(dispatcher) {
        val vm = SplashViewModel(FakePreferences(AppPreferences(introSeen = true)), FakeUsers(), Random(1))
        runCurrent()
        assertEquals(LaunchMode.Returning, vm.state.value.mode)
    }

    private class FakeUsers(private val stored: UserProfile? = null) : UserRepository {
        override val profile: Flow<UserProfile?> = flowOf(stored)
        override suspend fun current() = stored
        override suspend fun save(profile: UserProfile) = Unit
        override suspend fun update(transform: (UserProfile) -> UserProfile) = Unit
    }

    private class FakePreferences(private val prefs: AppPreferences = AppPreferences()) : PreferencesRepository {
        override val preferences: Flow<AppPreferences> = flowOf(prefs)
        override suspend fun snapshot() = prefs
        override suspend fun setOnboardingComplete(value: Boolean) = Unit
        override suspend fun setIntroSeen(value: Boolean) = Unit
        override suspend fun setHeightInFeet(value: Boolean) = Unit
        override suspend fun setWeightInLb(value: Boolean) = Unit
        override suspend fun setLanguage(language: AppLanguage) = Unit
        override suspend fun setActivityTracking(enabled: Boolean) = Unit
        override suspend fun setWaterGoal(ml: Int) = Unit
        override suspend fun markFreeTrialPromptSeen() = Unit
        override suspend fun setRating(stars: Int) = Unit
        override suspend fun setPlan(plan: PremiumPlan?) = Unit
    }
}
