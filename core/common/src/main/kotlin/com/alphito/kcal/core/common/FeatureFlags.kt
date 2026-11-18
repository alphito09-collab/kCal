package com.alphito.kcal.core.common

/** Compile-time switches for features that exist in code but are not shipped yet. */
object FeatureFlags {
    /**
     * Premium (intro + plans screens, Settings "kCal Premium" / "Payment History" rows and the
     * dashboard free-trial dialog). Off for now; flip to `true` to bring it all back.
     */
    const val PREMIUM_ENABLED = false
}
