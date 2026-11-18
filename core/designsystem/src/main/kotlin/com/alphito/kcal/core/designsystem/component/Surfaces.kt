package com.alphito.kcal.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alphito.kcal.core.designsystem.modifier.kcalClickable
import com.alphito.kcal.core.designsystem.modifier.softShadow
import com.alphito.kcal.core.designsystem.theme.KcalShapes
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import com.alphito.kcal.core.designsystem.util.FoodImages

/**
 * White rounded card with a soft plum shadow. When [onClick] is set the whole card gets the
 * kCal press-scale + ripple clipped to the card radius.
 */
@Composable
fun KcalCard(
    modifier: Modifier = Modifier,
    shape: Shape = KcalShapes.card,
    cornerRadius: Dp = 22.dp,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    border: BorderStroke? = null,
    color: Color = KcalTheme.colors.surface,
    elevated: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = KcalTheme.colors
    Column(
        modifier
            .then(if (elevated) Modifier.softShadow(colors.shadow, cornerRadius = cornerRadius) else Modifier)
            .then(if (onClick != null) Modifier.kcalClickable(shape = shape, onClick = onClick) else Modifier.clip(shape))
            .background(color, shape)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

/** Circular blush container holding a line icon (dashboard action grid, settings rows …). */
@Composable
fun IconChip(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: Dp = 22.dp,
    container: Color = KcalTheme.colors.accentContainer,
    tint: Color = KcalTheme.colors.primary,
) {
    Box(modifier.size(size).background(container, KcalShapes.pill), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Food thumbnail – bundled 192px WebP, cropped into a rounded square. */
@Composable
fun FoodThumb(imageKey: String?, modifier: Modifier = Modifier, size: Dp = 42.dp, shape: Shape = KcalShapes.sm) {
    Image(
        painter = painterResource(FoodImages.resFor(imageKey)),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.size(size).clip(shape),
    )
}

/** Small rounded label ("BEST MATCH", "Dairy", "−0.6 kg"). */
@Composable
fun KcalBadge(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = KcalTheme.colors.accentContainer,
    content: Color = KcalTheme.colors.textBrand,
    uppercase: Boolean = false,
) {
    Text(
        text = if (uppercase) text.uppercase() else text,
        style = if (uppercase) KcalTheme.typography.overline else KcalTheme.typography.labelSmall,
        color = content,
        modifier = modifier
            .background(container, KcalShapes.pill)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/**
 * Title + chevron row used by Settings / Privacy. The ripple fills the *whole* row
 * (padding is inside the clickable area); pass [shape] = RectangleShape when the row sits
 * inside a card so the card's own corners clip it. [onClick] = null renders a static row.
 */
@Composable
fun KcalListRow(
    title: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    @DrawableRes leadingIcon: Int? = null,
    titleColor: Color = KcalTheme.colors.textBrand,
    shape: Shape = KcalShapes.md,
    contentPadding: PaddingValues = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.kcalClickable(shape = shape, onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            IconChip(leadingIcon, size = 36.dp, iconSize = 18.dp)
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = KcalTheme.typography.label.copy(fontSize = KcalTheme.typography.titleSmall.fontSize), color = titleColor)
            if (subtitle != null) {
                Text(subtitle, style = KcalTheme.typography.bodySmall, color = KcalTheme.colors.textSecondary)
            }
        }
        if (trailing != null) trailing() else {
            Icon(
                painterResource(com.alphito.kcal.core.designsystem.icon.KcalIcons.ChevronRight), null,
                tint = KcalTheme.colors.textTertiary, modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
fun KcalDivider(modifier: Modifier = Modifier, color: Color = KcalTheme.colors.outline) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}
