package com.alphito.kcal.feature.progress.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alphito.kcal.core.designsystem.component.IconChip
import com.alphito.kcal.core.designsystem.component.KcalCard
import com.alphito.kcal.core.designsystem.theme.KcalTheme

@Composable
internal fun StatLine(label: String, value: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = KcalTheme.typography.body.copy(fontSize = KcalTheme.typography.titleSmall.fontSize), color = KcalTheme.colors.textSecondary, modifier = Modifier.weight(1f))
        value()
    }
}

@Composable
internal fun MetricTile(@DrawableRes icon: Int, label: String, modifier: Modifier = Modifier, value: @Composable () -> Unit) {
    KcalCard(modifier, contentPadding = PaddingValues(start = 12.dp, end = 8.dp, top = 14.dp, bottom = 18.dp)) {
        IconChip(icon, size = 40.dp, iconSize = 20.dp)
        Spacer(Modifier.height(16.dp))
        Text(label, style = KcalTheme.typography.bodySmall, color = KcalTheme.colors.textSecondary, minLines = 2)
        Spacer(Modifier.height(8.dp))
        value()
    }
}
