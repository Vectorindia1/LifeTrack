package com.lifetrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lifetrack.core.navigation.LifeTrackApp
import com.lifetrack.core.ui.theme.LifeTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LifeTrackTheme {
                LifeTrackApp()
            }
        }
    }
}
