package com.example.myapplication.data.firebase.models

data class Review(
    val userId: String = "",
    val rating: Int = 0,
    val review: String = "",
    val points: Int = 0
)
