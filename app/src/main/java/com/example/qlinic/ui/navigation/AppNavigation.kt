package com.example.qlinic.ui.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.qlinic.ui.screens.Notification
import com.example.qlinic.ui.screens.DoctorCalendar
import com.example.qlinic.ui.screens.Profile
import com.example.qlinic.ui.screens.Report
import com.example.qlinic.ui.screens.SpecificScheduleCalendar
import com.example.qlinic.ui.viewModels.DoctorScheduleViewModel
import com.example.qlinic.ui.viewModels.ScheduleAppointmentViewModel
import com.example.qlinic.ui.screens.RescheduleAppointmentScreen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(viewModel: DoctorScheduleViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.DoctorCalendar.route) {

        composable(Routes.Notification.route){
            Notification(navController)
        }

        composable(Routes.DoctorCalendar.route){
            DoctorCalendar(navController = navController, viewModel = viewModel)
        }

        composable(route = Routes.DoctorAppointmentSchedule.route, arguments = listOf(navArgument("doctorID"){type = NavType.StringType}))
        { backStackEntry ->
            val doctorID = backStackEntry.arguments?.getString("doctorID") ?: ""
            val vm:  ScheduleAppointmentViewModel = viewModel()
            SpecificScheduleCalendar(
                navController = navController,
                doctorID = doctorID,
                viewModel = vm
            )
        }

        // Reschedule appointment screen - expects appointmentID and doctorID, optional rescheduleDate as query param
        composable(
            route = Routes.DoctorAppointmentReschedule.route,
            arguments = listOf(
                navArgument("appointmentID") { type = NavType.StringType },
                navArgument("doctorID") { type = NavType.StringType },
                navArgument("rescheduleDate") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val appointmentID = backStackEntry.arguments?.getString("appointmentID") ?: ""
            val doctorID = backStackEntry.arguments?.getString("doctorID") ?: ""
            val rescheduleDateArg = backStackEntry.arguments?.getLong("rescheduleDate") ?: -1L
            val initialDateMillis = if (rescheduleDateArg > 0) rescheduleDateArg else null

            val vm: ScheduleAppointmentViewModel = viewModel()
            RescheduleAppointmentScreen(
                navController = navController,
                appointmentId = appointmentID,
                doctorId = doctorID,
                initialDateMillis = initialDateMillis,
                viewModel = vm
            )
        }

        composable(Routes.Report.route){
            Report(navController)
        }

        composable(Routes.Profile.route){
            Profile(navController)
        }


    }
}