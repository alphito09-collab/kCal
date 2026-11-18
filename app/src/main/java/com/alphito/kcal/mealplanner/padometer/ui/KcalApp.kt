package com.alphito.kcal.mealplanner.padometer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.alphito.kcal.core.designsystem.component.KcalNavRail
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.alphito.kcal.core.common.network.NetworkMonitor
import com.alphito.kcal.core.designsystem.component.BottomBarItem
import com.alphito.kcal.core.designsystem.component.DialogBody
import com.alphito.kcal.core.designsystem.component.KcalBottomBar
import com.alphito.kcal.core.designsystem.component.KcalDialog
import com.alphito.kcal.core.designsystem.component.LocalBottomBarInset
import com.alphito.kcal.core.designsystem.icon.KcalIcons
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.designsystem.motion.LocalEntryAnimatedScope
import com.alphito.kcal.core.designsystem.motion.LocalSharedTransitionScope
import com.alphito.kcal.core.designsystem.motion.kcalRenderAboveSharedTransition
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import com.alphito.kcal.core.navigation.LocalNavigator
import com.alphito.kcal.core.navigation.Navigator
import com.alphito.kcal.core.navigation.Route
import com.alphito.kcal.core.navigation.rememberNavigator
import com.alphito.kcal.mealplanner.padometer.R
import com.alphito.kcal.mealplanner.padometer.navigation.kcalPopTransition
import com.alphito.kcal.mealplanner.padometer.navigation.kcalPredictivePopTransition
import com.alphito.kcal.mealplanner.padometer.navigation.kcalPushTransition
import kotlinx.collections.immutable.persistentListOf
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import org.koin.compose.navigation3.EntryProviderInstaller
import org.koin.core.annotation.KoinExperimentalAPI

private val TopLevelTabs = persistentListOf(Route.Home, Route.DietPlan, Route.Progress, Route.Settings)
private val BottomBarHeight = 69.dp

@OptIn(ExperimentalSharedTransitionApi::class, KoinExperimentalAPI::class)
@Composable
fun KcalApp(onExit: () -> Unit) {
    KcalTheme {
        val navigator = rememberNavigator(Route.Splash)
        val koin = getKoin()
        // Each feature contributes its own entries through Koin's `navigation<Route>{}` DSL.
        // Built once – the provider is a pure lookup table and never needs to recompose.
        val entryProvider = remember(koin) {
            val installers = koin.getAll<EntryProviderInstaller>()
            entryProvider<NavKey> {
                @Suppress("UNCHECKED_CAST")
                val scope = this as EntryProviderScope<Any>
                installers.forEach { install -> scope.install() }
            }
        }
        // Bridges Navigation 3's per-entry AnimatedContentScope into the design system, so
        // features can use shared elements without depending on navigation3-ui.
        val animatedScopeDecorator = remember {
            NavEntryDecorator<NavKey> { entry ->
                CompositionLocalProvider(LocalEntryAnimatedScope provides LocalNavAnimatedContentScope.current) { entry.Content() }
            }
        }
        SharedTransitionLayout(Modifier.fillMaxSize().background(KcalTheme.colors.background)) {
            // Window-size aware shell: bottom bar on phones (portrait), side rail on tablets and
            // in landscape (≥ 600 dp). Horizontal safe-drawing insets (side nav bar, display
            // cutout) are applied once here; each screen handles its own top/bottom insets.
            BoxWithConstraints(
                Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            ) {
                val useRail = maxWidth >= RailBreakpoint
                val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                val bottomInset = if (useRail) navBottom else BottomBarHeight + navBottom
                CompositionLocalProvider(
                    LocalNavigator provides navigator,
                    LocalSharedTransitionScope provides this@SharedTransitionLayout,
                    LocalBottomBarInset provides bottomInset,
                ) {
                    Row(Modifier.fillMaxSize()) {
                        if (useRail) KcalSideNavigation(navigator)
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            NavDisplay(
                                backStack = navigator.backStack,
                                onBack = { navigator.back() },
                                entryDecorators = listOf(
                                    rememberSaveableStateHolderNavEntryDecorator(),
                                    rememberViewModelStoreNavEntryDecorator(),
                                    animatedScopeDecorator,
                                    readableWidthDecorator,
                                ),
                                sharedTransitionScope = this@SharedTransitionLayout,
                                transitionSpec = { kcalPushTransition(rtl) },
                                popTransitionSpec = { kcalPopTransition(rtl) },
                                predictivePopTransitionSpec = { edge -> kcalPredictivePopTransition(edge) },
                                entryProvider = entryProvider,
                                modifier = Modifier.fillMaxSize(),
                            )
                            if (!useRail) KcalBottomNavigation(navigator, Modifier.align(Alignment.BottomCenter))
                        }
                    }
                    RootBackAndExit(navigator, onExit)
                    OfflineDialog(navigator)
                }
            }
        }
    }
}

