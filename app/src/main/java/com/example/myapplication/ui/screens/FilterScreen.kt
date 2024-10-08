package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.firebase.models.MarkerData
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun FilterScreen(userLocation: LatLng) {

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var selectedDistance by remember { mutableFloatStateOf(100f) }
    val distances = listOf(100f, 500f, 1000f, 5000f, 20000f)
    val distanceLabels = listOf("100m", "500m", "1km", "5km", "20km")
    val markers = remember { mutableStateListOf<MarkerData>() }
    val filteredMarkers = remember { mutableStateListOf<MarkerData>() }

    LaunchedEffect(Unit) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("objects")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    try {
                        val marker = document.toObject<MarkerData>()
                        markers.add(marker)
                        println("Fetched marker: ${marker.title}")
                    } catch (e: Exception) {
                        println("Error converting document: ${document.id}, ${e.message}")
                    }
                }
                println("Total markers fetched: ${markers.size}")
            }
            .addOnFailureListener { e ->
                println("Error fetching documents: ${e.message}")
            }
    }

    LaunchedEffect(selectedDistance) {
        filteredMarkers.clear()
        markers.forEach { marker ->
            val distance = calculateDistance(userLocation, marker.location.toLatLng())
            println("Distance to marker ${marker.title}: $distance")
            if (distance <= selectedDistance) {
                filteredMarkers.add(marker)
            }
        }
        println("Filtered markers count: ${filteredMarkers.size}")
    }

    Scaffold {
        Column(modifier = Modifier
            .padding(it)
            .padding(16.dp)) {
            Text(
                "Select the distance to filter markers",
                style = MaterialTheme.typography.titleLarge
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(distanceLabels[sliderPosition.toInt()])
                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    valueRange = 0f..4f,
                    steps = 3,
                    modifier = Modifier.weight(1f)
                )
                Text(distanceLabels.last())
            }
            Button(
                onClick = { selectedDistance = distances[sliderPosition.toInt()] },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 16.dp)
            ) {
                Text("Show Markers")
            }
            Button(
                onClick = { filteredMarkers.sortByDescending { it.averageRating } },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
            ) {
                Text("Sort by Rating")
            }
            Spacer(modifier = Modifier.padding(vertical = 16.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredMarkers) { marker ->
                    FilteredMarkerItem(marker)
                }
            }
        }
    }
}

@Composable
fun FilteredMarkerItem(marker: MarkerData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = marker.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Rating: ${marker.averageRating}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

fun calculateDistance(loc1: LatLng, loc2: LatLng): Double {
    val earthRadius = 6371000
    val dLat = Math.toRadians(loc2.latitude - loc1.latitude)
    val dLng = Math.toRadians(loc2.longitude - loc1.longitude)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(loc1.latitude)) * cos(Math.toRadians(loc2.latitude)) *
            sin(dLng / 2) * sin(dLng / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadius * c
}