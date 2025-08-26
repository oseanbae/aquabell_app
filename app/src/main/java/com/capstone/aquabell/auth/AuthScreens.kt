package com.capstone.aquabell.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AuthGate(
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val auth = remember { FirebaseAuth.getInstance() }
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    LaunchedEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { fa ->
            currentUser = fa.currentUser
            if (fa.currentUser != null) onAuthenticated()
        }
        auth.addAuthStateListener(listener)
    }

    if (currentUser == null) {
        AuthScreen(modifier)
    }
}

@Composable
private fun AuthScreen(modifier: Modifier = Modifier) {
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val auth = remember { FirebaseAuth.getInstance() }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = if (isLogin) "Welcome back" else "Create account", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
        errorMsg?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            errorMsg = null
            if (isLogin) {
                auth.signInWithEmailAndPassword(email.trim(), password)
                    .addOnFailureListener { errorMsg = it.message }
            } else {
                auth.createUserWithEmailAndPassword(email.trim(), password)
                    .addOnFailureListener { errorMsg = it.message }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(if (isLogin) "Log in" else "Sign up") }
        TextButton(onClick = { isLogin = !isLogin }) { Text(if (isLogin) "No account? Sign up" else "Have an account? Log in") }
    }
}


