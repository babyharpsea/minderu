package com.minderu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.minderu.ui.components.AddTaskBottomSheet
import com.minderu.ui.components.FloatingNavDock
import com.minderu.ui.screens.AuthScreen
import com.minderu.ui.screens.CardBinderScreen
import com.minderu.ui.screens.ImpactTrackerScreen
import com.minderu.ui.screens.TodayTasksScreen
import com.minderu.ui.theme.MinderuTheme
import com.minderu.ui.viewmodels.AuthViewModel
import com.minderu.ui.viewmodels.CardBinderViewModel
import com.minderu.ui.viewmodels.ImpactTrackerViewModel
import com.minderu.ui.viewmodels.TodayTasksViewModel
import io.github.jan.supabase.auth.status.SessionStatus

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

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
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory())
    val sessionStatus by authViewModel.sessionStatus.collectAsStateWithLifecycle()

    when (val status = sessionStatus) {
        SessionStatus.Initializing -> {
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
            val activeSheet by authViewModel.activeSheet.collectAsStateWithLifecycle()
            val fieldErrors by authViewModel.fieldErrors.collectAsStateWithLifecycle()
            AuthScreen(
                uiState = authUiState,
                activeSheet = activeSheet,
                fieldErrors = fieldErrors,
                onOpenSheet = { authViewModel.openSheet(it) },
                onCloseSheet = { authViewModel.closeSheet() },
                onSignUp = { email, password -> authViewModel.signUp(email, password) },
                onSignIn = { email, password -> authViewModel.signIn(email, password) },
                onForgotPassword = { email -> authViewModel.sendPasswordReset(email) },
                onSignInWithPasskey = { authViewModel.signInWithPasskey() },
                onClearError = { authViewModel.clearError() },
                modifier = modifier
            )
        }

        is SessionStatus.Authenticated -> {
            val userId = status.session.user?.id
            MainAppContent(
                authViewModel = authViewModel,
                userId = userId,
                modifier = modifier
            )
        }

        is SessionStatus.RefreshFailure -> {
            // Recoverable screen instead of TODO() crash
            RefreshFailureScreen(
                onRetry = {
                    // The SDK will retry on its own when connectivity returns,
                    // but signing out and back in is the reliable path.
                },
                onSignOut = { authViewModel.signOut() },
                modifier = modifier
            )
        }
    }
}

/**
 * Shown when the JWT refresh fails (offline launch, revoked token, etc.).
 * The old code had `TODO()` here which threw NotImplementedError.
 */
@Composable
private fun RefreshFailureScreen(
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Session expired",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Your session couldn't be refreshed.\nSign in again to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onSignOut) {
                Text("Sign In Again")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun MainAppContent(
    authViewModel: AuthViewModel,
    userId: String?,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Key the ViewModel on the userId so sign out → sign in as a different user
    // gets a fresh ViewModel instead of reusing the previous user's tasks.
    val todayTasksViewModel: TodayTasksViewModel = viewModel(
        key = "tasks_$userId",
        factory = TodayTasksViewModel.Factory()
    )

    val cardBinderViewModel: CardBinderViewModel = viewModel(
        key = "binder_$userId",
        factory = CardBinderViewModel.Factory()
    )

    val impactTrackerViewModel: ImpactTrackerViewModel = viewModel(
        key = "impact_$userId",
        factory = ImpactTrackerViewModel.Factory()
    )

    val uiState by todayTasksViewModel.uiState.collectAsStateWithLifecycle()

    // FAB-triggered add task sheet (shared across screens)
    var showFabSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            FloatingNavDock(
                selectedDestination = MinderuDestination.entries.find { it.screen.route == currentRoute }
                    ?: MinderuDestination.Dashboard,
                onDestinationSelected = { destination ->
                    if (destination.screen == Screen.Binder) {
                        cardBinderViewModel.loadBinderData()
                    } else if (destination.screen == Screen.Impact) {
                        impactTrackerViewModel.loadImpactData()
                    }
                    navController.navigate(destination.screen.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onFabClick = {
                    // Navigate to Dashboard if not there, then open add sheet
                    if (currentRoute != Screen.Dashboard.route) {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    showFabSheet = true
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            enterTransition = {
                androidx.compose.animation.fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(280, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
                ) + androidx.compose.animation.scaleIn(
                    initialScale = 0.96f,
                    animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.82f, stiffness = 360f)
                )
            },
            exitTransition = {
                androidx.compose.animation.fadeOut(
                    animationSpec = androidx.compose.animation.core.tween(200, easing = androidx.compose.animation.core.FastOutLinearInEasing)
                )
            },
            popEnterTransition = {
                androidx.compose.animation.fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(280, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
                ) + androidx.compose.animation.scaleIn(
                    initialScale = 0.96f,
                    animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.82f, stiffness = 360f)
                )
            },
            popExitTransition = {
                androidx.compose.animation.fadeOut(
                    animationSpec = androidx.compose.animation.core.tween(200, easing = androidx.compose.animation.core.FastOutLinearInEasing)
                )
            }
        ) {
            composable(Screen.Dashboard.route) {
                TodayTasksScreen(
                    viewModel = todayTasksViewModel,
                    contentPadding = innerPadding,
                    userEmail = authViewModel.currentUserEmail(),
                    userId = userId,
                    onLinkPasskey = { authViewModel.linkPasskey() },
                    onSignOut = { authViewModel.signOut() }
                )
            }
            composable(Screen.Binder.route) {
                CardBinderScreen(
                    viewModel = cardBinderViewModel,
                    contentPadding = innerPadding
                )
            }
            composable(Screen.Impact.route) {
                ImpactTrackerScreen(
                    viewModel = impactTrackerViewModel,
                    contentPadding = innerPadding
                )
            }
        }
    }

    // FAB-triggered add task bottom sheet
    if (showFabSheet) {
        AddTaskBottomSheet(
            existingTask = null,
            onDismissRequest = { showFabSheet = false },
            onSubmit = { title, subhead, sparks ->
                todayTasksViewModel.addTask(title, subhead, sparks)
                showFabSheet = false
            }
        )
    }
}
