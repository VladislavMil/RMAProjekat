package com.example.projekat.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.InputStream
import java.util.UUID

fun Uri.toBitmap(context: Context): Bitmap? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(this)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        e.printStackTrace()
        null
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

fun saveObjectToFirestore(description: String, location: LatLng, imageUrl: String?) {
    val firestore = FirebaseFirestore.getInstance()
    val objectData = hashMapOf(
        "description" to description,
        "location" to mapOf("lat" to location.latitude, "lng" to location.longitude),
        "imageUrl" to imageUrl
    )
    firestore.collection("objects")
        .add(objectData)
        .addOnSuccessListener {
            Log.d("Firestore", "Object saved successfully: $objectData")
        }
        .addOnFailureListener { exception ->
            Log.e("Firestore", "Error saving object", exception)
        }
}
