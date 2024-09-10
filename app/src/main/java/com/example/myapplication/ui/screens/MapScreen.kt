package com.example.myapplication.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.ui.graphics.asImageBitmap
import com.example.myapplication.data.firebase.FirebaseAuthManager.saveObjectToFirestore
import com.example.myapplication.data.firebase.FirebaseAuthManager.uploadImageToFirebase
import coil.compose.rememberImagePainter
import com.example.myapplication.data.firebase.models.MarkerData

@Composable
fun MapScreen(modifier: Modifier = Modifier, navigateToProfile: () -> Unit) {
    val nis = LatLng(43.3209, 21.8958)
    val cameraPositionState = rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(nis, 10f)
    }
    val markers = remember { mutableStateListOf<MarkerData>() }
    val showAddObjectDialog = remember { mutableStateOf(false) }
    val selectedLatLng = remember { mutableStateOf<LatLng?>(null) }
    val selectedMarkerData = remember { mutableStateOf<MarkerData?>(null) }
    val title = remember { mutableStateOf("") }
    val description = remember { mutableStateOf("") }
    val imageUris = remember { mutableStateListOf<Uri?>(null, null, null) }

    LaunchedEffect(Unit) {
        val firestore = FirebaseFirestore.getInstance()
        val subscription =
            firestore.collection("objects").addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("Firestore", "Listen failed.", exception)
                    return@addSnapshotListener
                }

                snapshot?.let {
                    markers.clear()
                    for (document in it.documents) {
                        val lat = document.getDouble("location.lat") ?: 0.0
                        val lng = document.getDouble("location.lng") ?: 0.0
                        val title = document.getString("title") ?: ""
                        val description = document.getString("description") ?: ""
                        val imageUrls = listOf(
                            document.getString("imageUrl1"),
                            document.getString("imageUrl2"),
                            document.getString("imageUrl3")
                        )
                        val location = LatLng(lat, lng)
                        markers.add(MarkerData(location, title, description, imageUrls))
                    }
                }
            }
    }

    Scaffold(topBar = {
    }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            GoogleMap(modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapType = MapType.NORMAL,
                    isMyLocationEnabled = true,
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true, compassEnabled = true, mapToolbarEnabled = true
                ),
                onMapLongClick = { latLng ->
                    selectedLatLng.value = latLng
                    showAddObjectDialog.value = true
                }) {
                markers.forEach { markerData ->
                    Marker(state = MarkerState(position = markerData.location),
                        title = markerData.title,
                        snippet = "Tap to view details",
                        onClick = {
                            selectedMarkerData.value = markerData
                            true
                        })
                }
            }

            if (showAddObjectDialog.value) {
                AddObjectDialog(
                    onDismiss = { showAddObjectDialog.value = false },
                    onSave = { title, description, imageUrls ->
                        selectedLatLng.value?.let { latLng ->
                            saveObjectToFirestore(title, description, latLng, imageUrls)
                            markers.add(MarkerData(latLng, title, description, imageUrls))
                        }
                        showAddObjectDialog.value = false
                    },
                    title = title.value,
                    onTitleChange = { title.value = it },
                    description = description.value,
                    onDescriptionChange = { description.value = it },
                    imageUris = imageUris,
                    onImageUriChange = { uris ->
                        imageUris.clear()
                        imageUris.addAll(uris)
                    }
                )
            }
        }
    }

    Button(
        onClick = navigateToProfile, modifier = Modifier.padding(16.dp)
    ) {
        Text("Profile")
    }

    // Show the dialog for the selected marker
    selectedMarkerData.value?.let { markerData ->
        MarkerDetailsDialog(markerData = markerData,
            onDismiss = { selectedMarkerData.value = null })
    }
}

@Composable
fun AddObjectDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, imageUrls: List<String?>) -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    imageUris: List<Uri?>,
    onImageUriChange: (List<Uri?>) -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri>? ->
        onImageUriChange(uris?.take(3) ?: listOf())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Marker") },
        text = {
            Column {
                TextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                TextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                Button(
                    onClick = { launcher.launch("image/*") },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Upload Images")
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    imageUris.forEach { uri ->
                        uri?.let {
                            val bitmap = it.toBitmap(context)?.asImageBitmap()
                            bitmap?.let { img: ImageBitmap ->
                                Image(
                                    bitmap = img,
                                    contentDescription = "Selected Image",
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val imageUrls = mutableListOf<String?>()
                imageUris.forEach { uri ->
                    if (uri != null) {
                        uploadImageToFirebase(uri, { imageUrl ->
                            imageUrls.add(imageUrl)
                            if (imageUrls.size == imageUris.size) {
                                onSave(title, description, imageUrls)
                            }
                        }, { exception ->
                            Log.e("Firebase Storage", "Error uploading image", exception)
                            onSave(title, description, imageUrls)
                        })
                    } else {
                        imageUrls.add(null)
                    }
                }
                if (imageUrls.size == imageUris.size) {
                    onSave(title, description, imageUrls)
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

@Composable
fun MarkerDetailsDialog(markerData: MarkerData, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(markerData.title) },
        text = {
            Column {
                Text("Description: ${markerData.description}", style = MaterialTheme.typography.bodyLarge)
                LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    items(markerData.imageUrls) { imageUrl ->
                        imageUrl?.let {
                            Image(
                                painter = rememberImagePainter(it),
                                contentDescription = "Marker Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .aspectRatio(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

fun Uri.toBitmap(context: Context): Bitmap? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, this))
    } else {
        MediaStore.Images.Media.getBitmap(context.contentResolver, this)
    }
}