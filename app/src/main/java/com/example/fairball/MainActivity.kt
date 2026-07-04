package com.example.fairball

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.key
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.fairball.data.DataStoreManager
import com.example.fairball.data.ThemePreference
import com.example.fairball.data.FirebaseDataSeeder
import com.example.fairball.ui.*
import com.example.fairball.ui.theme.FairBallTheme
import com.google.firebase.FirebaseApp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

private fun NavController.navigateSafe(route: String, builder: NavOptionsBuilder.() -> Unit = {}) {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        navigate(route, builder)
    }
}

private fun NavController.popBackStackSafe() {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        popBackStack()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            FirebaseApp.initializeApp(this)
            FirebaseDataSeeder.seedData()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        enableEdgeToEdge()
        val dataStoreManager = DataStoreManager(this)

        setContent {
            val themePreference by dataStoreManager.themePreferenceFlow.collectAsState(initial = ThemePreference.SYSTEM)

            val scope = rememberCoroutineScope()

            FairBallTheme(
                themePreference = themePreference,
                dynamicColor = true
            ) {
                FairBallApp(
                    currentTheme = themePreference,
                    onThemeChange = { newTheme ->
                        scope.launch {
                            dataStoreManager.saveThemePreference(newTheme)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FairBallApp(
    currentTheme: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit
) {
    val navController = rememberNavController()

    key(Session.uid, Session.role) {
        NavHost(navController = navController, startDestination = "login") {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = { role, uid ->
                        Session.uid = uid
                        Session.role = role

                        val destination = if (uid != null) "home/$role?uid=$uid" else "home/$role"
                        navController.navigateSafe(destination) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToRegister = { navController.navigateSafe("register") }
                )
            }
            composable("register") {
                RegisterScreen(
                    onRegisterSuccess = { role, uid ->
                        Session.uid = uid
                        Session.role = role

                        val destination = if (uid != null) "home/$role?uid=$uid" else "home/$role"
                        navController.navigateSafe(destination) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToLogin = { navController.popBackStackSafe() }
                )
            }
            composable(
                route = "home/{role}?uid={uid}",
                arguments = listOf(
                    navArgument("role") { type = NavType.StringType },
                    navArgument("uid") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { backStackEntry ->
                val currentRole = Session.role
                if (currentRole == null) {
                    LaunchedEffect(Unit) {
                        navController.navigateSafe("login") {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                    return@composable
                }

                HomeScreen(
                    role = currentRole,
                    debugUid = Session.uid,
                    onViewReferees = {
                        navController.navigateSafe("league_referees") { launchSingleTop = true }
                    },
                    onViewProfile = { navController.navigateSafe("profile") { launchSingleTop = true } },
                    onViewRefereeProfile = { refereeId -> navController.navigateSafe("profile?uid=$refereeId") },
                    onViewChampionship = { navController.navigateSafe("championship") { launchSingleTop = true } },
                    onViewMap = { navController.navigateSafe("map") { launchSingleTop = true } },
                    onArbitrateMatch = { matchId -> navController.navigateSafe("match_referee/$matchId") }
                )
            }
            composable("league_referees") {
                LeagueRefereesScreen(
                    onBack = { navController.popBackStackSafe() },
                    onRefereeClick = { refereeId -> navController.navigateSafe("profile?uid=$refereeId") }
                )
            }
            composable(
                route = "profile?uid={uid}",
                arguments = listOf(
                    navArgument("uid") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { backStackEntry ->
                val uid = backStackEntry.arguments?.getString("uid")
                ProfileScreen(
                    refereeId = uid,
                    onBack = { navController.popBackStackSafe() },
                    onViewMatchReport = { matchId -> navController.navigateSafe("match_report/$matchId") },
                    onLogoutSuccess = {
                        Session.uid = null
                        Session.role = null
                        navController.navigateSafe("login") {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    currentTheme = currentTheme,
                    onThemeChange = onThemeChange
                )
            }
            composable("championship") {
                ChampionshipScreen(
                    onBack = { navController.popBackStackSafe() },
                    onViewReport = { matchId -> navController.navigateSafe("match_report/$matchId") }
                )
            }
            composable("map") {
                MapScreen(onBack = { navController.popBackStackSafe() })
            }
            composable(
                route = "match_referee/{matchId}",
                arguments = listOf(navArgument("matchId") { type = NavType.StringType })
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                MatchRefereeScreen(
                    matchId = matchId,
                    onBack = { navController.popBackStackSafe() },
                    onEndMatch = { id -> navController.navigateSafe("match_summary/$id") }
                )
            }
            composable(
                route = "match_summary/{matchId}",
                arguments = listOf(navArgument("matchId") { type = NavType.StringType })
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                MatchSummaryScreen(
                    matchId = matchId,
                    onFinish = {
                        navController.popBackStackSafe()
                    },
                    onBack = { navController.popBackStackSafe() }
                )
            }
            composable(
                route = "match_report/{matchId}",
                arguments = listOf(navArgument("matchId") { type = NavType.StringType })
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                MatchReportScreen(
                    matchId = matchId,
                    onClose = {
                        navController.popBackStackSafe()
                    }
                )
            }
        }
    }
}