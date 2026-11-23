package com.alphito.kcal.feature.settings.presentation.mvi

import androidx.lifecycle.viewModelScope
import com.alphito.kcal.core.common.flags.FeatureFlags
import com.alphito.kcal.core.common.mvi.MviViewModel
import com.alphito.kcal.core.common.mvi.UiEffect
import com.alphito.kcal.core.common.mvi.UiIntent
import com.alphito.kcal.core.common.mvi.UiState
import com.alphito.kcal.core.data.repository.AccountRepository
import com.alphito.kcal.core.data.repository.PreferencesRepository
import com.alphito.kcal.core.data.repository.UserRepository
import com.alphito.kcal.core.model.AppLanguage
import com.alphito.kcal.core.model.PremiumPlan
import com.alphito.kcal.core.navigation.Route
import com.alphito.kcal.feature.settings.domain.DataExporter
import com.alphito.kcal.feature.settings.domain.PasswordResult
import com.alphito.kcal.feature.settings.domain.PasswordRules
import com.alphito.kcal.feature.settings.domain.PasswordService
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

enum class SettingsSheet { None, Help, Payments, Rate }

// ------------------------------------------------------------------ Settings tab

data class SettingsState(
    val name: String = "",
    val initials: String = "",
    val plan: PremiumPlan? = null,
    val rating: Int = 0,
    val sheet: SettingsSheet = SettingsSheet.None,
    val premiumEnabled: Boolean = false,
) : UiState

sealed interface SettingsIntent : UiIntent {
    data class Open(val route: Route) : SettingsIntent
    data class Show(val sheet: SettingsSheet) : SettingsIntent
    data object Dismiss : SettingsIntent
    data class Rate(val stars: Int) : SettingsIntent
    data object EmailSupport : SettingsIntent
}

sealed interface SettingsEffect : UiEffect {
    data class Navigate(val route: Route) : SettingsEffect
    data object OpenEmail : SettingsEffect
    data object OpenStoreListing : SettingsEffect
}

class SettingsViewModel(
    users: UserRepository,
    private val preferences: PreferencesRepository,
    featureFlags: FeatureFlags,
) : MviViewModel<SettingsState, SettingsIntent, SettingsEffect>(SettingsState()) {

    init {
        combine(users.profile, preferences.preferences, featureFlags.flags) { profile, prefs, flags ->
            reduce {
                copy(
                    name = profile?.name.orEmpty(), initials = profile?.initials.orEmpty(), plan = prefs.subscription.activePlan,
                    rating = prefs.lastRating, premiumEnabled = flags.premiumEnabled,
                )
            }
        }.catch { onError(it) }.launchIn(viewModelScope)
    }

    override fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.Open -> sendEffect(SettingsEffect.Navigate(intent.route))
            is SettingsIntent.Show -> reduce { copy(sheet = intent.sheet) }
            SettingsIntent.Dismiss -> reduce { copy(sheet = SettingsSheet.None) }
            is SettingsIntent.Rate -> {
                reduce { copy(rating = intent.stars) }
                launchSafe { preferences.setRating(intent.stars) }
                // Happy users go to the store listing; everyone else is simply thanked.
                if (intent.stars >= 4) sendEffect(SettingsEffect.OpenStoreListing)
            }
            SettingsIntent.EmailSupport -> sendEffect(SettingsEffect.OpenEmail)
        }
    }
}

// ------------------------------------------------------------------ Language

data class LanguageState(
    val selected: AppLanguage = AppLanguage.English,
    val saved: AppLanguage = AppLanguage.English,
    val onboarding: Boolean = false,
) : UiState

sealed interface LanguageIntent : UiIntent {
    data class Select(val language: AppLanguage) : LanguageIntent
    data object Save : LanguageIntent
    data object Back : LanguageIntent
}

sealed interface LanguageEffect : UiEffect {
    data class Apply(val language: AppLanguage) : LanguageEffect
    data object Close : LanguageEffect
    data object ContinueOnboarding : LanguageEffect
}

class LanguageViewModel(
    private val onboarding: Boolean,
    private val preferences: PreferencesRepository,
) : MviViewModel<LanguageState, LanguageIntent, LanguageEffect>(LanguageState(onboarding = onboarding)) {

    init {
        launchSafe {
            val lang = preferences.snapshot().language
            reduce { copy(selected = lang, saved = lang) }
        }
    }

    override fun onIntent(intent: LanguageIntent) {
        when (intent) {
            is LanguageIntent.Select -> reduce { copy(selected = intent.language) }
            LanguageIntent.Back -> sendEffect(LanguageEffect.Close)
            LanguageIntent.Save -> launchSafe {
                val lang = currentState.selected
                preferences.setLanguage(lang)
                if (onboarding) {
                    // Premium + Language done: later launches go straight to "Tell us about you".
                    preferences.setIntroSeen(true)
                    sendEffect(LanguageEffect.ContinueOnboarding)
                } else sendEffect(LanguageEffect.Close)
                // Applied in place (no activity recreation): the UI re-renders in the new language
                // on the screen we return to – Settings, or the intro in the first session.
                if (lang != currentState.saved) sendEffect(LanguageEffect.Apply(lang))
            }
        }
    }
}

