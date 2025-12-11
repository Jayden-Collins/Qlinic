import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.qlinic.ui.navigation.BottomNavBar
import com.example.qlinic.ui.navigation.Screen
import com.example.qlinic.ui.navigation.TopBarNav

@Composable
fun MainAppScaffold(
    navController: NavController,
    //special composable lambda to hold the content of the screen
    content: @Composable (PaddingValues) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val onNavigateHome: () -> Unit =
        { navController.navigate(Screen.Home.route) { launchSingleTop = true } }
    val onNavigateToSchedule: () -> Unit =
        { navController.navigate(Screen.Schedule.route) { launchSingleTop = true } }
    val onNavigateToReport: () -> Unit =
        { navController.navigate(Screen.Report.route) { launchSingleTop = true } }
    val onNavigateToProfile: () -> Unit =
        { navController.navigate(Screen.Profile.route) { launchSingleTop = true } }

    val title = when (currentRoute) {
        Screen.Home.route -> "Upcoming Appointments"
        Screen.Schedule.route -> "Doctor Schedules"
        Screen.Report.route -> "Reports"
        Screen.Profile.route -> "Profile"
        else -> "" // Default or loading title
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.onPrimary,
        topBar = {
            TopBarNav(
                title = title,
                onNotificationClick = { /* TODO: Handle notification click */ }
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