package com.example.projekat.components

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.UUID

@Composable
fun MapScreen(modifier: Modifier = Modifier, navigateToProfile: () -> Unit) {
    val nis = LatLng(43.3209, 21.8958)
    val cameraPositionState = rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(nis, 10f)
    }

    val markers = remember { mutableStateListOf<MarkerData>() }
    val showAddObjectDialog = remember { mutableStateOf(false) }
    val selectedLatLng = remember { mutableStateOf<LatLng?>(null) }
    val objectDescription = remember { mutableStateOf("") }
    val imageUri = remember { mutableStateOf<Uri?>(null) }

    // Fetch markers from Firestore when the screen is opened
    LaunchedEffect(Unit) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("objects").get()
            .addOnSuccessListener { result ->
                markers.clear()
                for (document in result) {
                    val lat = document.getDouble("location.lat") ?: 0.0
                    val lng = document.getDouble("location.lng") ?: 0.0
                    val description = document.getString("description") ?: ""
                    val imageUrl = document.getString("imageUrl")
                    val location = LatLng(lat, lng)
                    markers.add(MarkerData(location, description, imageUrl))
                    Log.d("Firestore", "Fetched Marker: $description, $location, $imageUrl")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error fetching markers", exception)
            }
    }

    Scaffold(
        topBar = {
            // TopBar for navigation to profile can be added here
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapType = MapType.NORMAL,
                    isMyLocationEnabled = true,
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    compassEnabled = true,
                    mapToolbarEnabled = true
                ),
                onMapLongClick = { latLng ->
                    selectedLatLng.value = latLng
                    showAddObjectDialog.value = true
                }
            ) {
                markers.forEach { markerData ->
                    Marker(
                        state = MarkerState(position = markerData.location),
                        title = markerData.description,
                        snippet = "Tap to view details"
                    )
                }
            }

            if (showAddObjectDialog.value) {
                AddObjectDialog(
                    onDismiss = { showAddObjectDialog.value = false },
                    onSave = { description, imageUrl ->
                        selectedLatLng.value?.let { latLng ->
                            // Save marker with image URL to Firestore
                            saveObjectToFirestore(description, latLng, imageUrl)
                            markers.add(MarkerData(latLng, description, imageUrl))
                        }
                        showAddObjectDialog.value = false
                    },
                    description = objectDescription.value,
                    onDescriptionChange = { objectDescription.value = it },
                    imageUri = imageUri.value,
                    onImageUriChange = { imageUri.value = it }
                )
            }
        }
    }
}



@Composable
fun AddObjectDialog(
    onDismiss: () -> Unit,
    onSave: (description: String, imageUrl: String?) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    imageUri: Uri?,
    onImageUriChange: (Uri?) -> Unit
) {
    val context = LocalContext.current // Access the current context
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Object") },
        text = {
            Column {
                TextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                imageUri?.let {
                    val bitmap = it.toBitmap(context)?.asImageBitmap()
                    bitmap?.let { img ->
                        Image(
                            bitmap = img,
                            contentDescription = "Selected Image",
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (imageUri != null) {
                    println("Upload Vladislav")
                    uploadImageToFirebase(imageUri, { imageUrl ->
                        onSave(description, imageUrl)
                    }, { exception ->
                        // Handle error during image upload
                    })
                } else {
                    onSave(description, null)
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Data class for marker information
data class MarkerData(
    val location: LatLng,
    val description: String,
    val imageUrl: String?
)


