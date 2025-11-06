package com.capstone.aquabell

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.capstone.aquabell.ui.theme.AquabellTheme
import com.capstone.aquabell.ui.AppRoot
import com.capstone.aquabell.utils.FirestoreSeeder
import com.capstone.aquabell.ui.utils.NotificationUtils

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationUtils.initNotificationChannel(this)
        requestPostNotificationsIfNeeded()
        // TODO: Remove FirestoreSeeder before production
        // Call once to seed fake data for /daily_logs testing
        FirestoreSeeder.seedDailyLogs()
        setContent {
            AquabellTheme { AppRoot() }
        }
    }

    private fun requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1000)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    AquabellTheme {
        AppRoot()
    }
}