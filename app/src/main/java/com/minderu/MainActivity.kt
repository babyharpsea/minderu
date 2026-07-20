package com.minderu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.minderu.data.MinderuDestination
import com.minderu.data.Screen
import com.minderu.data.mockBinderCards
import com.minderu.data.mockPlantings
import com.minderu.ui.components.FloatingNavDock
import com.minderu.ui.screens.AuthScreen
import com.minderu.ui.screens.CardBinderScreen
import com.minderu.ui.screens.ImpactTrackerScreen
import com.minderu.ui.screens.TodayTasksScreen
import com.minderu.ui.theme.MinderuTheme
import com.minderu.ui.viewmodels.AuthViewModel
import com.minderu.ui.viewmodels.TodayTasksViewModel
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.android.gms.ads.MobileAds
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = Firebase.analytics

        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@MainActivity) {}
        }

        setContent {
            MinderuTheme {
                MinderuApp()
            }
        }
    }
}

@Composable
fun MinderuApp(
    modifier: Modifier = Modifier
) {
    val authViewModel: AuthViewModel = viewModel()
    val sessionStatus by authViewModel.sessionStatus.collectAsStateWithLifecycle()

    when (sessionStatus) {
        is SessionStatus.LoadingFromStorage -> {
            // Full-screen loading spinner while restoring session
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is SessionStatus.NotAuthenticated -> {
            val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
            AuthScreen(
                uiState = authUiState,
                onSignUp = { email, password -> authViewModel.signUp(email, password) },
                onSignIn = { email, password -> authViewModel.signIn(email, password) },
                onClearError = { authViewModel.clearError() },
                modifier = modifier
            )
        }

        is SessionStatus.Authenticated -> {
            MainAppContent(
                authViewModel = authViewModel,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun MainAppContent(
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Create ViewModel once and share across recompositions
    val todayTasksViewModel: TodayTasksViewModel = viewModel()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            FloatingNavDock(
                selectedDestination = MinderuDestination.entries.find { it.screen.route == currentRoute }
                    ?: MinderuDestination.Dashboard,
                onDestinationSelected = { destination ->
                    navController.navigate(destination.screen.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onFabClick = {
                    // FAB click now handled within TodayTasksScreen via its own bottom sheet
                    // Navigate to Dashboard if not already there
                    if (currentRoute != Screen.Dashboard.route) {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route
        ) {
            composable(Screen.Dashboard.route) {
                TodayTasksScreen(
                    viewModel = todayTasksViewModel,
                    contentPadding = innerPadding,
                    onSignOut = { authViewModel.signOut() }
                )
            }
            composable(Screen.Binder.route) {
                CardBinderScreen(
                    cards = mockBinderCards,
                    sparksBalance = 350,
                    contentPadding = innerPadding
                )
            }
            composable(Screen.Impact.route) {
                ImpactTrackerScreen(
                    treesPlanted = 12,
                    plantings = mockPlantings,
                    contentPadding = innerPadding
                )
            }
        }
    }
}
