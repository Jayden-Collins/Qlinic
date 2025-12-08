package com.example.qlinic.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.qlinic.ui.screen.ReportScreen

// Define Routes
object Routes {
    const val HOME = "home"
    const val REPORT = "report"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.REPORT) {

        composable(Routes.REPORT) {
            ReportScreen(onNavigateHome = { navController.navigate(Routes.HOME) })
        }


    }
}