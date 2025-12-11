package com.example.qlinic.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Schedule : Screen("schedule")
    object Report : Screen("report")
    object Profile : Screen("profile")
}