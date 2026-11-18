package com.alphito.kcal.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alphito.kcal.core.designsystem.motion.kcalSharedBounds
import com.alphito.kcal.core.designsystem.theme.KcalTheme

/**
 * Detail screen shell (Water, Steps, Weight, Health Log …).
 * When [sharedBoundsKey] is set, the whole screen is the target of a container transform from
 * the dashboard card with the same key – the card grows into the screen on a spring.
 */
@Composable
fun KcalDetailScaffold(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    sharedBoundsKey: String? = null,
    subtitle: String? = null,
    scrollable: Boolean = true,
    centeredTitle: Boolean = false,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = KcalTheme.colors
    Column(
        modifier
            .then(if (sharedBoundsKey != null) Modifier.kcalSharedBounds(sharedBoundsKey) else Modifier)
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        KcalTopBar(title = title, onBack = onBack, centered = centeredTitle, modifier = Modifier.padding(horizontal = 12.dp), actions = actions)
        Column(
            Modifier
                .weight(1f)
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = 20.dp),
        ) {
            if (subtitle != null) {
                androidx.compose.material3.Text(subtitle, style = KcalTheme.typography.body, color = colors.textSecondary)
                Spacer(Modifier.height(16.dp))
            } else {
                Spacer(Modifier.height(6.dp))
            }
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}