// ------------------------------------------------------------------ Privacy & security

enum class PrivacyDialog { None, ChangePassword, Download, Delete }

data class PrivacyState(
    val activityTracking: Boolean = false,
    val dialog: PrivacyDialog = PrivacyDialog.None,
    val newPassword: String = "",
    val confirmPassword: String = "",
    val passwordError: Int? = null,
    val busy: Boolean = false,
) : UiState {
    val canSavePassword: Boolean get() = PasswordRules.isStrong(newPassword) && newPassword == confirmPassword
}

sealed interface PrivacyIntent : UiIntent {
    data class ActivityToggled(val enabled: Boolean) : PrivacyIntent
    data class ActivityPermissionResult(val granted: Boolean) : PrivacyIntent
    data class Show(val dialog: PrivacyDialog) : PrivacyIntent
    data object Dismiss : PrivacyIntent
    data class NewPassword(val value: String) : PrivacyIntent
    data class ConfirmPassword(val value: String) : PrivacyIntent
    data object SavePassword : PrivacyIntent
    data object Download : PrivacyIntent
    data object Delete : PrivacyIntent
    data class OpenLink(val url: String) : PrivacyIntent
    data object Back : PrivacyIntent
}

sealed interface PrivacyEffect : UiEffect {
    data object RequestActivityPermission : PrivacyEffect
    data class ShareExport(val json: String) : PrivacyEffect
    data class OpenUrl(val url: String) : PrivacyEffect
    data class Message(val res: Int) : PrivacyEffect
    data object Deleted : PrivacyEffect
    data object Close : PrivacyEffect
}

class PrivacyViewModel(
    private val preferences: PreferencesRepository,
    private val passwords: PasswordService,
    private val exporter: DataExporter,
    private val account: AccountRepository,
) : MviViewModel<PrivacyState, PrivacyIntent, PrivacyEffect>(PrivacyState()) {

    init {
        preferences.preferences.onEach { reduce { copy(activityTracking = it.activityTrackingEnabled) } }
            .catch { onError(it) }.launchIn(viewModelScope)
    }

    override fun onIntent(intent: PrivacyIntent) {
        when (intent) {
            is PrivacyIntent.ActivityToggled ->
                if (intent.enabled) sendEffect(PrivacyEffect.RequestActivityPermission)
                else launchSafe { preferences.setActivityTracking(false) }
            is PrivacyIntent.ActivityPermissionResult -> launchSafe { preferences.setActivityTracking(intent.granted) }
            is PrivacyIntent.Show -> reduce { copy(dialog = intent.dialog, newPassword = "", confirmPassword = "", passwordError = null) }
            PrivacyIntent.Dismiss -> reduce { copy(dialog = PrivacyDialog.None, busy = false) }
            is PrivacyIntent.NewPassword -> reduce { copy(newPassword = intent.value, passwordError = null) }
            is PrivacyIntent.ConfirmPassword -> reduce { copy(confirmPassword = intent.value, passwordError = null) }
            PrivacyIntent.SavePassword -> savePassword()
            PrivacyIntent.Download -> launchSafe {
                reduce { copy(busy = true) }
                val json = exporter.exportJson()
                reduce { copy(busy = false, dialog = PrivacyDialog.None) }
                sendEffect(PrivacyEffect.ShareExport(json))
            }
            PrivacyIntent.Delete -> launchSafe {
                reduce { copy(busy = true) }
                account.deleteAllData()
                sendEffect(PrivacyEffect.Deleted)
            }
            is PrivacyIntent.OpenLink ->
                if (intent.url.isBlank()) sendEffect(PrivacyEffect.Message(com.alphito.kcal.feature.settings.R.string.privacy_link_unavailable))
                else sendEffect(PrivacyEffect.OpenUrl(intent.url))
            PrivacyIntent.Back -> sendEffect(PrivacyEffect.Close)
        }
    }

    private fun savePassword() {
        val s = currentState
        when {
            s.newPassword != s.confirmPassword -> reduce { copy(passwordError = com.alphito.kcal.feature.settings.R.string.password_mismatch) }
            !PasswordRules.isStrong(s.newPassword) -> reduce { copy(passwordError = com.alphito.kcal.feature.settings.R.string.password_rule) }
            else -> launchSafe {
                when (passwords.change(s.newPassword)) {
                    PasswordResult.Success -> reduce { copy(dialog = PrivacyDialog.None) }
                    PasswordResult.Unavailable -> reduce { copy(passwordError = com.alphito.kcal.feature.settings.R.string.password_unavailable) }
                }
            }
        }
    }
}
