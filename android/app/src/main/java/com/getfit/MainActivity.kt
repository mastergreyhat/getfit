package com.getfit

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.getfit.notification.NotificationHelper
import com.getfit.ui.theme.GetFitTheme
import com.getfit.ui.navigation.GetFitNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setOptimalDisplayRefreshRate()
        NotificationHelper.createNotificationChannels(this)

        setContent {
            GetFitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GetFitNavHost()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setOptimalDisplayRefreshRate()
    }

    private fun setOptimalDisplayRefreshRate() {
        try {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                val currentDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else windowManager.defaultDisplay
                val modes = currentDisplay?.supportedModes
                val maxRefreshMode = modes?.maxByOrNull { it.refreshRate }
                if (maxRefreshMode != null) {
                    val params = window.attributes
                    params.preferredDisplayModeId = maxRefreshMode.modeId
                    params.preferredRefreshRate = maxRefreshMode.refreshRate
                    window.attributes = params
                }
            }
        } catch (_: Exception) {
            // Safe fallback for devices/emulators without custom display modes
        }
    }
}
