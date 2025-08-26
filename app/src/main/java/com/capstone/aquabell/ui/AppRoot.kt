package com.capstone.aquabell.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.capstone.aquabell.auth.AuthGate
import com.capstone.aquabell.data.FirebaseRepository
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AppRoot(modifier: Modifier = Modifier) {
    val repo = remember { FirebaseRepository() }
    var isAuthed by remember { mutableStateOf(false) }
    var connection by remember { mutableStateOf(FirebaseRepository.ConnectionState.CONNECTING) }
    var cached by remember { mutableStateOf<com.capstone.aquabell.data.model.LiveDataSnapshot?>(null) }

    LaunchedEffect(Unit) {
        cached = repo.getCachedLiveData()
        repo.connectionState.collectLatest { connection = it }
    }

    if (!isAuthed) {
        AuthGate(onAuthenticated = { isAuthed = true })
        return
    }

    // Start a Firestore listener once authenticated so connection state can advance
    LaunchedEffect(isAuthed) {
        if (isAuthed) {
            repo.liveData().collectLatest { /* no-op, just to drive connection state */ }
        }
    }

    HomeScreen()
}




