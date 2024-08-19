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
import com.example.projekat.components.LoginScreen
import com.example.projekat.components.MapScreen
import com.example.projekat.components.ProfileScreen
import com.example.projekat.components.SignUpScreen
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
    Login,
    Map,
    Profile,
    SignUp
}

@Composable
fun NavApp(modifier: Modifier = Modifier.fillMaxSize()) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screens.Login.name) {
        composable(Screens.Login.name) {
            LoginScreen(
                modifier,
                navigateToMap = { navController.navigate(Screens.Map.name) },
                navigateToSignUp = { navController.navigate(Screens.SignUp.name) }
            )
        }
        composable(Screens.SignUp.name) {
            SignUpScreen(
                modifier,
                navigateToLogin = { navController.navigate(Screens.Login.name) }
            )
        }
        composable(Screens.Map.name) {
            MapScreen(
                modifier,
                navigateToProfile = { navController.navigate(Screens.Profile.name) }
            )
        }
        composable(Screens.Profile.name) {
            ProfileScreen(
                modifier,
                navigateToMap = { navController.popBackStack(Screens.Map.name, false) }
            )
        }
    }
}
