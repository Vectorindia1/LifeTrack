package com.lifetrack

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
        setContent {
            LifeTrackTheme {
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
