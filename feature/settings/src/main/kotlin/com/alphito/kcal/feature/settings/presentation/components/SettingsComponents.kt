package com.alphito.kcal.feature.settings.presentation.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alphito.kcal.core.designsystem.component.KcalCard
import com.alphito.kcal.core.designsystem.component.KcalListRow

/**
 * A settings row in its own rounded card (Figma "Settings" list style).
 * The *card* is the clickable surface, so the ripple fills the whole item edge-to-edge
 * (ripple-only feedback, no shrink).
 */
@Composable
internal fun SettingsCardRow(title: String, onClick: () -> Unit, subtitle: String? = null, leadingIcon: Int? = null) {
    KcalCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp), onClick = onClick) {
        KcalListRow(
            title = title, onClick = null, subtitle = subtitle, leadingIcon = leadingIcon,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        )
    }
}

/** Row inside a multi-row card: rectangular ripple across the full card width (the card clips corners). */
@Composable
internal fun CardListRow(
    title: String,
    onClick: () -> Unit,
    leadingIcon: Int? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    KcalListRow(
        title = title, onClick = onClick, leadingIcon = leadingIcon,
        titleColor = com.alphito.kcal.core.designsystem.theme.KcalTheme.colors.textPrimary,
        shape = androidx.compose.ui.graphics.RectangleShape,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        trailing = trailing,
    )
}

/** Launches an external intent without ever crashing when no app can handle it. */
internal fun Context.launchSafely(intent: Intent, onMissing: () -> Unit = {}) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        onMissing()
    }
}

internal fun Context.toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

internal fun emailIntent(address: String, subject: String) =
    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).putExtra(Intent.EXTRA_EMAIL, arrayOf(address)).putExtra(Intent.EXTRA_SUBJECT, subject)

internal fun storeIntent(packageName: String) =
    Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

internal fun storeWebIntent(packageName: String) =
    Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
