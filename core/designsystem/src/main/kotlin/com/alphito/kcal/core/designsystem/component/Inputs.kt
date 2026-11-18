package com.alphito.kcal.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.designsystem.theme.KcalShapes
import com.alphito.kcal.core.designsystem.theme.KcalTheme

/** Keyboard flavours used by kCal forms (a stable enum instead of the value-class `KeyboardType`). */
enum class KcalKeyboard(internal val type: KeyboardType) {
    Text(KeyboardType.Text), Number(KeyboardType.Number), Decimal(KeyboardType.Decimal), Password(KeyboardType.Password)
}

/** Labelled outlined field from "Tell us about you" – border animates to plum on focus. */
@Composable
fun KcalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    keyboard: KcalKeyboard = KcalKeyboard.Text,
    imeDone: Boolean = false,
    onImeAction: (() -> Unit)? = null,
    isPassword: Boolean = false,
    isError: Boolean = false,
    suffix: String? = null,
    textStyle: TextStyle = KcalTheme.typography.body.copy(fontSize = KcalTheme.typography.titleSmall.fontSize),
    singleLine: Boolean = true,
) {
    val colors = KcalTheme.colors
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        when {
            isError -> colors.selectedBorder
            focused -> colors.primary
            else -> colors.outline
        },
        KcalMotion.fadeTween(), label = "field-border",
    )
    Column(modifier) {
        if (label != null) {
            Text(label, style = KcalTheme.typography.bodySmall.copy(fontSize = KcalTheme.typography.body.fontSize), color = colors.textSecondary)
            Spacer(Modifier.height(8.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            interactionSource = source,
            textStyle = textStyle.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.primary),
            visualTransformation = if (isPassword) PasswordVisualTransformation('•') else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPassword) KeyboardType.Password else keyboard.type,
                imeAction = if (imeDone) ImeAction.Done else ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onAny = { onImeAction?.invoke() }),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 51.dp)
                        .background(colors.surface, KcalShapes.md)
                        .border(1.5.dp, borderColor, KcalShapes.md)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(placeholder, style = textStyle, color = colors.textTertiary)
                        }
                        inner()
                    }
                    if (suffix != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(suffix, style = KcalTheme.typography.body, color = colors.textSecondary)
                    }
                }
            },
        )
    }
}

/** Big centred numeric input ("68.4 kg", "110 mg/dL"). */
@Composable
fun BigNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    modifier: Modifier = Modifier,
    keyboard: KcalKeyboard = KcalKeyboard.Decimal,
    placeholder: String? = null,
) {
    val colors = KcalTheme.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = KcalTheme.typography.hero.copy(color = colors.textPrimary, textAlign = TextAlign.End),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard.type, imeAction = ImeAction.Done),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(colors.surfaceMuted, KcalShapes.lg)
                    .border(1.5.dp, colors.track, KcalShapes.lg)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(placeholder, style = KcalTheme.typography.hero, color = colors.textTertiary.copy(alpha = 0.5f))
                    }
                    inner()
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(0.8f), contentAlignment = Alignment.CenterStart) {
                    Text(unit, style = KcalTheme.typography.titleMedium, color = colors.textSecondary)
                }
            }
        },
    )
}
