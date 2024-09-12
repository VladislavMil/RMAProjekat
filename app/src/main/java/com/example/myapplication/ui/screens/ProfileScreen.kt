package com.example.myapplication.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onBackPress: () -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var username by remember { mutableStateOf(TextFieldValue("")) }
    var phoneNumber by remember { mutableStateOf(TextFieldValue("")) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(user) {
        user?.let {
            db.collection("users").document(it.uid).get()
                .addOnSuccessListener { document ->
                    username = TextFieldValue(document.getString("username") ?: "")
                    phoneNumber = TextFieldValue(document.getString("phoneNumber") ?: "")
                    isLoading = false
                }
                .addOnFailureListener { exception ->
                    Log.e("ProfileScreen", "Error fetching profile data", exception)
                    isLoading = false
                }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Button(onClick = onBackPress) {
                        Text("Back")
                    }
                }

                TextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                TextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                Button(
                    onClick = {
                        user?.let { currentUser ->
                            val updatedData = hashMapOf(
                                "username" to username.text,
                                "phoneNumber" to phoneNumber.text
                            )
                            Firebase.firestore.collection("users")
                                .document(currentUser.uid)
                                .update(updatedData as Map<String, Any>)
                                .addOnSuccessListener {
                                    Log.d("ProfileScreen", "Profile updated successfully")
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("ProfileScreen", "Error updating profile", exception)
                                }
                        }
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Save Changes")
                }
            }
        }
    }
}