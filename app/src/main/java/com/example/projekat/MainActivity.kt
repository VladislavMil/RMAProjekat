package com.example.projekat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.basicapp.components.ScreenA
import com.example.basicapp.components.ScreenB
import com.example.projekat.ui.theme.MapsDemoTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NavApp()
        }
    }
}

enum class Screens() {
    ScreenA,
    ScreenB
}

@Composable
fun NavApp(modifier: Modifier = Modifier.fillMaxSize()) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screens.ScreenA.name) {
        composable(Screens.ScreenA.name) {
            ScreenA(name = "student",
                modifier,
                navigateToB = { navController.navigate(Screens.ScreenB.name)
                })
        }
        composable(Screens.ScreenB.name) {
            ScreenB(list = List(100) { "$it" },
                modifier,
                navigateToA = { navController.popBackStack(Screens.ScreenA.name, false)
                })
        }
    }
}
