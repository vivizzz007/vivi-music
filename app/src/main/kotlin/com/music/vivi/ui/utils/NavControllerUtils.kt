package com.music.vivi.ui.utils

import androidx.navigation.NavController
import com.music.vivi.ui.screens.Screens

/**
 * Navigates back to one of the main screens (Home, Songs, etc.) in the backstack.
 * Pops the back stack until a main route is reached.
 */
fun NavController.backToMain() {
    val mainRoutes = Screens.MainScreens.map { it.route }

    while (previousBackStackEntry != null &&
        currentBackStackEntry?.destination?.route !in mainRoutes
    ) {
        popBackStack()
    }
}
