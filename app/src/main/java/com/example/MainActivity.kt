package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.TrendsScreen
import androidx.compose.material.icons.filled.TrendingUp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.NutriScanViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: NutriScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppHost(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppHost(viewModel: NutriScanViewModel) {
    val activeRoute by viewModel.currentRoute.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Keep bottom navigation hidden during camera scanner view to let vision HUD overlay remain full screen
            if (activeRoute != "scanner") {
                NavigationBar(
                    modifier = Modifier.testTag("main_navigation_bar")
                ) {
                    NavigationBarItem(
                        selected = activeRoute == "dashboard",
                        onClick = { viewModel.navigateTo("dashboard") },
                        label = { Text("Home") },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home dashboard") }
                    )
                    NavigationBarItem(
                        selected = activeRoute == "trends",
                        onClick = { viewModel.navigateTo("trends") },
                        label = { Text("Trends") },
                        icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Biometric trends") }
                    )
                    NavigationBarItem(
                        selected = activeRoute == "scanner",
                        onClick = { viewModel.navigateTo("scanner") },
                        label = { Text("Scan Tool") },
                        icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Scan food") }
                    )
                    NavigationBarItem(
                        selected = activeRoute == "history",
                        onClick = { viewModel.navigateTo("history") },
                        label = { Text("Diary") },
                        icon = { Icon(Icons.Default.History, contentDescription = "Meal diary logs") }
                    )
                    NavigationBarItem(
                        selected = activeRoute == "profile",
                        onClick = { viewModel.navigateTo("profile") },
                        label = { Text("Coach") },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile calculations") }
                    )
                }
            }
        }
    ) { innerPadding ->
        // Animated transition between screens mimics high-tech Apple feel
        AnimatedContent(
            targetState = activeRoute,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "route_transition",
            modifier = Modifier.padding(innerPadding)
        ) { route ->
            when (route) {
                "dashboard" -> DashboardScreen(viewModel = viewModel)
                "trends" -> TrendsScreen(viewModel = viewModel)
                "scanner" -> ScannerScreen(viewModel = viewModel)
                "history" -> HistoryScreen(viewModel = viewModel)
                "profile" -> ProfileScreen(viewModel = viewModel)
                else -> DashboardScreen(viewModel = viewModel)
            }
        }
    }
}
