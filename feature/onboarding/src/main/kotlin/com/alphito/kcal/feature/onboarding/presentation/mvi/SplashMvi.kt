package com.alphito.kcal.feature.onboarding.presentation.mvi

import com.alphito.kcal.core.common.mvi.MviViewModel
import com.alphito.kcal.core.common.mvi.UiEffect
import com.alphito.kcal.core.common.mvi.UiIntent
import com.alphito.kcal.core.common.mvi.UiState
import com.alphito.kcal.core.data.repository.PreferencesRepository
import com.alphito.kcal.core.data.repository.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class LaunchMode {
    /** Deciding (a few ms, while prefs/profile load off the main thread). */
    Loading,
    /** Very first session: rotating taglines + "Get Started". */
    FirstLaunch,
    /** Every later launch: rotating taglines + 4 s progress, then continue automatically. */
    Returning,
}

data class SplashState(
    val mode: LaunchMode = LaunchMode.Loading,
    val taglineIndex: Int = 0,
    val showGetStarted: Boolean = false,
) : UiState

sealed interface SplashIntent : UiIntent { data object GetStarted : SplashIntent }

sealed interface SplashEffect : UiEffect {
    data object OpenHome : SplashEffect
    data object OpenAboutYou : SplashEffect
    data object OpenPremiumIntro : SplashEffect
}

/**
 * Launch routing:
 *  - profile saved ("Tell us about you" done)        → Dashboard
 *  - intro seen but profile missing                  → Tell us about you
 *  - first session                                   → Get Started → Premium → Language → …
 */
class SplashViewModel(
    private val preferences: PreferencesRepository,
    private val users: UserRepository,
    private val random: Random = Random.Default,
) : MviViewModel<SplashState, SplashIntent, SplashEffect>(SplashState()) {

    /** The three taglines in a random order; each is shown once, then the last one stays. */
    private val taglineOrder: List<Int> = (0 until TAGLINE_COUNT).shuffled(random)

    init {
        reduce { copy(taglineIndex = taglineOrder.first()) }
        launchSafe {
            val hasProfile = async { users.current() != null }
            val introSeen = async { preferences.snapshot().introSeen }
            val profileDone = hasProfile.await()
            val returning = profileDone || introSeen.await()
            reduce { copy(mode = if (returning) LaunchMode.Returning else LaunchMode.FirstLaunch) }
            if (returning) {
                delay(RETURNING_SPLASH_MS)
                sendEffect(if (profileDone) SplashEffect.OpenHome else SplashEffect.OpenAboutYou)
            } else {
                delay(GET_STARTED_DELAY_MS)
                reduce { copy(showGetStarted = true) }
            }
        }
        launchSafe { rotateTaglines() }
    }

    /** Steps through the shuffled taglines once, one per second, and stops on the last. */
    private suspend fun rotateTaglines() {
        for (index in taglineOrder.drop(1)) {
            delay(TAGLINE_INTERVAL_MS)
            reduce { copy(taglineIndex = index) }
        }
    }

    override fun onIntent(intent: SplashIntent) {
        when (intent) {
            SplashIntent.GetStarted -> sendEffect(SplashEffect.OpenPremiumIntro)
        }
    }

    override fun onError(throwable: Throwable) {
        super.onError(throwable)
        reduce { copy(mode = LaunchMode.FirstLaunch, showGetStarted = true) }
    }

    companion object {
        const val TAGLINE_COUNT = 3
        const val TAGLINE_INTERVAL_MS = 1_000L
        const val RETURNING_SPLASH_MS = 4_000L
        const val GET_STARTED_DELAY_MS = 1_400L
    }
}
