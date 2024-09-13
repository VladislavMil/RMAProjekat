package com.example.myapplication.ui.screens

import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import com.example.myapplication.data.firebase.FirebaseAuthManager.saveObjectToFirestore
import com.example.myapplication.data.firebase.models.MarkerData
import com.example.myapplication.data.firebase.models.Review
import com.example.myapplication.ui.components.AddObjectDialog
import com.example.myapplication.ui.components.AddReviewDialog
import com.example.myapplication.ui.components.MarkerDetailsDialog
import com.example.myapplication.ui.components.ShowReviewsDialog
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    userLocation: Location?,
    navigateToProfile: () -> Unit
) {
    val nis = LatLng(43.3209, 21.8958)
    val cameraPositionState = rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(nis, 10f)
    }
    val markers = remember { mutableStateListOf<MarkerData>() }
    val showAddObjectDialog = remember { mutableStateOf(false) }
    val showReviewDialog = remember { mutableStateOf(false) }
    val selectedLatLng = remember { mutableStateOf<LatLng?>(null) }
    val selectedMarkerData = remember { mutableStateOf<MarkerData?>(null) }

    val title = remember { mutableStateOf("") }
    val description = remember { mutableStateOf("") }
    val imageUris = remember { mutableStateListOf<Uri?>(null, null, null) }

    val showAllReviewsDialog = remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var searchResultMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("objects").addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                Log.e("Firestore", "Listen failed.", exception)
                return@addSnapshotListener
            }

            snapshot?.documents?.forEach { document ->
                val markerId = document.id
                val userId = document.getString("userId") ?: return@forEach
                val lat = document.getDouble("location.lat") ?: return@forEach
                val lng = document.getDouble("location.lng") ?: return@forEach
                val location = LatLng(lat, lng)
                val title = document.getString("title") ?: return@forEach
                val description = document.getString("description") ?: return@forEach
                val imageUrls = listOf(
                    document.getString("imageUrl1"),
                    document.getString("imageUrl2"),
                    document.getString("imageUrl3")
                ).filterNotNull()

                val existingIndex = markers.indexOfFirst { it.id == markerId }
                val newMarker = MarkerData(markerId, userId, location, title, description, imageUrls, mutableListOf(), 0.0)
                if (existingIndex >= 0) {
                    markers[existingIndex] = newMarker
                } else {
                    markers.add(newMarker)
                }

                firestore.collection("objects").document(markerId).collection("reviews").addSnapshotListener { reviewsSnapshot, reviewsException ->
                    if (reviewsException != null) {
                        Log.e("Firestore", "Failed to fetch reviews for marker $markerId", reviewsException)
                        return@addSnapshotListener
                    }
                    val reviews = reviewsSnapshot?.toObjects(Review::class.java) ?: listOf()
                    val averageRating = if (reviews.isNotEmpty()) reviews.map { it.rating }.average() else 0.0

                    markers.find { it.id == markerId }?.apply {
                        this.reviews = reviews
                        this.averageRating = averageRating
                    }
                }
            }
        }
    }

    Scaffold() { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).background(Color.Transparent)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Marker by Title") },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val foundMarker = markers.find { it.title.equals(searchQuery, ignoreCase = true) }
                                if (foundMarker != null) {
                                    cameraPositionState.position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(foundMarker.location, 15f)
                                    searchResultMessage = ""
                                } else {
                                    searchResultMessage = "Marker not found"
                                }
                            },
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            val foundMarker = markers.find { it.title.equals(searchQuery, ignoreCase = true) }
                            if (foundMarker != null) {
                                cameraPositionState.position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(foundMarker.location, 15f)
                                searchResultMessage = ""
                            } else {
                                searchResultMessage = "Marker not found"
                            }
                        }
                    ),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.Transparent
                    )
                )
                if (searchResultMessage.isNotEmpty()) {
                    Text(text = searchResultMessage, modifier = Modifier.padding(top = 8.dp))
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        mapType = MapType.NORMAL,
                        isMyLocationEnabled = true,
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true, compassEnabled = true, mapToolbarEnabled = true
                    ),
                    onMapLongClick = { latLng ->
                        title.value = ""
                        description.value = ""
                        imageUris.clear()
                        imageUris.addAll(listOf(null, null, null))

                        selectedLatLng.value = latLng
                        showAddObjectDialog.value = true
                    }
                ) {
                    markers.forEach { markerData ->
                        Marker(
                            state = MarkerState(position = markerData.location),
                            title = markerData.title,
                            snippet = "Tap to view details",
                            onClick = {
                                selectedMarkerData.value = markerData
                                true
                            }
                        )
                    }
                }
            }

            if (showAddObjectDialog.value) {
                AddObjectDialog(
                    onDismiss = { showAddObjectDialog.value = false },
                    onSave = { title, description, imageUrls ->
                        selectedLatLng.value?.let { latLng ->
                            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                            saveObjectToFirestore(
                                title = title,
                                description = description,
                                location = latLng,
                                imageUrls = imageUrls,
                                userId = userId,
                                onComplete = { success, message ->
                                    if (success) {
                                        // Handle success
                                    } else {
                                        // Handle failure
                                    }
                                }
                            )
                        }
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

    Box(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = navigateToProfile,
            modifier = Modifier.padding(16.dp).align(Alignment.BottomStart)
        ) {
            Text("Profile")
        }
    }

    selectedMarkerData.value?.let { markerData ->
        MarkerDetailsDialog(
            markerData = markerData,
            userLocation = userLocation,
            currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
            onDismiss = { selectedMarkerData.value = null },
            onAddReview = { showReviewDialog.value = true },
            onShowReviews = { showAllReviewsDialog.value = true }
        )
    }

    if (showAllReviewsDialog.value && selectedMarkerData.value != null) {
        ShowReviewsDialog(
            reviews = selectedMarkerData.value!!.reviews,
            onDismiss = { showAllReviewsDialog.value = false }
        )
    }

    if (showReviewDialog.value && selectedMarkerData.value != null) {
        AddReviewDialog(
            markerId = selectedMarkerData.value!!.id,
            onDismiss = { showReviewDialog.value = false },
            onReviewAdded = { review ->
                selectedMarkerData.value?.let { marker ->
                    val updatedReviews = marker.reviews.toMutableList().apply { add(review) }
                    val averageRating = updatedReviews.map { it.rating }.average()
                    selectedMarkerData.value = marker.copy(reviews = updatedReviews, averageRating = averageRating)
                }
            }
        )
    }
}



