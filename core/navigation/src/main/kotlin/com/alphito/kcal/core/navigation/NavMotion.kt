package com.alphito.kcal.core.navigation

/**
 * Per-destination motion hints attached as Navigation 3 entry metadata. The app's NavDisplay
 * picks the transition from the *target* scene's hint:
 *  - [TopLevel]  → fade-through (bottom-bar tabs, no horizontal travel)
 *  - [Container] → plain cross-fade so the card → screen shared-bounds transform leads
 *  - [Fade]      → cross-fade (launch flow / shared logo)
 *  - none        → shared-axis X slide (default push / pop)
 */
object NavMotion {
    const val KEY = "kcal.motion"
    val TopLevel: Map<String, Any> = mapOf(KEY to "topLevel")
    val Container: Map<String, Any> = mapOf(KEY to "container")
    val Fade: Map<String, Any> = mapOf(KEY to "fade")
}
