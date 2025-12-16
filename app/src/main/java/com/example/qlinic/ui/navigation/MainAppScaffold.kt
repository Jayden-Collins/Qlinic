import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.qlinic.ui.navigation.BottomNavBar
import com.example.qlinic.ui.navigation.Routes
import com.example.qlinic.ui.navigation.TopBarNav

@Composable
fun MainAppScaffold(
    navController: NavController,
    screenTitle: String = "",
    //special composable lambda to hold the content of the screen
    content: @Composable (PaddingValues) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val onNavigateHome: () -> Unit =
        { navController.navigate(Routes.Home.route) { launchSingleTop = true } }
    val onNavigateToSchedule: () -> Unit =
        { navController.navigate(Routes.Schedule.route) { launchSingleTop = true } }
    val onNavigateToReport: () -> Unit =
        { navController.navigate(Routes.Report.route) { launchSingleTop = true } }
    val onNavigateToProfile: () -> Unit =
        { navController.navigate(Routes.Profile.route) { launchSingleTop = true } }

    val finalTitle = if (screenTitle.isNotEmpty()) {
        screenTitle
    } else {
        when (currentRoute) {
            Routes.Home.route -> "Upcoming Appointments"
            Routes.Schedule.route -> "Doctor Schedules"
            Routes.Report.route -> "Reports"
            Routes.Profile.route -> "Profile"
            else -> ""
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.onPrimary,
        topBar = {
            TopBarNav(
                title = finalTitle,
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