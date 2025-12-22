package com.example.qlinic.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.qlinic.R
import com.example.qlinic.ui.theme.darkblue
import com.example.qlinic.ui.theme.grey

@Composable
fun BottomNavBar(navController: NavController) {
    // get current route to determine which item is selected
    val currentRoute = navController.currentDestination?.route
    val selectedColor = darkblue
    val unselectedColor = darkblue

    Column {
        HorizontalDivider(
            color = grey,
            thickness = 1.dp,
        )
        NavigationBar(
            containerColor = Color.Transparent,
        ){
            // home icon
            NavigationBarItem(
                icon = {
                    Icon(
                        painterResource(R.drawable.home_2),
                        contentDescription = "Home Icon",
                        tint = if(currentRoute == null || currentRoute == Routes.Home.route) selectedColor else unselectedColor
                    )
                },
                selected = currentRoute == null || currentRoute == Routes.Home.route,
                onClick = { navController.navigate(Routes.Home.route) }
            )

            // Doctor calendar icon for schedule
            NavigationBarItem(
                icon = {
                    Icon(
                        painterResource(R.drawable.calendar),
                        contentDescription = "Calendar Icon",
                        tint = if(currentRoute == Routes.DoctorCalendar.route) selectedColor else unselectedColor
                    )
                },
                selected = currentRoute == Routes.DoctorCalendar.route,
                onClick = { navController.navigate(Routes.DoctorCalendar.route) }
            )

            // report icon
            NavigationBarItem(
                icon = {
                    Icon(
                        painterResource(R.drawable.mdi_report_box_outline),
                        contentDescription = "Report Icon",
                        tint = if(currentRoute == Routes.Report.route) selectedColor else unselectedColor
                    )
                },
                selected = currentRoute == Routes.Report.route,
                onClick = { navController.navigate(Routes.Report.route) }
            )

            // profile icon
            NavigationBarItem(
                icon = {
                    Icon (
                        painterResource(R.drawable.profile),
                        contentDescription = "Profile Icon",
                        tint = if(currentRoute == Routes.Profile.route) selectedColor else unselectedColor
                    )
                },
                selected = currentRoute == Routes.Profile.route,
                onClick = { navController.navigate(Routes.Profile.route) }
            )
        }
    }
}
