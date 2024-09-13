package com.example.myapplication.data.firebase.models

import com.google.android.gms.maps.model.LatLng

data class MarkerData(
    val id: String = "",
    val userId: String = "",
    val location: LatLng = LatLng(0.0, 0.0),
    val title: String = "",
    val description: String = "",
    val imageUrls: List<String?> = listOf(),
    var reviews: List<Review> = listOf(),
    var averageRating: Float = 0.0f
)
