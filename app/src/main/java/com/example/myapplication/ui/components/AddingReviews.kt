package com.example.myapplication.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.myapplication.data.firebase.FirebaseAuthManager
import com.example.myapplication.data.firebase.models.Review
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AddReviewDialog(markerId: String, onDismiss: () -> Unit, onReviewAdded: (Review) -> Unit) {
    var rating by remember { mutableStateOf(0f) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Review") },
        text = {
            Column {
                Text("Rating")
                Slider(
                    value = rating,
                    onValueChange = { rating = it },
                    valueRange = 0f..5f,
                    steps = 4
                )
                TextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comment") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val review = Review(FirebaseAuth.getInstance().currentUser?.uid ?: "", rating.toInt(), comment)
                FirebaseAuthManager.addReviewToFirestore(markerId, rating.toInt(), comment) { success, message ->
                    if (success) {
                        onReviewAdded(review)
                        onDismiss()
                    } else {
                        Log.e("Review", message)
                    }
                }
            }) {
                Text("Submit Review")
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
fun ShowReviewsDialog(
    reviews: List<Review>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reviews") },
        text = {
            LazyColumn {
                items(reviews) { review ->
                    Text("Rating: ${review.rating} - ${review.review}")
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