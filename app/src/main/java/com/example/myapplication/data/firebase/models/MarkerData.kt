package com.example.myapplication.data.firebase.models

import com.google.android.gms.maps.model.LatLng

data class MarkerData(
    val id: String,
    val userId: String,
    val location: LatLng,
    val title: String,
    val description: String,
    val imageUrls: List<String?> = listOf(),
    var reviews: List<Review> = listOf(),
    var averageRating: Any? = null
)
