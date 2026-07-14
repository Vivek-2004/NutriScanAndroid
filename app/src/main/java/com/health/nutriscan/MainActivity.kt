package com.health.nutriscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.health.nutriscan.ui.screens.*
import com.health.nutriscan.ui.viewmodel.NutriCheckViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val coreViewModel: NutriCheckViewModel = viewModel()

                    NavHost(navController = navController, startDestination = "dashboard") {
                        composable("dashboard") {
                            DashboardScreen(coreViewModel) {
                                navController.navigate("scan")
                            }
                        }
                        composable("scan") {
                            ScanScreen(coreViewModel) {
                                navController.navigate("result")
                            }
                        }
                        composable("result") {
                            ResultScreen(coreViewModel) {
                                navController.popBackStack("dashboard", inclusive = false)
                            }
                        }
                    }
                }
            }
        }
    }
}