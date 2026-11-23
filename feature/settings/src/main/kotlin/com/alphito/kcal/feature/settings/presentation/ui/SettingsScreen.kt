package com.alphito.kcal.feature.settings.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alphito.kcal.core.common.mvi.ObserveEffects
import com.alphito.kcal.core.designsystem.component.DialogBody
import com.alphito.kcal.core.designsystem.component.IconChip
import com.alphito.kcal.core.designsystem.component.KcalBottomSheet
import com.alphito.kcal.core.designsystem.component.KcalButton
import com.alphito.kcal.core.designsystem.component.KcalCard
import com.alphito.kcal.core.designsystem.component.KcalDialog
import com.alphito.kcal.core.designsystem.component.KcalIconButton
import com.alphito.kcal.core.designsystem.component.KcalTextButton
import com.alphito.kcal.core.designsystem.component.StarRating
import com.alphito.kcal.core.designsystem.icon.KcalIcons
import com.alphito.kcal.core.designsystem.theme.KcalShapes
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import com.alphito.kcal.core.model.PremiumPlan
import com.alphito.kcal.core.navigation.LocalNavigator
import com.alphito.kcal.core.navigation.Route
import com.alphito.kcal.feature.settings.R
import com.alphito.kcal.feature.settings.presentation.components.SettingsCardRow
import com.alphito.kcal.feature.settings.presentation.components.emailIntent
import com.alphito.kcal.feature.settings.presentation.components.launchSafely
import com.alphito.kcal.feature.settings.presentation.components.storeIntent
import com.alphito.kcal.feature.settings.presentation.components.storeWebIntent
import com.alphito.kcal.feature.settings.presentation.mvi.SettingsEffect
import com.alphito.kcal.feature.settings.presentation.mvi.SettingsIntent
import com.alphito.kcal.feature.settings.presentation.mvi.SettingsSheet
import com.alphito.kcal.feature.settings.presentation.mvi.SettingsState
import com.alphito.kcal.feature.settings.presentation.mvi.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun SettingsRoute() {
    val viewModel: SettingsViewModel = koinViewModel()
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val supportEmail = stringResource(R.string.support_email)
    val subject = stringResource(R.string.help_email_subject)
    ObserveEffects(viewModel.effects) {
        when (it) {
            is SettingsEffect.Navigate -> navigator.navigate(it.route)
            SettingsEffect.OpenEmail -> context.launchSafely(emailIntent(supportEmail, subject))
            SettingsEffect.OpenStoreListing -> context.launchSafely(storeIntent(context.packageName)) {
                context.launchSafely(storeWebIntent(context.packageName))
            }
        }
    }
    SettingsScreen(state, viewModel::onIntent)
}

