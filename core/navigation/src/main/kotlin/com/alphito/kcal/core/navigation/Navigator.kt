package com.alphito.kcal.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclassesOfSealed

/**
 * Thin, stable wrapper around the Navigation 3 back stack (a snapshot-state list that is
 * saved across process death by [rememberNavBackStack]).
 *
 * Top-level tabs follow the Material guideline: Home is always the root of the main graph,
 * every other tab sits on top of it, so system back from any tab returns to Home and
 * back from Home leaves the app.
 */
@Stable
class Navigator(val backStack: NavBackStack<NavKey>) {

    val current: NavKey? by derivedStateOf { backStack.lastOrNull() }

    /** The top-level tab that owns the current destination (drives the bottom bar). */
    val currentTopLevel: Route.TopLevel? by derivedStateOf {
        backStack.lastOrNull { it is Route.TopLevel } as? Route.TopLevel
    }

    val isOnTopLevel: Boolean by derivedStateOf { backStack.lastOrNull() is Route.TopLevel }

    fun navigate(route: Route) {
        if (backStack.lastOrNull() == route) return // ignore double taps
        backStack.add(route)
    }

    /** Replace the entire stack – used when finishing onboarding or signing out. */
    fun resetTo(route: Route) {
        backStack.clear()
        backStack.add(route)
    }

    /** Replace the current destination (e.g. Splash → Get started) so back does not return to it. */
    fun replace(route: Route) {
        if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex)
        backStack.add(route)
    }

    fun selectTab(tab: Route.TopLevel) {
        if (currentTopLevel == tab && backStack.lastOrNull() == tab) return
        val homeIndex = backStack.indexOf(Route.Home)
        if (homeIndex == -1) {
            resetTo(Route.Home)
        } else {
            // Drop everything above Home, then stack the selected tab (unless it is Home).
            while (backStack.lastIndex > homeIndex) backStack.removeAt(backStack.lastIndex)
        }
        if (tab != Route.Home) backStack.add(tab)
    }

    /** @return false when there is nothing to pop (caller decides whether to exit). */
    fun back(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }
}

/**
 * Explicit polymorphic registration of every [Route] – no reflection, so it is R8-safe and
 * the back stack can be restored after process death.
 */
@OptIn(ExperimentalSerializationApi::class)
private val RouteSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) { subclassesOfSealed<Route>() }
    }
}

@Composable
fun rememberNavigator(start: Route): Navigator {
    val backStack = rememberNavBackStack(RouteSavedStateConfiguration, start)
    return remember(backStack) { Navigator(backStack) }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("No Navigator provided – wrap content in CompositionLocalProvider(LocalNavigator provides …)")
}
