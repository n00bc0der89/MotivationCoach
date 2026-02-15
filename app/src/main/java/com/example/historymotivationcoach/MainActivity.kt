package com.example.historymotivationcoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.example.historymotivationcoach.business.NotificationScheduler
import com.example.historymotivationcoach.business.NotificationSchedulerImpl
import com.example.historymotivationcoach.data.AppDatabase
import com.example.historymotivationcoach.data.repository.MotivationRepository
import com.example.historymotivationcoach.data.repository.PreferencesRepository
import com.example.historymotivationcoach.ui.navigation.AppNavHost
import com.example.historymotivationcoach.ui.navigation.BottomNavigationBar
import com.example.historymotivationcoach.ui.theme.HistoryMotivationCoachTheme
import com.example.historymotivationcoach.viewmodel.HistoryViewModel
import com.example.historymotivationcoach.viewmodel.HomeViewModel
import com.example.historymotivationcoach.viewmodel.SettingsViewModel

/**
 * Main activity for the History Motivation Coach app.
 * 
 * Displays a loading screen while seed data is being loaded on first launch,
 * then shows the main app content once initialization is complete.
 * 
 * Requirements:
 * - 22.1: Show loading indicator during seed data load
 * - 22.3: Display user-friendly error message if loading fails
 * - 11.1: Bottom navigation with three tabs
 * - 3.2: Handle deep links from notifications
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HistoryMotivationCoachTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Extract motivation ID from intent if present (deep link from notification)
                    val motivationId = intent?.getLongExtra("motivation_id", -1L) ?: -1L
                    AppContent(initialMotivationId = if (motivationId > 0) motivationId else null)
                }
            }
        }
    }
    
    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        // Handle new intents when activity is already running
        setIntent(intent)
    }
}

@Composable
fun AppContent(initialMotivationId: Long? = null) {
    val app = MotivationApplication.getInstance()
    val initState by app.initializationState.collectAsState()
    
    when (initState) {
        is MotivationApplication.InitializationState.Loading -> {
            LoadingScreen()
        }
        is MotivationApplication.InitializationState.Success -> {
            MainAppContent(initialMotivationId = initialMotivationId)
        }
        is MotivationApplication.InitializationState.Error -> {
            val errorMessage = (initState as MotivationApplication.InitializationState.Error).message
            ErrorScreen(errorMessage)
        }
    }
}

@Composable
fun MainAppContent(initialMotivationId: Long? = null) {
    val context = LocalContext.current
    val navController = rememberNavController()
    
    // Initialize dependencies (in production, use proper DI like Hilt)
    val database = AppDatabase.getInstance(context)
    val motivationRepository = MotivationRepository(
        database.motivationDao(),
        database.historyDao()
    )
    val preferencesRepository = PreferencesRepository(database.preferencesDao())
    val notificationScheduler: NotificationScheduler = NotificationSchedulerImpl(context, preferencesRepository)
    
    // Create ViewModels
    val homeViewModel = HomeViewModel(motivationRepository, preferencesRepository, notificationScheduler)
    val historyViewModel = HistoryViewModel(motivationRepository)
    val settingsViewModel = SettingsViewModel(
        context,
        preferencesRepository,
        motivationRepository,
        notificationScheduler
    )
    
    // Handle deep link navigation from notification
    androidx.compose.runtime.LaunchedEffect(initialMotivationId) {
        if (initialMotivationId != null && initialMotivationId > 0) {
            navController.navigate(com.example.historymotivationcoach.ui.navigation.Screen.Detail.createRoute(initialMotivationId))
        }
    }
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            AppNavHost(
                navController = navController,
                homeViewModel = homeViewModel,
                historyViewModel = historyViewModel,
                settingsViewModel = settingsViewModel
            )
        }
    }
}

/**
 * Loading screen displayed while seed data is being loaded.
 * Shows a circular progress indicator and loading message.
 */
@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading motivational content...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

/**
 * Error screen displayed if seed data loading fails.
 * Shows the error message and a retry button.
 */
@Composable
fun ErrorScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "⚠️",
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = "Oops! Something went wrong",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = {
                    // Restart the app
                    android.os.Process.killProcess(android.os.Process.myPid())
                }
            ) {
                Text("Restart App")
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun LoadingScreenPreview() {
    HistoryMotivationCoachTheme {
        LoadingScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun ErrorScreenPreview() {
    HistoryMotivationCoachTheme {
        ErrorScreen("Failed to load motivational content. Please restart the app or contact support if the problem persists.")
    }
}
