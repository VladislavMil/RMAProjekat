package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
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
import android.location.Location
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.core.app.ActivityCompat
import com.example.myapplication.ui.screens.FilterScreen
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.MapsInitializer

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLocation by mutableStateOf<Location?>(null)
    private var isLocationPermissionGranted by mutableStateOf(false)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            isLocationPermissionGranted = isGranted
            if (isGranted) {
                getUserLocation()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapsInitializer.initialize(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        isLocationPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (isLocationPermissionGranted) {
            getUserLocation()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        setContent {
            NavApp(isLocationPermissionGranted, userLocation)
        }
    }

    private fun getUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            userLocation = location
        }
    }
}

@Composable
fun NavApp(
    isLocationPermissionGranted: Boolean,
    userLocation: Location?,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screens.Login.name) {
        composable(Screens.Login.name) {
            LoginScreen(
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
                userLocation = userLocation,
                navigateToProfile = { navController.navigate(Screens.Profile.name) },
                navigateToFilter = { navController.navigate(Screens.Filter.name) }
            )
        }
        composable(Screens.Profile.name) {
            ProfileScreen(onBackPress = { navController.popBackStack() })
        }
        composable(Screens.Filter.name) {
            userLocation?.let {
                FilterScreen(
                    userLocation = com.google.android.gms.maps.model.LatLng(
                        it.latitude,
                        it.longitude
                    )
                )
            }
        }
    }
}

enum class Screens {
    Login,
    SignUp,
    Map,
    Profile,
    Filter
}
