package com.example.qlinic.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.qlinic.data.model.SessionManager

@Composable
fun MainAppScaffold(
    navController: NavController,
    screenTitle: String = "",
    content: @Composable (PaddingValues) -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val onNavigateHome: () -> Unit =
        { navController.navigate(Routes.Home.route) { launchSingleTop = true } }
    val onNavigateToSchedule: () -> Unit =
        { navController.navigate(Routes.Schedule.route) { launchSingleTop = true } }
    val onNavigateToReport: () -> Unit =
        { navController.navigate(Routes.Report.route) { launchSingleTop = true } }

    val onNavigateToProfile: () -> Unit = {
        val userType = sessionManager.getSavedUserType()
        val savedRole = sessionManager.getSavedRole()?.lowercase()
        val userId = sessionManager.getSavedUserId() ?: ""
        val staffId = sessionManager.getSavedStaffId()

        val role = when (userType) {
            "PATIENT" -> "patient"
            "CLINIC_STAFF" -> if (savedRole == "doctor") "doctor" else "staff"
            else -> "patient"
        }

        val route = Routes.profileRoute(role, userId, staffId)
        navController.navigate(route) { launchSingleTop = true }
    }

    val finalTitle = if (screenTitle.isNotEmpty()) {
        screenTitle
    } else {
        when {
            currentRoute == Routes.Home.route -> "Upcoming Appointments"
            currentRoute == Routes.Schedule.route -> "Doctor Schedules"
            currentRoute == Routes.Report.route -> "Reports"
            currentRoute?.startsWith("profile") == true -> "Profile"
            else -> ""
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.onPrimary,
        topBar = {
            TopBarNav(
                title = finalTitle,
                onNotificationClick = { navController.navigate(Routes.Notifications.route) }
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                onNavigateHome = onNavigateHome,
                onNavigateToSchedule = onNavigateToSchedule,
                onNavigateToReport = onNavigateToReport,
                onNavigateToProfile = onNavigateToProfile
            )
        },
        content = content
    )
}
