package com.example.myapplication.data.firebase

import android.net.Uri
import android.util.Log
import androidx.navigation.NavController
import com.example.myapplication.Screens
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

object FirebaseAuthManager {

    fun registerUser(email: String, password: String, fullName: String, phoneNumber: String, imageUri: Uri, navController: NavController, onComplete: (Boolean, String) -> Unit) {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).addOnCompleteListener { authTask ->
            if (authTask.isSuccessful) {
                val userId = authTask.result?.user?.uid
                uploadUserImage(userId, imageUri) { imageUrl ->
                    if (imageUrl.isNotEmpty()) {
                        saveUserDetails(userId, fullName, phoneNumber, imageUrl) { success: Boolean ->
                            if (success) {
                                navController.navigate(Screens.SignUp.name)
                                onComplete(true, "Registration Successful")
                            } else {
                                onComplete(false, "Failed to save user details")
                            }
                        }
                    } else {
                        onComplete(false, "Image upload failed")
                    }
                }
            } else {
                onComplete(false, "Registration failed: ${authTask.exception?.localizedMessage}")
            }
        }
    }

    fun uploadUserImage(userId: String?, imageUri: Uri, onComplete: (imageUrl: String) -> Unit) {
        if (userId == null) {
            onComplete("")
            return
        }

        val storageRef = FirebaseStorage.getInstance().getReference("user_images/$userId.jpg")
        storageRef.putFile(imageUri).continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            storageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUrl = task.result.toString()
                onComplete(downloadUrl)
            } else {
                onComplete("")
            }
        }
    }

    private fun saveUserDetails(userId: String?, fullName: String, phoneNumber: String, imageUrl: String, onComplete: (Boolean) -> Unit) {
        val userMap = hashMapOf(
            "fullName" to fullName,
            "phoneNumber" to phoneNumber,
            "imageUrl" to imageUrl
        )
        FirebaseFirestore.getInstance().collection("users").document(userId!!)
            .set(userMap)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener {
                Log.e("Firestore", "Error saving user details: ", it)
                onComplete(false)
            }
    }

    fun loginUser(email: String, password: String, onComplete: (Boolean, String) -> Unit) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, "Login successful")
                } else {
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthInvalidUserException -> "Email does not exist"
                        is FirebaseAuthInvalidCredentialsException -> "Incorrect password"
                        else -> "Login failed: ${task.exception?.message}"
                    }
                    onComplete(false, errorMessage)
                    Log.e("Login", errorMessage)
                }
            }
    }

    fun saveObjectToFirestore(title: String, description: String, location: LatLng, imageUrls: List<String?>
    ) {
        val firestore = FirebaseFirestore.getInstance()
        val objectData = hashMapOf(
            "title" to title,
            "description" to description,
            "location" to hashMapOf(
                "lat" to location.latitude,
                "lng" to location.longitude
            ),
            "imageUrl1" to imageUrls.getOrNull(0),
            "imageUrl2" to imageUrls.getOrNull(1),
            "imageUrl3" to imageUrls.getOrNull(2)
        )

        firestore.collection("objects")
            .add(objectData)
            .addOnSuccessListener { documentReference ->
                // Handle success
            }
            .addOnFailureListener { e ->
                // Handle failure
            }
    }

    fun uploadImageToFirebase(uri: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("images/${UUID.randomUUID()}")
        Log.d("Firebase Storage", "Starting image upload: $uri")

        imageRef.putFile(uri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    Log.d("Firebase Storage", "Image uploaded successfully: $downloadUrl")
                    onSuccess(downloadUrl.toString())
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase Storage", "Error uploading image", exception)
                onFailure(exception)
            }
    }
}
