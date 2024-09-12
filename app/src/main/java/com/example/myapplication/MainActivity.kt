package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.myapplication.ui.screens.LoginScreen
import com.example.myapplication.ui.screens.MapScreen
import com.example.myapplication.ui.screens.ProfileScreen
import com.example.myapplication.ui.screens.RegistrationScreen

class MainActivity : ComponentActivity() {
    private var isLocationPermissionGranted by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean -> isLocationPermissionGranted = isGranted
        if (isGranted) {

        } else {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isLocationPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!isLocationPermissionGranted) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        setContent {
            NavApp(isLocationPermissionGranted)
        }
    }
}

@Composable
fun NavApp(isLocationPermissionGranted: Boolean, modifier: Modifier = Modifier.fillMaxSize()) {
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
            RegistrationScreen(
                modifier,
                navigateToLogin = { navController.navigate(Screens.Login.name) },
                navController
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
                onBackPress = { navController.popBackStack() }
            )
        }
    }
}

enum class Screens {
    Login,
    SignUp,
    Map,
    Profile
}
