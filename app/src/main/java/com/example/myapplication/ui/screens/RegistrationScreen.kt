package com.example.myapplication.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.data.firebase.FirebaseAuthManager
import java.io.InputStream
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.navigation.NavController


@Composable
fun RegistrationScreen(
    modifier: Modifier = Modifier,
    navigateToLogin: () -> Unit,
    navController: NavController
) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var userMessage by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }
    var imageBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    val context = LocalContext.current

    val pickImage =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
            imageUri = uri
            uri?.let {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 500, 500, true)
                imageBitmap = resizedBitmap.asImageBitmap()
            }
        }

    fun validateInputs(): Boolean {
        return email.isNotEmpty() && password.isNotEmpty() && username.isNotEmpty() &&
                fullName.isNotEmpty() && phoneNumber.isNotEmpty() && imageUri != null
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "Please fill in the details to sign up",
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
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                textStyle = TextStyle(color = Color.Black),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                textStyle = TextStyle(color = Color.Black),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                textStyle = TextStyle(color = Color.Black),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap!!,
                    contentDescription = "Selected Profile Picture",
                    modifier = Modifier.padding(16.dp)
                )
            }
            Button(onClick = { pickImage.launch("image/*") }) {
                Text("Upload Photo")
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    if (validateInputs()) {
                        isRegistering = true
                        FirebaseAuthManager.registerUser(
                            email,
                            password,
                            fullName,
                            phoneNumber,
                            imageUri!!,
                            username,
                            navController
                        ) { success, message ->
                            isRegistering = false
                            userMessage = message
                            if (success) {
                                navigateToLogin()
                            } else {
                                errorMessage = message
                            }
                        }
                    } else {
                        errorMessage = "Please fill in all fields and select an image."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Register", fontSize = 18.sp)
            }
            if (userMessage.isNotEmpty()) {
                Text(userMessage, color = MaterialTheme.colorScheme.error)
            }
            if (isRegistering) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            }
        }
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