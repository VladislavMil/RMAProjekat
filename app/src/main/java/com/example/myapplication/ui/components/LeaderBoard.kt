package com.example.myapplication.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@Composable
fun LeaderboardDialog(onDismiss: () -> Unit) {
    val users = remember { mutableStateListOf<Pair<String, Long>>() }

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