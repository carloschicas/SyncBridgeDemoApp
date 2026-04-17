package com.syncbridge.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.syncbridge.demo.presentation.OrderViewModel
import com.syncbridge.demo.presentation.create_order.CreateOrderScreen
import com.syncbridge.demo.presentation.dashboard.DashboardScreen
import com.syncbridge.demo.presentation.dashboard.DashboardViewModel
import com.syncbridge.demo.ui.theme.SyncbridgedemoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SyncbridgedemoTheme {
                val navController = rememberNavController()
                val dashboardViewModel: DashboardViewModel = hiltViewModel()
                val orderViewModel: OrderViewModel = hiltViewModel()

                NavHost(navController = navController, startDestination = "dashboard") {
                    composable("dashboard") {
                        DashboardScreen(
                            viewModel = dashboardViewModel,
                            onNavigateToCreateOrder = { navController.navigate("create_order") }
                        )
                    }
                    composable("create_order") {
                        CreateOrderScreen(
                            viewModel = orderViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
