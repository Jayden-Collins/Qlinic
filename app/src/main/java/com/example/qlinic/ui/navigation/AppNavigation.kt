package com.example.qlinic.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.qlinic.ui.screen.LoginScreen
import com.example.qlinic.ui.screen.PatientSignUpScreen
import com.example.qlinic.data.repository.FirestoreDoctorRepository
import com.example.qlinic.ui.screen.DoctorDetailsScreen
import com.example.qlinic.ui.screen.HomeScreen
import com.example.qlinic.ui.screen.Schedule
import com.example.qlinic.ui.viewmodel.HomeViewModel
import com.example.qlinic.ui.viewmodel.HomeViewModelFactory
import com.example.qlinic.ui.viewmodel.ScheduleViewModel
import com.example.qlinic.ui.viewmodel.ScheduleViewModelFactory
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.navArgument
import com.example.qlinic.data.model.SessionManager
import com.example.qlinic.data.repository.DoctorSchedule
import com.example.qlinic.data.repository.FirestoreAppointmentRepository
import com.example.qlinic.ui.screen.BookAppt
import com.example.qlinic.ui.screen.EditProfileScreen
import com.example.qlinic.ui.screen.ForgotPasswordScreen
import com.example.qlinic.ui.screen.ProfileScreen
import com.example.qlinic.ui.screen.Notifs
import com.example.qlinic.ui.screen.ReportScreen
import com.example.qlinic.ui.viewmodel.BookApptViewModel
import com.example.qlinic.ui.viewmodel.BookApptViewModelFactory
import com.example.qlinic.ui.screens.DoctorCalendar
import com.example.qlinic.ui.screens.SpecificScheduleCalendar
import com.example.qlinic.ui.viewmodel.DoctorScheduleViewModel
import com.example.qlinic.ui.viewmodel.ScheduleAppointmentViewModel
import com.example.qlinic.ui.screens.RescheduleAppointmentScreen
import com.example.qlinic.ui.viewmodel.DoctorScheduleViewModelFactory


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val sessionManager = remember { SessionManager(context) }

    val startDestination = remember {
        if (sessionManager.getIsLoggedIn()) {
            Routes.Home.route
        } else {
            Routes.USER_SELECTION
        }
    }

    val doctorRepository = remember { FirestoreDoctorRepository() }
    val scheduleViewModel: ScheduleViewModel = viewModel(
        factory = ScheduleViewModelFactory(doctorRepository)
    )

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.Home.route) {
            val repository = remember { FirestoreAppointmentRepository() }

            val userType = sessionManager.getSavedUserType()
            val role = sessionManager.getSavedRole()?.uppercase()

            val dynamicTitle = when (userType) {
                "PATIENT" -> "My Bookings"
                "CLINIC_STAFF" -> {
                    if (role == "DOCTOR") "My Appointments"
                    else "Upcoming Appointments"
                }
                else -> "Upcoming Appointments"
            }

            val factory = HomeViewModelFactory(repository, sessionManager)
            val homeViewModel: HomeViewModel = viewModel(factory = factory)

            MainAppScaffold(
                navController = navController,
                screenTitle = dynamicTitle
            ) { paddingValues ->
                HomeScreen(
                    paddingValues = paddingValues,
                    homeViewModel = homeViewModel,
                    onNavigateToSchedule = {
                        val route = if (userType == "CLINIC_STAFF") Routes.DoctorCalendar.route else Routes.Schedule.route
                        navController.navigate(route)
                    },
                    navController = navController
                )
            }
        }

        composable(Routes.USER_SELECTION) {
            LoginScreen(
                navController = navController
            )
        }

        composable(Routes.Schedule.route) {
            MainAppScaffold(navController = navController, screenTitle = "Select Doctor") { paddingValues ->
                Schedule(
                    paddingValues = paddingValues,
                    viewModel = scheduleViewModel,
                    onDoctorClick = { doctorId ->
                        navController.navigate("doctor_details/$doctorId")
                    }
                )
            }
        }

        composable(Routes.SIGNUP) {
            PatientSignUpScreen(
                navController = navController,
                onLoginClick = { navController.navigate(Routes.USER_SELECTION) }
            )
        }

        composable("doctor_details/{doctorId}") { backStackEntry ->
            DoctorDetailsScreen(
                viewModel = scheduleViewModel,
                onBackClick = { navController.popBackStack() },
                onBookClick = { doctorId ->
                    navController.navigate("book_appointment/$doctorId")
                }
            )
        }

        // Book appointment screen
        composable(
            route = Routes.BOOK_APPOINTMENT,
            arguments = listOf(navArgument("doctorId") { type = NavType.StringType })
        ) { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
            val isStaff = sessionManager.getSavedUserType() == "CLINIC_STAFF"

            val bookApptViewModel: BookApptViewModel = viewModel(
                factory = BookApptViewModelFactory(sessionManager)
            )

            BookAppt(
                doctorId = doctorId,
                onUpClick = { navController.navigateUp() },
                isStaff = isStaff,
                viewModel = bookApptViewModel
            )
        }

        composable(Routes.FORGET_PASSWORD) {
            ForgotPasswordScreen(navController = navController)
        }

        composable(Routes.Notifications.route) {
            val isDoctor = sessionManager.getSavedRole()?.uppercase() == "DOCTOR"
            Notifs(
                onUpClick = { navController.popBackStack() },
                isDoctor = isDoctor
            )
        }

        composable(Routes.Report.route) {
            MainAppScaffold(navController = navController, screenTitle = "Reports") { paddingValues ->
                ReportScreen(paddingValues = paddingValues)
            }
        }

        composable(
            Routes.PROFILE_ROLE_ONLY,
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "patient"
            MainAppScaffold(navController = navController, screenTitle = "Profile") { paddingValues ->
                ProfileScreen(navController, paddingValues, role, onNotificationClick = {
                    navController.navigate(Routes.Notifications.route)
                })
            }
        }

        composable(
            route = Routes.PROFILE_WITHOUT_STAFF,
            arguments = listOf(
                navArgument("role") { type = NavType.StringType },
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role")!!
            MainAppScaffold(navController = navController, screenTitle = "Profile") { paddingValues ->
                ProfileScreen(
                    navController = navController,
                    paddingValues = paddingValues,
                    role = role,
                    staffId = null,
                    onNotificationClick = {navController.navigate(Routes.Notifications.route)}
                )
            }
        }

        composable(
            route = Routes.PROFILE,
            arguments = listOf(
                navArgument("role") { type = NavType.StringType },
                navArgument("userId") { type = NavType.StringType },
                navArgument("staffId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role")!!
            val staffId = backStackEntry.arguments?.getString("staffId")
            MainAppScaffold(navController = navController, screenTitle = "Profile") { paddingValues ->
                ProfileScreen(
                    navController = navController,
                    paddingValues = paddingValues,
                    role = role,
                    staffId = staffId,
                    onNotificationClick = {navController.navigate(Routes.Notifications.route)}
                )
            }
        }

        composable(
            route = Routes.EDIT_PROFILE_WITHOUT_STAFF,
            arguments = listOf(
                navArgument("role") { type = NavType.StringType },
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role")!!
            val userId = backStackEntry.arguments?.getString("userId")!!
            MainAppScaffold(navController = navController, screenTitle = "Edit Profile") { paddingValues ->
                EditProfileScreen(
                    navController = navController,
                    paddingValues = paddingValues,
                    userId = userId,
                    role = role,
                    staffId = null
                )
            }
        }

        composable(
            route = Routes.EDIT_PROFILE,
            arguments = listOf(
                navArgument("role") { type = NavType.StringType },
                navArgument("userId") { type = NavType.StringType },
                navArgument("staffId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role")!!
            val userId = backStackEntry.arguments?.getString("userId")!!
            val staffId = backStackEntry.arguments?.getString("staffId")

            MainAppScaffold(navController = navController, screenTitle = "Edit Profile") { paddingValues ->
                EditProfileScreen(
                    navController = navController,
                    paddingValues = paddingValues,
                    userId = userId,
                    role = role,
                    staffId = staffId
                )
            }
        }

        composable(Routes.DoctorCalendar.route){
            val doctorScheduleRepo = remember { DoctorSchedule() }
            val doctorScheduleViewModel: DoctorScheduleViewModel = viewModel(
                factory = DoctorScheduleViewModelFactory(doctorScheduleRepo)
            )
            DoctorCalendar(
                navController = navController,
                viewModel = doctorScheduleViewModel
            )
        }

        composable(route = Routes.DoctorAppointmentSchedule.route, arguments = listOf(navArgument("doctorID"){type = NavType.StringType}))
        { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorID") ?: ""
            val vm:  ScheduleAppointmentViewModel = viewModel()
            SpecificScheduleCalendar(
                navController = navController,
                doctorID = doctorId,
                viewModel = vm
            )
        }

        composable(
            route = Routes.DoctorAppointmentReschedule.route,
            arguments = listOf(
                navArgument("appointmentID") { type = NavType.StringType },
                navArgument("doctorID") { type = NavType.StringType },
                navArgument("rescheduleDate") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val appointmentID = backStackEntry.arguments?.getString("appointmentID") ?: ""
            val doctorId = backStackEntry.arguments?.getString("doctorID") ?: ""
            val rescheduleDateArg = backStackEntry.arguments?.getLong("rescheduleDate") ?: -1L
            val initialDateMillis = if (rescheduleDateArg > 0) rescheduleDateArg else null

            val vm: ScheduleAppointmentViewModel = viewModel()
            RescheduleAppointmentScreen(
                navController = navController,
                appointmentId = appointmentID,
                doctorId = doctorId,
                initialDateMillis = initialDateMillis,
                viewModel = vm
            )
        }

        // Reschedule Appointment Screen
        composable(
            route = "reschedule_appointment/{appointmentId}/{doctorId}",
            arguments = listOf(
                navArgument("appointmentId") { type = NavType.StringType },
                navArgument("doctorId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val appointmentId = backStackEntry.arguments?.getString("appointmentId") ?: ""
            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
            val scheduleAppointmentViewModel: ScheduleAppointmentViewModel = viewModel()
            RescheduleAppointmentScreen(
                navController = navController,
                appointmentId = appointmentId,
                doctorId = doctorId,
                viewModel = scheduleAppointmentViewModel
            )
        }
    }
}
