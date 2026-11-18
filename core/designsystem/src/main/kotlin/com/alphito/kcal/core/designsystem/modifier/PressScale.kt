package com.alphito.kcal.core.designsystem.modifier

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Spring "squish" on press, implemented as a [Modifier.Node]:
 *  - no recomposition: the animated scale is read only inside `placeWithLayer`, so a press
 *    invalidates just the graphics layer (draw phase), never composition or layout;
 *  - no allocation per frame and the node is reused when its inputs are equal.
 */
fun Modifier.pressScale(interactionSource: InteractionSource, pressedScale: Float = 0.96f): Modifier =
    this then PressScaleElement(interactionSource, pressedScale)

private data class PressScaleElement(
    val interactionSource: InteractionSource,
    val pressedScale: Float,
) : ModifierNodeElement<PressScaleNode>() {
    override fun create() = PressScaleNode(interactionSource, pressedScale)
    override fun update(node: PressScaleNode) = node.update(interactionSource, pressedScale)
    override fun InspectorInfo.inspectableProperties() {
        name = "pressScale"
        properties["pressedScale"] = pressedScale
    }
}

private class PressScaleNode(
    private var interactionSource: InteractionSource,
    private var pressedScale: Float,
) : Modifier.Node(), LayoutModifierNode {

    private val scale = Animatable(1f)
    private var collectJob: Job? = null

    override fun onAttach() = collect()

    fun update(source: InteractionSource, scaleTarget: Float) {
        pressedScale = scaleTarget
        if (source != interactionSource) {
            interactionSource = source
            collectJob?.cancel()
            collect()
        }
    }

    private fun collect() {
        collectJob = coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> launch { scale.animateTo(pressedScale, KcalMotion.pressSpring()) }
                    is PressInteraction.Release, is PressInteraction.Cancel ->
                        launch { scale.animateTo(1f, KcalMotion.bouncySpring()) }
                }
            }
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                val s = scale.value
                scaleX = s
                scaleY = s
            }
        }
    }
}
