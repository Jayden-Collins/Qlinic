package com.example.qlinic.ui.navigation

import MainAppScaffold
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.qlinic.ui.screen.HomeScreen
import com.example.qlinic.ui.screen.Profile
import com.example.qlinic.ui.screen.ReportScreen
import com.example.qlinic.ui.screen.Schedule

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            MainAppScaffold(navController = navController) {paddingValues ->
                HomeScreen(paddingValues = paddingValues,)
            }
        }

        composable(Screen.Schedule.route) {
            MainAppScaffold(navController = navController) {paddingValues ->
                Schedule(paddingValues = paddingValues,)
            }
        }

        composable(Screen.Report.route) {
            MainAppScaffold(navController = navController) {paddingValues ->
                ReportScreen(paddingValues = paddingValues,)
            }
        }

        composable(Screen.Profile.route) {
            MainAppScaffold(navController = navController) {paddingValues ->
                Profile(paddingValues = paddingValues,)
            }
        }
    }
}
