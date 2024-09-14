package com.example.myapplication.data.firebase

import android.net.Uri
import android.util.Log
import androidx.navigation.NavController
import com.example.myapplication.Screens
import com.example.myapplication.data.firebase.models.CustomLatLng
import com.example.myapplication.data.firebase.models.MarkerData
import com.example.myapplication.data.firebase.models.Review
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

object FirebaseAuthManager {

    fun registerUser(
        email: String,
        password: String,
        fullName: String,
        phoneNumber: String,
        imageUri: Uri,
        username: String,
        navController: NavController,
        onComplete: (Boolean, String) -> Unit
    ) {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).addOnCompleteListener { authTask ->
            if (authTask.isSuccessful) {
                val userId = authTask.result?.user?.uid
                uploadUserImage(userId, imageUri) { imageUrl ->
                    if (imageUrl.isNotEmpty()) {
                        saveUserDetails(userId, fullName, phoneNumber, username, imageUrl) { success ->
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

    private fun saveUserDetails(
        userId: String?,
        fullName: String,
        phoneNumber: String,
        username: String,
        imageUrl: String,
        onComplete: (Boolean) -> Unit
    ) {
        val userMap = hashMapOf(
            "fullName" to fullName,
            "phoneNumber" to phoneNumber,
            "username" to username,
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

    fun saveObjectToFirestore(
        title: String,
        description: String,
        location: LatLng,
        imageUrls: List<String>,
        userId: String,
        onComplete: (Boolean, String, MarkerData?) -> Unit
    ) {
        val firestore = FirebaseFirestore.getInstance()
        val newMarker = MarkerData(
            id = firestore.collection("objects").document().id,
            userId = userId,
            location = CustomLatLng(location.latitude, location.longitude),
            title = title,
            description = description,
            imageUrls = imageUrls,
            reviews = mutableListOf(),
            averageRating = 0.0f,
            points = 10
        )

        firestore.collection("objects").document(newMarker.id)
            .set(newMarker)
            .addOnSuccessListener {
                updateUserPoints(userId, 10)
                onComplete(true, "Marker added successfully", newMarker)
            }
            .addOnFailureListener { e ->
                onComplete(false, e.message ?: "Error adding marker", null)
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

    fun addReviewToFirestore(markerId: String, rating: Int, comment: String, onComplete: (Boolean, String) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            onComplete(false, "User not authenticated")
            return
        }

        val review = Review(userId, rating, comment, points = 2)
        FirebaseFirestore.getInstance()
            .collection("markers")
            .document(markerId)
            .collection("reviews")
            .add(review)
            .addOnSuccessListener {
                updateUserPoints(userId, 2)
                onComplete(true, "Review successfully added")
            }
            .addOnFailureListener { e ->
                onComplete(false, "Error adding review: ${e.message}")
            }
    }

    fun updateUserPoints(userId: String, points: Int) {
        val userRef = FirebaseFirestore.getInstance().collection("users").document(userId)
        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val currentPoints = document.getLong("points") ?: 0
                userRef.update("points", currentPoints + points)
            }
        }
    }
}
