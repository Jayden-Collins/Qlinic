package com.example.qlinic.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.qlinic.ui.screen.LoginScreen
import com.example.qlinic.ui.screen.PatientSignUpScreen
import com.example.qlinic.data.repository.FirestoreAppointmentRepository
import com.example.qlinic.data.repository.FirestoreDoctorRepository
import com.example.qlinic.ui.screen.DoctorDetailsScreen
import com.example.qlinic.ui.screen.HomeScreen
import com.example.qlinic.ui.screen.Schedule
import com.example.qlinic.ui.viewmodel.HomeViewModel
import com.example.qlinic.ui.viewmodel.HomeViewModelFactory
import com.example.qlinic.ui.viewmodel.ScheduleViewModel
import com.example.qlinic.ui.viewmodel.ScheduleViewModelFactory
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.qlinic.data.model.SessionManager
import com.example.qlinic.ui.screen.BookAppt
import com.example.qlinic.ui.screen.EditProfileScreen
import com.example.qlinic.ui.screen.ForgotPasswordScreen
import com.example.qlinic.ui.screen.ProfileScreen
import com.example.qlinic.ui.screen.Notifs
import com.example.qlinic.ui.viewmodel.BookApptViewModel
import com.example.qlinic.ui.viewmodel.BookApptViewModelFactory

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
                    onNavigateToSchedule = { navController.navigate(Routes.Schedule.route) }
                )
            }
        }

        composable(Routes.USER_SELECTION) {
            LoginScreen(
                navController = navController
            )
        }

        composable(Routes.Schedule.route) {
            MainAppScaffold(navController = navController) { paddingValues ->
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
                onUpClick = { navController.popBackStack() },
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

        composable(Routes.REPORT) {
            ReportScreen(onNavigateHome = { navController.navigate(Routes.HOME) })
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
    }
}