@Composable
internal fun SettingsScreen(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    val colors = KcalTheme.colors
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.settings_title), style = KcalTheme.typography.headline.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = colors.textBrand)
        Spacer(Modifier.height(16.dp))
        KcalCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).background(colors.accentContainer, KcalShapes.pill), contentAlignment = Alignment.Center) {
                    Text(state.initials, style = KcalTheme.typography.title, color = colors.textBrand)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(state.name, style = KcalTheme.typography.titleMedium, color = colors.textPrimary)
                    Text(stringResource(R.string.settings_local_profile), style = KcalTheme.typography.bodySmall, color = colors.textSecondary)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingsCardRow(stringResource(R.string.settings_edit_profile), { onIntent(SettingsIntent.Open(Route.AboutYou(edit = true))) })
            SettingsCardRow(stringResource(R.string.settings_health), { onIntent(SettingsIntent.Open(Route.HealthConditions(edit = true))) })
            SettingsCardRow(stringResource(R.string.settings_region), { onIntent(SettingsIntent.Open(Route.RegionBudget(edit = true))) })
            SettingsCardRow(stringResource(R.string.settings_language), { onIntent(SettingsIntent.Open(Route.Language())) })
            SettingsCardRow(stringResource(R.string.settings_reminders), { onIntent(SettingsIntent.Open(Route.Reminders)) })
            if (state.premiumEnabled) SettingsCardRow(
                title = stringResource(R.string.settings_premium),
                subtitle = state.plan?.let { stringResource(R.string.settings_premium_active, stringResource(it.label())) }
                    ?: stringResource(R.string.settings_premium_free),
                leadingIcon = KcalIcons.Crown,
                onClick = { onIntent(SettingsIntent.Open(Route.Premium())) },
            )
            if (state.premiumEnabled) SettingsCardRow(stringResource(R.string.settings_payments), { onIntent(SettingsIntent.Show(SettingsSheet.Payments)) })
            SettingsCardRow(stringResource(R.string.settings_rate), { onIntent(SettingsIntent.Show(SettingsSheet.Rate)) })
            SettingsCardRow(stringResource(R.string.settings_privacy), { onIntent(SettingsIntent.Open(Route.PrivacySecurity)) })
            SettingsCardRow(stringResource(R.string.settings_help), { onIntent(SettingsIntent.Show(SettingsSheet.Help)) })
        }
        com.alphito.kcal.core.designsystem.component.BottomBarSpacer(extra = 24.dp)
    }

    when (state.sheet) {
        SettingsSheet.Help -> HelpSheet(onIntent)
        SettingsSheet.Payments -> PaymentsSheet(state.plan, onIntent)
        SettingsSheet.Rate -> RateDialog(state.rating, onIntent)
        SettingsSheet.None -> Unit
    }
}

private fun PremiumPlan.label() = when (this) {
    PremiumPlan.Weekly -> R.string.settings_weekly
    PremiumPlan.Monthly -> R.string.settings_monthly
}

@Composable
private fun HelpSheet(onIntent: (SettingsIntent) -> Unit) {
    val colors = KcalTheme.colors
    KcalBottomSheet(onDismiss = { onIntent(SettingsIntent.Dismiss) }) { dismiss ->
        IconChip(KcalIcons.Mail, size = 56.dp, iconSize = 24.dp)
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.help_title), style = KcalTheme.typography.title, color = colors.textBrand)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.help_body), style = KcalTheme.typography.body, color = colors.textSecondary)
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.support_email),
            style = KcalTheme.typography.titleSmall.copy(textDecoration = TextDecoration.Underline), color = colors.textBrand,
            modifier = Modifier.background(colors.accentContainer, KcalShapes.pill).padding(horizontal = 20.dp, vertical = 10.dp),
        )
        Spacer(Modifier.height(16.dp))
        KcalButton(stringResource(R.string.help_email_button), { onIntent(SettingsIntent.EmailSupport) }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        KcalTextButton(stringResource(R.string.help_not_now), dismiss)
    }
}

@Composable
private fun PaymentsSheet(plan: PremiumPlan?, onIntent: (SettingsIntent) -> Unit) {
    KcalBottomSheet(onDismiss = { onIntent(SettingsIntent.Dismiss) }) { _ ->
        Text(stringResource(R.string.payments_title), style = KcalTheme.typography.title, color = KcalTheme.colors.textBrand)
        Spacer(Modifier.height(12.dp))
        Text(
            plan?.let { stringResource(R.string.payments_active, stringResource(it.label())) } ?: stringResource(R.string.payments_empty),
            style = KcalTheme.typography.body, color = KcalTheme.colors.textSecondary, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun RateDialog(rating: Int, onIntent: (SettingsIntent) -> Unit) {
    val colors = KcalTheme.colors
    KcalDialog(onDismiss = { onIntent(SettingsIntent.Dismiss) }) { _ ->
        IconChip(KcalIcons.StarFilled, size = 56.dp, iconSize = 22.dp)
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.rate_title), style = KcalTheme.typography.title, color = colors.textBrand)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(if (rating > 0) R.string.rate_thanks else R.string.rate_body), style = KcalTheme.typography.body,
            color = colors.textSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        StarRating(rating, { onIntent(SettingsIntent.Rate(it)) })
    }
}
