package com.alphito.kcal.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.alphito.kcal.core.designsystem.icon.KcalIcons
import com.alphito.kcal.core.designsystem.modifier.softShadow
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.designsystem.theme.KcalShapes
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Back chevron + plum title used at the top of every detail screen. */
@Composable
fun KcalTopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    centered: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            KcalIconButton(
                icon = KcalIcons.Back, contentDescription = androidx.compose.ui.res.stringResource(com.alphito.kcal.core.designsystem.R.string.ds_back), onClick = onBack,
                container = androidx.compose.ui.graphics.Color.Transparent, size = 40.dp, iconSize = 20.dp,
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            title,
            style = KcalTheme.typography.headlineSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
            color = KcalTheme.colors.textBrand,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier = titleModifier.weight(1f),
        )
        if (centered && onBack != null) Spacer(Modifier.width(46.dp))
        actions()
    }
}

/**
 * kCal dialog: scrim fades while the card springs up from 0.86 scale.
 * The exit animation is played *before* [onDismiss] is invoked so the dialog never pops away.
 * Use [DialogController.dismiss] from buttons to animate out.
 */
@Composable
fun KcalDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissOnOutside: Boolean = true,
    content: @Composable ColumnScope.(DialogController) -> Unit,
) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    val scope = rememberCoroutineScope()
    val controller = remember(visible) {
        DialogController { after ->
            visible.targetState = false
            scope.launch {
                snapshotFlow { visible.isIdle && !visible.currentState }.first { it }
                onDismiss(); after()
            }
        }
    }
    Dialog(
        onDismissRequest = { if (dismissOnOutside) controller.dismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = dismissOnOutside),
    ) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
            AnimatedVisibility(
                visibleState = visible,
                enter = fadeIn(KcalMotion.fadeTween()) + scaleIn(KcalMotion.bouncySpring(), initialScale = 0.86f),
                exit = fadeOut(KcalMotion.fadeTween(140)) + scaleOut(KcalMotion.fadeTween(140), targetScale = 0.92f),
            ) {
                Column(
                    modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .softShadow(KcalTheme.colors.shadow.copy(alpha = 0.25f), cornerRadius = 24.dp, blur = 30.dp, offsetY = 12.dp)
                        .background(KcalTheme.colors.surface, KcalShapes.xl)
                        .verticalScroll(rememberScrollState()) // tall dialogs still fit in landscape
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { content(controller) }
            }
        }
    }
}

/** Animated dismissal handle passed into [KcalDialog] content. */
class DialogController(private val animateOut: (after: () -> Unit) -> Unit) {
    fun dismiss(after: () -> Unit = {}) = animateOut(after)
}

/** Standard dialog body: optional icon chip, title, message, primary + secondary actions. */
@Composable
fun ColumnScope.DialogBody(
    title: String,
    message: String,
    primaryText: String,
    onPrimary: () -> Unit,
    secondaryText: String?,
    onSecondary: () -> Unit,
    icon: Int? = null,
    primaryLeadingIcon: Int? = null,
) {
    val colors = KcalTheme.colors
    if (icon != null) {
        IconChip(icon, size = 56.dp, iconSize = 24.dp)
        Spacer(Modifier.height(16.dp))
    }
    Text(title, style = KcalTheme.typography.title, color = colors.textBrand, textAlign = TextAlign.Center)
    Spacer(Modifier.height(10.dp))
    Text(message, style = KcalTheme.typography.body, color = colors.textSecondary, textAlign = TextAlign.Center)
    Spacer(Modifier.height(20.dp))
    KcalButton(primaryText, onPrimary, Modifier.fillMaxWidth(), leadingIcon = primaryLeadingIcon)
    if (secondaryText != null) {
        Spacer(Modifier.height(6.dp))
        KcalTextButton(secondaryText, onSecondary, color = colors.textBrand)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KcalBottomSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dismiss: () -> Unit = { scope.launch { state.hide() }.invokeOnCompletion { onDismiss() } }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = KcalShapes.sheet,
        containerColor = KcalTheme.colors.surface,
        scrimColor = KcalTheme.colors.scrim,
        dragHandle = {
            Box(Modifier.padding(top = 12.dp).size(width = 40.dp, height = 4.dp).background(KcalTheme.colors.track, KcalShapes.pill))
        },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { content(dismiss) }
    }
}

/** Section header row: title + optional trailing action ("Today's Meal  View All >"). */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = KcalTheme.typography.titleMedium, color = KcalTheme.colors.textBrand, modifier = Modifier.weight(1f))
        trailing()
    }
}
