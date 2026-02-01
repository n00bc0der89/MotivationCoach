package com.example.historymotivationcoach.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.historymotivationcoach.ui.screens.DetailScreen
import com.example.historymotivationcoach.ui.screens.HistoryScreen
import com.example.historymotivationcoach.ui.screens.HomeScreen
import com.example.historymotivationcoach.ui.screens.SettingsScreen
import com.example.historymotivationcoach.viewmodel.HistoryViewModel
import com.example.historymotivationcoach.viewmodel.HomeViewModel
import com.example.historymotivationcoach.viewmodel.SettingsViewModel

/**
 * Navigation routes for the app.
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object History : Screen("history")
    object Settings : Screen("settings")
    object Detail : Screen("detail/{motivationId}") {
        fun createRoute(motivationId: Long) = "detail/$motivationId"
    }
}

/**
 * Bottom navigation items.
 * 
 * Requirements: 11.1, 11.2, 11.4, 18.3
 */
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem(
        route = Screen.Home.route,
        title = "Home",
        icon = Icons.Default.Home
    )
    
    object History : BottomNavItem(
        route = Screen.History.route,
        title = "History",
        icon = Icons.Default.History
    )
    
    object Settings : BottomNavItem(
        route = Screen.Settings.route,
        title = "Settings",
        icon = Icons.Default.Settings
    )
}

/**
 * Bottom navigation bar with three tabs.
 * Highlights the active tab and handles tab switching.
 * 
 * Requirements: 11.1, 11.2, 11.4, 18.3
 */
@Composable
fun BottomNavigationBar(
    navController: NavHostController
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.History,
        BottomNavItem.Settings
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    NavigationBar {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null // Icon description is provided by label
                    )
                },
                label = { Text(item.title) },
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                },
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = if (selected) {
                        "${item.title} tab, selected"
                    } else {
                        "${item.title} tab, not selected. Tap to navigate to ${item.title}."
                    }
                }
            )
        }
    }
}

/**
 * Main navigation host that manages screen navigation with smooth transitions.
 * 
 * @param navController The navigation controller
 * @param homeViewModel ViewModel for the home screen
 * @param historyViewModel ViewModel for the history screen
 * @param settingsViewModel ViewModel for the settings screen
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavHost(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    historyViewModel: HistoryViewModel,
    settingsViewModel: SettingsViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // Home screen with fade transition
        composable(
            route = Screen.Home.route,
            enterTransition = {
                fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300))
            }
        ) {
            HomeScreen(
                viewModel = homeViewModel,
                onMotivationClick = { motivationId ->
                    navController.navigate(Screen.Detail.createRoute(motivationId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        // History screen with fade transition
        composable(
            route = Screen.History.route,
            enterTransition = {
                fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300))
            }
        ) {
            HistoryScreen(
                viewModel = historyViewModel,
                onMotivationClick = { motivationId ->
                    navController.navigate(Screen.Detail.createRoute(motivationId))
                }
            )
        }
        
        // Settings screen with fade transition
        composable(
            route = Screen.Settings.route,
            enterTransition = {
                fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300))
            }
        ) {
            SettingsScreen(viewModel = settingsViewModel)
        }
        
        // Detail screen with slide-in from right transition
        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("motivationId") { type = NavType.LongType }
            ),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) { backStackEntry ->
            val motivationId = backStackEntry.arguments?.getLong("motivationId") ?: 0L
            DetailScreen(
                motivationId = motivationId,
                onBackClick = { navController.navigateUp() }
            )
        }
    }
}
