package com.lifetrack

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifetrack.core.data.AppPreferences
import com.lifetrack.core.data.ThemeMode
import com.lifetrack.core.navigation.LifeTrackApp
import com.lifetrack.core.ui.theme.LifeTrackTheme
import com.lifetrack.notification.Notifier

class MainActivity : ComponentActivity() {

    /**
     * The only permission this app asks for. Nothing is gated on the answer — a
     * refusal simply means no digest, and every tracker still works — so the result
     * is deliberately ignored rather than nagged about.
     */
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        maybeRequestNotificationPermission()
        val preferences = (application as LifeTrackApplication).container
            .preferencesRepository.preferences

        setContent {
            val prefs by preferences.collectAsStateWithLifecycle(initialValue = AppPreferences())
            LifeTrackTheme(
                darkTheme = when (prefs.themeMode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                },
            ) {
                LifeTrackApp()
            }
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (Notifier.canPost(this)) return
        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
