package com.alphito.kcal.mealplanner.padometer

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.alphito.kcal.mealplanner.padometer.ui.KcalApp

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
        window.isNavigationBarContrastEnforced = false
        setContent { KcalApp(onExit = ::finish) }
    }
}
