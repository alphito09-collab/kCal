package com.alphito.kcal.feature.settings.presentation.ui

import com.alphito.kcal.core.designsystem.util.languageDisplayName
import android.Manifest
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import com.alphito.kcal.core.ads.AdsManager
import org.koin.compose.koinInject
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alphito.kcal.core.common.mvi.ObserveEffects
import com.alphito.kcal.core.designsystem.component.DialogBody
import com.alphito.kcal.core.designsystem.component.KcalButton
import com.alphito.kcal.core.designsystem.component.KcalCard
import com.alphito.kcal.core.designsystem.component.KcalDetailScaffold
import com.alphito.kcal.core.designsystem.component.KcalDialog
import com.alphito.kcal.core.designsystem.component.KcalDivider
import com.alphito.kcal.core.designsystem.component.KcalIconButton
import com.alphito.kcal.core.designsystem.component.KcalListRow
import com.alphito.kcal.core.designsystem.component.KcalSwitch
import com.alphito.kcal.core.designsystem.component.KcalTextButton
import com.alphito.kcal.core.designsystem.component.KcalTextField
import com.alphito.kcal.core.designsystem.component.RadioDot
import com.alphito.kcal.core.designsystem.icon.KcalIcons
import com.alphito.kcal.core.designsystem.modifier.kcalClickable
import com.alphito.kcal.core.designsystem.theme.KcalShapes
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import com.alphito.kcal.core.designsystem.util.Flags
import com.alphito.kcal.core.model.AppLanguage
import com.alphito.kcal.core.navigation.LocalNavigator
import com.alphito.kcal.core.navigation.Route
import com.alphito.kcal.feature.settings.R
import com.alphito.kcal.core.common.LegalLinks
import com.alphito.kcal.feature.settings.presentation.components.CardListRow
import com.alphito.kcal.feature.settings.presentation.components.launchSafely
import org.koin.core.parameter.parametersOf
import com.alphito.kcal.feature.settings.presentation.components.toast
import com.alphito.kcal.feature.settings.presentation.mvi.LanguageEffect
import com.alphito.kcal.feature.settings.presentation.mvi.LanguageIntent
import com.alphito.kcal.feature.settings.presentation.mvi.LanguageState
import com.alphito.kcal.feature.settings.presentation.mvi.LanguageViewModel
import com.alphito.kcal.feature.settings.presentation.mvi.PrivacyDialog
import com.alphito.kcal.feature.settings.presentation.mvi.PrivacyEffect
import com.alphito.kcal.feature.settings.presentation.mvi.PrivacyIntent
import com.alphito.kcal.feature.settings.presentation.mvi.PrivacyState
import com.alphito.kcal.feature.settings.presentation.mvi.PrivacyViewModel
import org.koin.compose.viewmodel.koinViewModel

// ------------------------------------------------------------------ Language

@Composable
internal fun LanguageRoute(onboarding: Boolean) {
    val viewModel: LanguageViewModel = koinViewModel { parametersOf(onboarding) }
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEffects(viewModel.effects) {
        when (it) {
            LanguageEffect.Close -> navigator.back()
            // First session moves forward only: Language is replaced so back can't return to it.
            LanguageEffect.ContinueOnboarding -> navigator.replace(Route.Intro)
            // Per-app language (Android 13+ system setting, AppCompat back-port below).
            is LanguageEffect.Apply -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(it.language.tag))
        }
    }
    // First session (opened from the splash): back is disabled on this screen.
    BackHandler(enabled = onboarding) { }
    LanguageScreen(state, viewModel::onIntent)
}

