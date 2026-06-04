package com.hp.hp_omnipad.ui.home.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.hp.hp_omnipad.R

/*
 * Description: Sealed class defining the bottom navigation items for the app
 * Params: route - navigation route, titleRes - display title resource, icon - navigation icon
 * Returns: BottomNavItem instances for each navigation destination
 */
sealed class BottomNavItem(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    object Home : BottomNavItem(
        route = "home_tab",
        titleRes = R.string.home,
        icon = Icons.Default.Home
    )

    object Categories : BottomNavItem(
        route = "library",
        titleRes = R.string.categories,
        icon = Icons.Default.Category
    )

    object Settings : BottomNavItem(
        route = "settings",
        titleRes = R.string.settings,
        icon = Icons.Default.Settings
    )
}
