package com.example.myapplication.ui.screens

import android.graphics.BitmapFactory
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import com.example.myapplication.R
import com.example.myapplication.data.firebase.FirebaseAuthManager.saveObjectToFirestore
import com.example.myapplication.data.firebase.models.MarkerData
import com.example.myapplication.ui.components.AddObjectDialog
import com.example.myapplication.ui.components.AddReviewDialog
import com.example.myapplication.ui.components.MarkerDetailsDialog
import com.example.myapplication.ui.components.ShowReviewsDialog
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    userLocation: Location?,
    navigateToProfile: () -> Unit,
    navigateToFilter: () -> Unit
) {
    val context = LocalContext.current
    val nis = LatLng(43.3209, 21.8958)
    val cameraPositionState = rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(nis, 10f)
    }
    val markers = remember { mutableStateListOf<MarkerData>() }
    val showAddObjectDialog = remember { mutableStateOf(false) }
    val showReviewDialog = remember { mutableStateOf(false) }
    val selectedLatLng = remember { mutableStateOf<LatLng?>(null) }
    val selectedMarkerData = remember { mutableStateOf<MarkerData?>(null) }
    val showLeaderboardDialog = remember { mutableStateOf(false) }
    val title = remember { mutableStateOf("") }
    val description = remember { mutableStateOf("") }
    val imageUris = remember { mutableStateListOf<Uri?>(null, null, null) }

    val showAllReviewsDialog = remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var searchResultMessage by remember { mutableStateOf("") }

    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.tent)
    val markerIcon = BitmapDescriptorFactory.fromBitmap(bitmap)

    val hasReviewed = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("objects").get().addOnSuccessListener { snapshot ->
            markers.clear()
            for (document in snapshot.documents) {
                val markerData = document.toObject(MarkerData::class.java)
                markerData?.let { markers.add(it) }
            }
        }.addOnFailureListener { exception ->
            Log.e("Firestore", "Error fetching markers: ${exception.localizedMessage}")
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
                                    cameraPositionState.position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(foundMarker.location.toLatLng(), 15f)
                                    searchResultMessage = ""
                                } else {
                                    searchResultMessage = "Marker not found"
                                }
                            }
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
                                cameraPositionState.position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(foundMarker.location.toLatLng(), 15f)
                                searchResultMessage = ""
                            } else {
                                searchResultMessage = "Marker not found"
                            }
                        }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
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
                            state = MarkerState(position = markerData.location.toLatLng()),
                            title = markerData.title,
                            snippet = "Tap to view details",
                            icon = markerIcon,
                            onClick = {
                                selectedMarkerData.value = markerData
                                hasReviewed.value = markerData.reviews.any { it.userId == FirebaseAuth.getInstance().currentUser?.uid }
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
                            val nonNullImageUrls = imageUrls.filterNotNull()
                            saveObjectToFirestore(
                                title, description, latLng, nonNullImageUrls, userId
                            ) { success, message, newMarker ->
                                if (success) {
                                    markers.add(newMarker!!)
                                    showAddObjectDialog.value = false
                                } else {
                                    Log.e("Firestore", message)
                                }
                            }
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

            if (showReviewDialog.value && selectedMarkerData.value != null) {
                AddReviewDialog(
                    markerId = selectedMarkerData.value!!.id,
                    onDismiss = { showReviewDialog.value = false },
                    onReviewAdded = { review ->
                        selectedMarkerData.value?.let { marker ->
                            val updatedReviews = marker.reviews.toMutableList().apply { add(review) }
                            val averageRating = updatedReviews.map { it.rating }.average()
                            selectedMarkerData.value = marker.copy(
                                reviews = updatedReviews,
                                averageRating = averageRating.toFloat()
                            )
                            hasReviewed.value = true
                        }
                    }
                )
            }

            if (showAllReviewsDialog.value && selectedMarkerData.value != null) {
                ShowReviewsDialog(
                    reviews = selectedMarkerData.value!!.reviews,
                    onDismiss = { showAllReviewsDialog.value = false }
                )
            }

            selectedMarkerData.value?.let { markerData ->
                MarkerDetailsDialog(
                    markerData = markerData,
                    userLocation = userLocation,
                    currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                    onDismiss = { selectedMarkerData.value = null },
                    onAddReview = { showReviewDialog.value = true },
                    onShowReviews = { showAllReviewsDialog.value = true },
                    hasReviewed = hasReviewed
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
        Button(
            onClick = navigateToFilter,
            modifier = Modifier.padding(16.dp).align(Alignment.BottomCenter)
        ) {
            Text("Filter")
        }
        Button(
            onClick = { showLeaderboardDialog.value = true },
            modifier = Modifier.padding(16.dp).align(Alignment.BottomEnd)
        ) {
            Text("Leaderboard")
        }
    }

    if (showLeaderboardDialog.value) {
        LeaderboardDialog(onDismiss = { showLeaderboardDialog.value = false })
    }
}

@Composable
fun LeaderboardDialog(onDismiss: () -> Unit) {
    val users = remember { mutableStateListOf<Pair<String, Long>>() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users").orderBy("points", Query.Direction.DESCENDING).get()
            .addOnSuccessListener { snapshot ->
                users.clear()
                for (document in snapshot.documents) {
                    val username = document.getString("username") ?: "Unknown"
                    val points = document.getLong("points") ?: 0
                    users.add(username to points)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error fetching users: ${exception.localizedMessage}")
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Leaderboard") },
        text = {
            Column {
                users.forEach { (username, points) ->
                    Text("$username: $points points")
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



