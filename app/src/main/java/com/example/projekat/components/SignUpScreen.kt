package com.example.projekat.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import java.io.InputStream

@Composable
fun SignUpScreen(modifier: Modifier = Modifier, navigateToLogin: () -> Unit) {

    val scrollState = rememberScrollState()
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val confirmPassword = remember { mutableStateOf("") }
    val username = remember { mutableStateOf("") }
    val name = remember { mutableStateOf("") }
    val surname = remember { mutableStateOf("") }
    val phoneNumber = remember { mutableStateOf("") }
    val profileImageUri = remember { mutableStateOf<Uri?>(null) }
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        profileImageUri.value = uri
    }

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextField(
            value = email.value,
            onValueChange = { email.value = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        TextField(
            value = username.value,
            onValueChange = { username.value = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )

        TextField(
            value = name.value,
            onValueChange = { name.value = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )

        TextField(
            value = surname.value,
            onValueChange = { surname.value = it },
            label = { Text("Surname") },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )

        TextField(
            value = phoneNumber.value,
            onValueChange = { phoneNumber.value = it },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )

        TextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )

        TextField(
            value = confirmPassword.value,
            onValueChange = { confirmPassword.value = it },
            label = { Text("Confirm Password") },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )

        Button(
            onClick = {
                launcher.launch("image/*")
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Select Profile Picture")
        }

        profileImageUri.value?.let { uri ->
            val context = LocalContext.current
            val bitmap = remember(uri) {
                val inputStream = context.contentResolver.openInputStream(uri)
                inputStream?.let {
                    BitmapFactory.decodeStream(it)
                }
            }
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Selected Profile Picture",
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                )
            }
        }

        Button(
            onClick = {
                if (password.value == confirmPassword.value) {
                    auth.createUserWithEmailAndPassword(email.value, password.value)
                        .addOnCompleteListener { task ->
                            println("SignUp OnCompleteListener called")
                            if (task.isSuccessful) {
                                profileImageUri.value?.let { uri ->
                                    val storageRef = FirebaseStorage.getInstance().reference
                                    val profileImagesRef = storageRef.child("profile_images/${auth.currentUser?.uid}.jpg")
                                    profileImagesRef.putFile(uri)
                                        .addOnSuccessListener {
                                            navigateToLogin()
                                        }
                                        .addOnFailureListener {
                                            // TODO: loš upload
                                        }
                                } ?: run {
                                    navigateToLogin()
                                }
                            } else {
                                // TODO: loš signup
                            }
                        }
                } else {
                    // TODO: ne poklapaju se pasvordi
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Sign Up")
        }
    }
}
