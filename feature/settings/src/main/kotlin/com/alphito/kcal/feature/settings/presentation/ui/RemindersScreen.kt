package com.alphito.kcal.feature.settings.presentation.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alphito.kcal.core.common.mvi.ObserveEffects
import com.alphito.kcal.core.designsystem.component.KcalCard
import com.alphito.kcal.core.designsystem.component.KcalDetailScaffold
import com.alphito.kcal.core.designsystem.component.KcalListRow
import com.alphito.kcal.core.designsystem.component.KcalSwitch
import com.alphito.kcal.core.designsystem.icon.KcalIcons
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import com.alphito.kcal.core.model.ReminderType
import com.alphito.kcal.core.navigation.LocalNavigator
import com.alphito.kcal.feature.settings.R
import com.alphito.kcal.feature.settings.presentation.components.toast
import com.alphito.kcal.feature.settings.presentation.mvi.RemindersEffect
import com.alphito.kcal.feature.settings.presentation.mvi.RemindersIntent
import com.alphito.kcal.feature.settings.presentation.mvi.RemindersState
import com.alphito.kcal.feature.settings.presentation.mvi.RemindersViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun RemindersRoute() {
    val viewModel: RemindersViewModel = koinViewModel()
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pending by remember { mutableStateOf<ReminderType?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pending?.let { viewModel.onIntent(RemindersIntent.PermissionResult(it, granted)) }
        pending = null
    }
    ObserveEffects(viewModel.effects) {
        when (it) {
            is RemindersEffect.RequestPermission ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pending = it.type
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else viewModel.onIntent(RemindersIntent.PermissionResult(it.type, granted = true))
            RemindersEffect.PermissionDenied -> context.toast(context.getString(R.string.reminders_permission_denied))
            RemindersEffect.Close -> navigator.back()
        }
    }
    RemindersScreen(state, viewModel::onIntent)
}

@Composable
internal fun RemindersScreen(state: RemindersState, onIntent: (RemindersIntent) -> Unit) {
    KcalDetailScaffold(
        title = stringResource(R.string.reminders_title),
        onBack = { onIntent(RemindersIntent.Back) },
        subtitle = stringResource(R.string.reminders_body),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                Triple(ReminderType.Water, R.string.reminders_water to R.string.reminders_water_sub, KcalIcons.Water),
                Triple(ReminderType.Meals, R.string.reminders_meals to R.string.reminders_meals_sub, KcalIcons.Utensils),
                Triple(ReminderType.WeighIn, R.string.reminders_weigh_in to R.string.reminders_weigh_in_sub, KcalIcons.Weight),
            ).forEach { (type, text, icon) ->
                val on = type in state.enabled
                KcalCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                    KcalListRow(
                        title = stringResource(text.first),
                        subtitle = stringResource(text.second),
                        leadingIcon = icon,
                        titleColor = KcalTheme.colors.textPrimary,
                        onClick = { onIntent(RemindersIntent.Toggle(type, !on)) },
                        shape = androidx.compose.ui.graphics.RectangleShape,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        trailing = { KcalSwitch(on, { onIntent(RemindersIntent.Toggle(type, it)) }) },
                    )
                }
            }
        }
    }
}
