package com.example.myapplication.data.firebase.models

import com.google.android.gms.maps.model.LatLng

data class CustomLatLng(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) {
    fun toLatLng(): LatLng {
        return LatLng(latitude, longitude)
    }
}
