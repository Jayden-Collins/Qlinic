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

data class BottomNavItem(
    val route: String,
    val iconResId: Int,
    val label: String
)

val navigationItems = listOf(
    BottomNavItem(Screen.Home.route, R.drawable.ic_home, "Home"),
    BottomNavItem(Screen.Schedule.route, R.drawable.ic_schedule, "Schedule"),
    BottomNavItem(Screen.Report.route, R.drawable.ic_report,"Report"),
    BottomNavItem(Screen.Profile.route, R.drawable.ic_profile, "Profile")
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
        Screen.Home.route to onNavigateHome,
        Screen.Schedule.route to onNavigateToSchedule,
        Screen.Report.route to onNavigateToReport,
        Screen.Profile.route to onNavigateToProfile
    )

    MaterialTheme.colorScheme.primary
    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    MaterialTheme.colorScheme.surface

    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        NavigationBar(containerColor = MaterialTheme.colorScheme.onPrimary) {
            navigationItems.forEach { item ->
                NavigationBarItem(
                    selected = (currentRoute == item.route),
                    onClick = {
                        // Find the correct lambda from the map and invoke it
                        navigationActions[item.route]?.invoke()
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = item.iconResId),
                            contentDescription = item.label,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}