/**
 * - Screens keep a phone-like readable width on tablets / landscape and are centred.
 * - Language changes are applied *in place* (the activity handles `locale|layoutDirection`, so
 *   there is no recreation and no black frame). Keying the entry on the locale re-runs its
 *   composition so every locale-dependent value (numbers, dates) refreshes; ViewModel state
 *   and the back stack are untouched.
 */
private val readableWidthDecorator = NavEntryDecorator<NavKey> { entry ->
    val localeTags = LocalConfiguration.current.locales.toLanguageTags()
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = MaxContentWidth).fillMaxSize()) {
            key(localeTags) { entry.Content() }
        }
    }
}

private val RailBreakpoint = 600.dp
private val MaxContentWidth = 640.dp

@Composable
private fun navItems() = persistentListOf(
    BottomBarItem(stringResource(R.string.nav_home), KcalIcons.NavHome),
    BottomBarItem(stringResource(R.string.nav_diet_plan), KcalIcons.NavDiet),
    BottomBarItem(stringResource(R.string.nav_progress), KcalIcons.NavProgress),
    BottomBarItem(stringResource(R.string.nav_settings), KcalIcons.NavSettings),
)

@Composable
private fun KcalSideNavigation(navigator: Navigator) {
    val items = navItems()
    val selected by remember(navigator) { derivedStateOf { TopLevelTabs.indexOf(navigator.currentTopLevel) } }
    AnimatedVisibility(
        visible = navigator.isOnTopLevel,
        enter = slideInHorizontally(KcalMotion.slideSpring) { -it } + fadeIn(),
        exit = slideOutHorizontally(KcalMotion.slideSpring) { -it } + fadeOut(),
        modifier = Modifier.kcalRenderAboveSharedTransition(),
    ) {
        KcalNavRail(items, selected, { navigator.selectTab(TopLevelTabs[it]) })
    }
}

@Composable
private fun KcalBottomNavigation(navigator: Navigator, modifier: Modifier = Modifier) {
    val items = navItems()
    val selected by remember(navigator) { derivedStateOf { TopLevelTabs.indexOf(navigator.currentTopLevel) } }
    AnimatedVisibility(
        visible = navigator.isOnTopLevel,
        enter = slideInVertically(KcalMotion.slideSpring) { it } + fadeIn(),
        exit = slideOutVertically(KcalMotion.slideSpring) { it } + fadeOut(),
        modifier = modifier.kcalRenderAboveSharedTransition(),
    ) {
        KcalBottomBar(items, selected, { navigator.selectTab(TopLevelTabs[it]) })
    }
}

/** Back on the Home root asks before leaving ("Leaving already?"). */
@Composable
private fun RootBackAndExit(navigator: Navigator, onExit: () -> Unit) {
    var showExit by rememberSaveable { mutableStateOf(false) }
    val atHomeRoot by remember(navigator) { derivedStateOf { navigator.backStack.size == 1 && navigator.current == Route.Home } }
    BackHandler(enabled = atHomeRoot && !showExit) { showExit = true }
    if (showExit) {
        KcalDialog(onDismiss = { showExit = false }) { c ->
            DialogBody(
                title = stringResource(R.string.exit_title), message = stringResource(R.string.exit_body),
                primaryText = stringResource(R.string.exit_stay), onPrimary = { c.dismiss() },
                secondaryText = stringResource(R.string.exit_leave), onSecondary = { c.dismiss(onExit) },
            )
        }
    }
}

/**
 * kCal is offline-first, so losing connectivity never blocks the UI – the dialog only informs
 * once per session (after onboarding) and "Try again" re-checks the live network state.
 */
@Composable
private fun OfflineDialog(navigator: Navigator) {
    val networkMonitor: NetworkMonitor = koinInject()
    val online by networkMonitor.isOnline.collectAsStateWithLifecycle()
    var dismissedThisSession by rememberSaveable { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    val inMainApp by remember(navigator) { derivedStateOf { navigator.currentTopLevel != null } }
    LaunchedEffect(online, inMainApp) {
        if (!online && inMainApp && !dismissedThisSession) visible = true
        if (online) visible = false
    }
    if (visible) {
        KcalDialog(onDismiss = { visible = false; dismissedThisSession = true }, dismissOnOutside = false) { c ->
            DialogBody(
                title = stringResource(R.string.offline_title), message = stringResource(R.string.offline_body),
                primaryText = stringResource(R.string.offline_retry),
                onPrimary = { if (networkMonitor.isOnline.value) c.dismiss() },
                secondaryText = stringResource(R.string.offline_continue), onSecondary = { c.dismiss() },
                primaryLeadingIcon = KcalIcons.Refresh,
            )
        }
    }
}