@Composable
internal fun LanguageScreen(state: LanguageState, onIntent: (LanguageIntent) -> Unit) {
    val colors = KcalTheme.colors
    KcalDetailScaffold(
        title = stringResource(R.string.language_title),
        // First session (opened from the splash): this is the first screen, so no way back.
        onBack = if (state.onboarding) null else ({ onIntent(LanguageIntent.Back) }),
        subtitle = stringResource(R.string.language_body),
        actions = {
            // Tick = save (Settings) / continue (first session). Springs, no ripple.
            KcalIconButton(
                KcalIcons.Check, stringResource(R.string.language_save), { onIntent(LanguageIntent.Save) },
                container = colors.primary, tint = colors.onPrimary, size = 42.dp, iconSize = 20.dp,
                modifier = Modifier.padding(end = 8.dp),
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppLanguage.entries.forEach { lang ->
                LanguageItem(lang, selected = lang == state.selected) { onIntent(LanguageIntent.Select(lang)) }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.language_footer), style = KcalTheme.typography.bodySmall, color = colors.textSecondary,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

/**
 * One language. The whole rounded item is the touch target (ripple fills it edge to edge);
 * the selected item tints, gains a plum outline and its check mark springs in.
 */
@Composable
private fun LanguageItem(language: AppLanguage, selected: Boolean, onClick: () -> Unit) {
    val colors = KcalTheme.colors
    val bg by animateColorAsState(if (selected) colors.accentContainer else colors.surface, KcalMotion.fadeTween(), label = "lang-bg")
    val border by animateColorAsState(if (selected) colors.primary else colors.outline, KcalMotion.fadeTween(), label = "lang-border")
    val check by animateFloatAsState(if (selected) 1f else 0f, KcalMotion.bouncySpring(), label = "lang-check")
    Row(
        Modifier
            .fillMaxWidth()
            .kcalClickable(shape = KcalShapes.lg, role = Role.RadioButton, onClick = onClick)
            .background(bg, KcalShapes.lg)
            .border(if (selected) 1.5.dp else 1.dp, border, KcalShapes.lg)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(width = 40.dp, height = 30.dp).clip(KcalShapes.xs).border(1.dp, colors.outline, KcalShapes.xs),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(Flags.resFor(language)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(language.nativeName, style = KcalTheme.typography.titleSmall, color = colors.textPrimary)
            Text(languageDisplayName(language.tag), style = KcalTheme.typography.bodySmall, color = colors.textSecondary)
        }
        Box(
            Modifier.size(24.dp).background(if (selected) colors.primary else colors.surface, KcalShapes.pill)
                .border(1.5.dp, if (selected) colors.primary else colors.outline, KcalShapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(KcalIcons.Check), null, tint = colors.onPrimary,
                modifier = Modifier.size(14.dp).graphicsLayer { scaleX = check; scaleY = check; alpha = check.coerceIn(0f, 1f) },
            )
        }
    }
}

// ------------------------------------------------------------------ Privacy & security

@Composable
internal fun PrivacyRoute() {
    val viewModel: PrivacyViewModel = koinViewModel()
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val shareTitle = stringResource(R.string.download_share_title)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onIntent(PrivacyIntent.ActivityPermissionResult(it))
    }
    ObserveEffects(viewModel.effects) {
        when (it) {
            PrivacyEffect.RequestActivityPermission -> permission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            is PrivacyEffect.ShareExport -> context.launchSafely(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_TEXT, it.json).putExtra(Intent.EXTRA_SUBJECT, shareTitle),
                    shareTitle,
                ),
            )
            is PrivacyEffect.OpenUrl -> context.launchSafely(Intent(Intent.ACTION_VIEW, Uri.parse(it.url)))
            is PrivacyEffect.Message -> context.toast(context.getString(it.res))
            PrivacyEffect.Deleted -> navigator.resetTo(Route.Splash)
            PrivacyEffect.Close -> navigator.back()
        }
    }
    val ads: AdsManager = koinInject()
    val activity = LocalActivity.current
    PrivacyScreen(
        state, viewModel::onIntent,
        onAdChoices = if (ads.privacyOptionsRequired && activity != null) ({ ads.showPrivacyOptions(activity) }) else null,
    )
}

@Composable
internal fun PrivacyScreen(state: PrivacyState, onIntent: (PrivacyIntent) -> Unit, onAdChoices: (() -> Unit)? = null) {
    val colors = KcalTheme.colors
    KcalDetailScaffold(title = stringResource(R.string.privacy_title), onBack = { onIntent(PrivacyIntent.Back) }, centeredTitle = true) {
        val card = PaddingValues(0.dp)
        KcalCard(Modifier.fillMaxWidth(), contentPadding = card) {
            CardListRow(
                stringResource(R.string.privacy_activity), { onIntent(PrivacyIntent.ActivityToggled(!state.activityTracking)) },
                leadingIcon = KcalIcons.Steps,
                trailing = { KcalSwitch(state.activityTracking, { onIntent(PrivacyIntent.ActivityToggled(it)) }) },
            )
        }
        Spacer(Modifier.height(14.dp))
        KcalCard(Modifier.fillMaxWidth(), contentPadding = card) {
            CardListRow(stringResource(R.string.privacy_change_password), { onIntent(PrivacyIntent.Show(PrivacyDialog.ChangePassword)) }, KcalIcons.Lock)
        }
        Spacer(Modifier.height(14.dp))
        KcalCard(Modifier.fillMaxWidth(), contentPadding = card) {
            CardListRow(stringResource(R.string.privacy_download), { onIntent(PrivacyIntent.Show(PrivacyDialog.Download)) }, KcalIcons.Download)
            KcalDivider(Modifier.padding(horizontal = 16.dp))
            CardListRow(stringResource(R.string.privacy_policy), { onIntent(PrivacyIntent.OpenLink(LegalLinks.PRIVACY_POLICY)) }, KcalIcons.PrivacyPolicy)
            KcalDivider(Modifier.padding(horizontal = 16.dp))
            CardListRow(stringResource(R.string.privacy_terms), { onIntent(PrivacyIntent.OpenLink(LegalLinks.TERMS)) }, KcalIcons.Terms)
            // Required by Google's consent policy when the user is in a regulated region.
            if (onAdChoices != null) {
                KcalDivider(Modifier.padding(horizontal = 16.dp))
                CardListRow(stringResource(R.string.privacy_ad_choices), onAdChoices, KcalIcons.Shield)
            }
        }
        Spacer(Modifier.height(14.dp))
        KcalCard(Modifier.fillMaxWidth(), contentPadding = card, border = BorderStroke(1.5.dp, colors.track)) {
            CardListRow(stringResource(R.string.privacy_delete), { onIntent(PrivacyIntent.Show(PrivacyDialog.Delete)) }, KcalIcons.Trash)
        }
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.privacy_footer), style = KcalTheme.typography.bodySmall, color = colors.textSecondary,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }

    when (state.dialog) {
        PrivacyDialog.ChangePassword -> ChangePasswordDialog(state, onIntent)
        PrivacyDialog.Download -> KcalDialog(onDismiss = { onIntent(PrivacyIntent.Dismiss) }) { c ->
            DialogBody(
                title = stringResource(R.string.download_title), message = stringResource(R.string.download_body),
                primaryText = stringResource(R.string.download_button), onPrimary = { onIntent(PrivacyIntent.Download) },
                secondaryText = stringResource(R.string.cancel), onSecondary = { c.dismiss() },
            )
        }
        PrivacyDialog.Delete -> KcalDialog(onDismiss = { onIntent(PrivacyIntent.Dismiss) }) { c ->
            DialogBody(
                title = stringResource(R.string.delete_title), message = stringResource(R.string.delete_body),
                primaryText = stringResource(R.string.delete_confirm), onPrimary = { onIntent(PrivacyIntent.Delete) },
                secondaryText = stringResource(R.string.cancel), onSecondary = { c.dismiss() }, icon = KcalIcons.Trash,
            )
        }
        PrivacyDialog.None -> Unit
    }
}

