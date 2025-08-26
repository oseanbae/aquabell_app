package com.capstone.aquabell

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.capstone.aquabell.ui.theme.AquabellTheme
import com.capstone.aquabell.ui.AppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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