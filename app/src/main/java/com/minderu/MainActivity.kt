package com.minderu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.minderu.data.MinderuDestination
import com.minderu.data.Screen
import com.minderu.data.Task
import com.minderu.data.mockBinderCards
import com.minderu.data.mockPlantings
import com.minderu.ui.components.AddTaskBottomSheet
import com.minderu.ui.components.FloatingNavDock
import com.minderu.ui.screens.CardBinderScreen
import com.minderu.ui.screens.ImpactTrackerScreen
import com.minderu.ui.screens.TodayTasksScreen
import com.minderu.ui.theme.MinderuTheme
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

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
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Task Creation State
    var showBottomSheet by remember { mutableStateOf(false) }

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
                onFabClick = { showBottomSheet = true }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route
        ) {
            composable(Screen.Dashboard.route) {
                TodayTasksScreen(
                    contentPadding = innerPadding
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

        if (showBottomSheet) {
            AddTaskBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                onAddTask = { title, sparks ->
                    // Persistence should be handled in a ViewModel or the screen itself
                    // For now, we are refactoring screens to fetch from DB
                    showBottomSheet = false
                }
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun MinderuPreview() {
    MinderuTheme {
        MinderuApp()
    }
}
