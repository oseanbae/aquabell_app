package com.capstone.aquabell

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.capstone.aquabell.ui.theme.AquabellTheme
import com.capstone.aquabell.ui.AppRoot
import com.capstone.aquabell.utils.FirestoreSeeder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // TODO: Remove FirestoreSeeder before production
        // Call once to seed fake data for /daily_logs testing
        FirestoreSeeder.seedDailyLogs()
        setContent {
            AquabellTheme { AppRoot() }
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