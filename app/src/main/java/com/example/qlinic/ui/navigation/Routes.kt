package com.example.qlinic.ui.navigation

sealed class Routes(val route: String) {
    object Home : Routes("home")
    object Schedule : Routes("schedule")
    object Report : Routes("report")
    object Profile : Routes("profile")
}