@Composable
private fun ChangePasswordDialog(state: PrivacyState, onIntent: (PrivacyIntent) -> Unit) {
    val colors = KcalTheme.colors
    KcalDialog(onDismiss = { onIntent(PrivacyIntent.Dismiss) }) { c ->
        Column(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.password_title), style = KcalTheme.typography.title, color = colors.textBrand)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.password_body), style = KcalTheme.typography.body, color = colors.textSecondary)
            Spacer(Modifier.height(16.dp))
            KcalTextField(state.newPassword, { onIntent(PrivacyIntent.NewPassword(it)) }, label = stringResource(R.string.password_new), isPassword = true, placeholder = stringResource(R.string.password_new_hint))
            Spacer(Modifier.height(12.dp))
            KcalTextField(
                state.confirmPassword, { onIntent(PrivacyIntent.ConfirmPassword(it)) }, label = stringResource(R.string.password_confirm),
                isPassword = true, isError = state.passwordError != null, placeholder = stringResource(R.string.password_confirm_hint),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(state.passwordError ?: R.string.password_rule), style = KcalTheme.typography.caption,
                color = if (state.passwordError != null) colors.textBrand else colors.textSecondary,
            )
            Spacer(Modifier.height(16.dp))
            KcalButton(stringResource(R.string.password_save), { onIntent(PrivacyIntent.SavePassword) }, Modifier.fillMaxWidth(), enabled = state.canSavePassword)
            Spacer(Modifier.height(6.dp))
            KcalTextButton(stringResource(R.string.cancel), { c.dismiss() }, Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
