package com.example.myapplication.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.example.myapplication.data.firebase.FirebaseAuthManager.uploadImageToFirebase
import com.example.myapplication.data.firebase.models.MarkerData
import kotlin.math.roundToInt

@Composable
fun AddObjectDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, imageUrls: List<String?>) -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    imageUris: List<Uri?>,
    onImageUriChange: (List<Uri?>) -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri>? ->
        onImageUriChange(uris?.take(3) ?: listOf())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Marker") },
        text = {
            Column {
                TextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                TextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                Button(
                    onClick = { launcher.launch("image/*") },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Upload Images")
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    imageUris.forEach { uri ->
                        uri?.let {
                            val bitmap = it.toBitmap(context)
                            bitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(100.dp).padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val imageUrls = mutableListOf<String?>()
                imageUris.forEach { uri ->
                    uri?.let {
                        uploadImageToFirebase(it, onSuccess = { url ->
                            imageUrls.add(url)
                            if (imageUrls.size == imageUris.size) {
                                onSave(title, description, imageUrls)
                                onDismiss()
                            }
                        }, onFailure = { error ->
                            Log.e("Firebase", "Image upload failed: $error")
                        })
                    }
                }
            }) {
                Text("Save")
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
fun MarkerDetailsDialog(
    markerData: MarkerData,
    userLocation: Location?,
    currentUserId: String,
    onDismiss: () -> Unit,
    onAddReview: () -> Unit,
    onShowReviews: () -> Unit
) {
    val distance = userLocation?.let {
        val markerLocation = Location("").apply {
            latitude = markerData.location.latitude
            longitude = markerData.location.longitude
        }
        it.distanceTo(markerLocation)
    }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(markerData.title) },
        text = {
            Column {
                Text("Description: ${markerData.description}", style = MaterialTheme.typography.bodyLarge)

                distance?.let {
                    Text("Distance: ${it.roundToInt()} meters", style = MaterialTheme.typography.bodyMedium)
                }

                Text("Average Rating: ${String.format("%.1f", markerData.averageRating)}", style = MaterialTheme.typography.bodyMedium)

                LazyColumn {
                    items(markerData.imageUrls) { imageUrl ->
                        imageUrl?.let {
                            Image(
                                painter = rememberImagePainter(it),
                                contentDescription = "Marker Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            Row {
                if (currentUserId != markerData.userId) {
                    Button(onClick = onAddReview) {
                        Text("Add Review")
                    }
                }
                Button(onClick = onShowReviews) {
                    Text("Show Reviews")
                }
            }
        }
    )
}

fun Uri.toBitmap(context: Context): Bitmap? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, this))
    } else {
        MediaStore.Images.Media.getBitmap(context.contentResolver, this)
    }
}