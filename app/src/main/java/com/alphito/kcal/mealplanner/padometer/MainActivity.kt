package com.alphito.kcal.mealplanner.padometer

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.alphito.kcal.feature.steps.domain.StepTrackingController
import com.alphito.kcal.mealplanner.padometer.ui.KcalApp
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Single activity. AppCompat is used only for the per-app language back-port; the whole UI is
 * Compose + Navigation 3. The system splash dismisses on the first Compose frame, which already
 * shows the same kCal bowl – so the hand-off is seamless and costs no extra startup time.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        // Let the ivory UI show through 3-button navigation instead of a white contrast scrim.
        // API 29+ only – calling it on Android 9 (minSdk 28) would crash at launch.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        setContent { KcalApp(onExit = ::finish) }
    }

    override fun onStart() {
        super.onStart()
        // Step tracking the user left on comes back with the app (the system may have stopped it).
        // Foreground-service starts are allowed here because the activity is visible.
        lifecycleScope.launch { stepTracking.resumeIfEnabled() }
    }

    private val stepTracking: StepTrackingController by inject()
}
