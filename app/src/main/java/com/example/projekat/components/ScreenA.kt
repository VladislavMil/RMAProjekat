package com.example.basicapp.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ScreenA(name: String, modifier: Modifier = Modifier.fillMaxSize(), navigateToB: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = navigateToB) {
            Text(text = "Go to Screen B")
        }
        Row (modifier.padding(24.dp)) {
            Text("Hello $name", modifier = Modifier.padding(24.dp))
            Text(text = "Welcome to RMAS", modifier = Modifier.padding(24.dp))
        }
    }

}