package com.example.qlinic.ui.navigation

import MainAppScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val doctorRepository = remember { FirestoreDoctorRepository() }
    val scheduleViewModel: ScheduleViewModel = viewModel(
        factory = ScheduleViewModelFactory(doctorRepository)
    )

    NavHost(navController = navController, startDestination = Routes.Home.route) {

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
        }

        // 3. Doctor Details Route
        composable("doctor_details/{doctorId}") {
            // We reuse the same ViewModel because it holds the 'selectedDoctor' state
            DoctorDetailsScreen(
                viewModel = scheduleViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.Report.route) {
            MainAppScaffold(navController = navController) {paddingValues ->
                ReportScreen(paddingValues = paddingValues,)
            }
        }

        composable(Routes.Profile.route) {
            MainAppScaffold(navController = navController) {paddingValues ->
                Profile(paddingValues = paddingValues,)
            }
        }
    }
}