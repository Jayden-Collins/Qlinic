package com.example.qlinic.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.qlinic.R
import com.example.qlinic.data.model.TestUsers
import com.example.qlinic.data.model.UserRole

data class BottomNavItem(
    val route: String,
    val iconResId: Int,
    val label: String
)

val navigationItems = listOf(
    BottomNavItem(Routes.Home.route, R.drawable.ic_home, "Home"),
    BottomNavItem(Routes.Schedule.route, R.drawable.ic_schedule, "Schedule"),
    BottomNavItem(Routes.Report.route, R.drawable.ic_report,"Report"),
    BottomNavItem(Routes.Profile.route, R.drawable.ic_profile, "Profile")
)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigateHome: () -> Unit,
    onNavigateToSchedule: () -> Unit,
    onNavigateToReport: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val navigationActions = mapOf(
        Routes.Home.route to onNavigateHome,
        Routes.Schedule.route to onNavigateToSchedule,
        Routes.Report.route to onNavigateToReport,
        Routes.Profile.route to onNavigateToProfile
    )

    val visibleItems = if (TestUsers.current.role == UserRole.PATIENT) {
        // Patients don't see Report
        navigationItems.filter { it.label != "Report" }
    } else if (TestUsers.current.role == UserRole.DOCTOR) {
        // Doctor see only Home & Profile
        navigationItems.filter { it.label != "Report" &&  it.label != "Schedule" }
    } else {
        // Staff see everything
        navigationItems
    }
    MaterialTheme.colorScheme.primary
    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    MaterialTheme.colorScheme.surface

    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        NavigationBar(containerColor = MaterialTheme.colorScheme.onPrimary) {
            visibleItems.forEach { item ->
                NavigationBarItem(
                    selected = (currentRoute == item.route),
                    onClick = { navigationActions[item.route]?.invoke() },
                    icon = {
                        Icon(
                            painter = painterResource(id = item.iconResId),
                            contentDescription = item.label,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSurface,
                        unselectedIconColor = MaterialTheme.colorScheme.outline,
                        indicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )
            }
        }
    }
}
