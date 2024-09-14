package com.example.myapplication.data.firebase.models

import com.google.android.gms.maps.model.LatLng

data class MarkerData(
    val id: String = "",
    val userId: String = "",
    val location: CustomLatLng = CustomLatLng(),
    val title: String = "",
    val description: String = "",
    val imageUrls: List<String> = emptyList(),
    var reviews: List<Review> = emptyList(),
    var averageRating: Float = 0.0f
)
