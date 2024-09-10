// LoginScreen.kt
package com.example.myapplication.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.navigation.NavController
import com.example.myapplication.Screens
import com.example.myapplication.data.firebase.FirebaseAuthManager.loginUser

@Composable
fun LoginScreen(modifier: Modifier = Modifier, navigateToMap: () -> Unit, navigateToSignUp: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome!",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Please log in to continue",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            textStyle = TextStyle(color = Color.Black),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            textStyle = TextStyle(color = Color.Black),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                if (email.isEmpty() || password.isEmpty()) {
                    errorMessage = "Please input both email and password"
                } else {
                    loginUser(email, password) { success, message ->
                        if (success) {
                            navigateToMap()
                        } else {
                            errorMessage = message
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Log In", fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            onClick = { navigateToSignUp() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Up", fontSize = 18.sp)
        }
        if (errorMessage.isNotEmpty()) {
            Snackbar(
                action = {
                    TextButton(onClick = { errorMessage = "" }) {
                        Text("Dismiss", color = Color.White)
                    }
                },
                modifier = Modifier.padding(8.dp)
            ) {
                Text(errorMessage, color = Color.White)
            }
        }
    }
}