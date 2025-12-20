package com.example.qlinic.ui.navigation

import MainAppScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.qlinic.ui.screen.LoginScreen
import com.example.qlinic.ui.screen.PatientSignUpScreen
import com.example.qlinic.data.model.TestUsers
import com.example.qlinic.data.model.UserRole
import com.example.qlinic.data.repository.FirestoreAppointmentRepository
import com.example.qlinic.data.repository.FirestoreDoctorRepository
import com.example.qlinic.ui.screen.DoctorDetailsScreen
import com.example.qlinic.ui.screen.HomeScreen
import com.example.qlinic.ui.screen.Profile
import com.example.qlinic.ui.screen.ReportScreen
import com.example.qlinic.ui.screen.Schedule
import com.example.qlinic.ui.viewmodel.HomeViewModel
import com.example.qlinic.ui.viewmodel.HomeViewModelFactory
import com.example.qlinic.ui.viewmodel.ScheduleViewModel
import com.example.qlinic.ui.viewmodel.ScheduleViewModelFactory
import android.util.Log
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.qlinic.data.model.SessionManager
import com.example.qlinic.ui.screen.DoctorHomeScreen
import com.example.qlinic.ui.screen.EditProfileScreen
import com.example.qlinic.ui.screen.ForgotPasswordScreen
import com.example.qlinic.ui.screen.PatientHomeScreen
import com.example.qlinic.ui.screen.ProfileScreen
import com.example.qlinic.ui.screen.StaffHomeScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    val startDestination = remember {
        if (sessionManager.getIsLoggedIn()) {
            // Determine which screen based on saved user type
            when (sessionManager.getSavedUserType()) {
                "PATIENT" -> Routes.PATIENT_HOME
                "CLINIC_STAFF" -> {
                    when (sessionManager.getSavedRole()?.uppercase()) {
                        "DOCTOR" -> "${Routes.DOCTOR_HOME}/${sessionManager.getSavedStaffId()}"
                        else -> "${Routes.STAFF_HOME}/${sessionManager.getSavedStaffId()}"
                    }
                }
                else -> Routes.USER_SELECTION
            }
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
            val currentUser = TestUsers.current
            val dynamicTitle = when (currentUser.role) {
                UserRole.PATIENT -> "My Bookings"
                UserRole.DOCTOR -> "My Appointments"
                else -> "Upcoming Appointments"
            }
            val factory = HomeViewModelFactory(repository, currentUser)
            val homeViewModel: HomeViewModel = viewModel(factory = factory)

            MainAppScaffold(
                navController = navController,
                screenTitle = dynamicTitle
            ) {paddingValues ->
                HomeScreen(
                    paddingValues = paddingValues,
                    homeViewModel = homeViewModel,
                    onNavigateToSchedule = { navController.navigate(Routes.Schedule.route) }
                    )
            }
        composable(Routes.USER_SELECTION) {
            LoginScreen(
                navController = navController
            )
        }

        composable(Routes.PATIENT_HOME) {
            PatientHomeScreen(
                navController = navController
            )
        }

        composable(Routes.Schedule.route) {
            MainAppScaffold(navController = navController) { paddingValues ->
                Schedule(
                    paddingValues = paddingValues,
                    viewModel = scheduleViewModel, // Pass the VM
                    onDoctorClick = { doctorId ->
                        // Navigate to details (we just append the ID for the route)
                        navController.navigate("doctor_details/$doctorId")
                    }
                )
        }
            composable(Routes.SIGNUP) {
                PatientSignUpScreen(
                    navController = navController,
                    onLoginClick = { navController.navigate(Routes.USER_SELECTION) }
                )
            }

        // 3. Doctor Details Route
        composable("doctor_details/{doctorId}") {
            // We reuse the same ViewModel because it holds the 'selectedDoctor' state
            DoctorDetailsScreen(
                viewModel = scheduleViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

            composable(Routes.FORGET_PASSWORD) {
                ForgotPasswordScreen(navController = navController)
            }

            composable(
                route = "${Routes.DOCTOR_HOME}/{staffId}",
                arguments = listOf(navArgument("staffId") {
                    type = NavType.StringType
                })
            ) { backStackEntry ->
                val staffId = backStackEntry.arguments?.getString("staffId")!!
                DoctorHomeScreen(navController, staffId)
            }

            composable(
                route = "${Routes.STAFF_HOME}/{staffId}",
                arguments = listOf(navArgument("staffId") {
                    type = NavType.StringType
                })
            ) { backStackEntry ->
                val staffId = backStackEntry.arguments?.getString("staffId")!!
                StaffHomeScreen(navController, staffId)
            }

            // Role-only profile (e.g. profile/doctor -> uses auth UID)
            composable(
                Routes.PROFILE_ROLE_ONLY,
                arguments = listOf(navArgument("role") { type = NavType.StringType })
            ) { backStackEntry ->
                val role = backStackEntry.arguments?.getString("role") ?: "patient"
                Log.d("AppNavigation", "Profile role-only: $role")
                ProfileScreen(navController, role)
            }

            // Profile with role + userId (no staffId)
            composable(
                route = Routes.PROFILE_WITHOUT_STAFF,
                arguments = listOf(
                    navArgument("role") { type = NavType.StringType },
                    navArgument("userId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val role = backStackEntry.arguments?.getString("role")!!
                val userId = backStackEntry.arguments?.getString("userId")
                ProfileScreen(
                    navController = navController,
                    role = role,
                    staffId = null
                )
            }

            // Profile with role + userId + staffId
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
                ProfileScreen(
                    navController = navController,
                    role = role,
                    staffId = staffId
                )
            }
            // Edit Profile with role + userId (no staffId)
            composable(
                route = Routes.EDIT_PROFILE_WITHOUT_STAFF,
                arguments = listOf(
                    navArgument("role") { type = NavType.StringType },
                    navArgument("userId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val role = backStackEntry.arguments?.getString("role")!!
                val userId = backStackEntry.arguments?.getString("userId")!!
                EditProfileScreen(
                    navController = navController,
                    userId = userId,
                    role = role,
                    staffId = null
                )
            }

            // Edit Profile with role + userId + staffId
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
                EditProfileScreen(
                    navController = navController,
                    userId = userId,
                    role = role,
                    staffId = staffId
                )
            }
        }
    }