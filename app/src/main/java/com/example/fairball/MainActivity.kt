package com.example.fairball

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.fairball.data.FirebaseDataSeeder
import com.example.fairball.ui.*
import com.example.fairball.ui.theme.FairBallTheme
import com.google.firebase.FirebaseApp


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
        setContent {
            FairBallTheme {
                FairBallApp()
            }
        }
    }
}

@Composable
fun FairBallApp() {
    val navController = rememberNavController()

    key(Session.uid, Session.role) {
        NavHost(navController = navController, startDestination = "login") {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = { role, uid ->
                        Session.uid = uid
                        Session.role = role

                        val destination = if (uid != null) "home/$role?uid=$uid" else "home/$role"
                        navController.navigate(destination) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToRegister = { navController.navigate("register") }
                )
            }
            composable("register") {
                RegisterScreen(
                    onRegisterSuccess = { role, uid ->
                        Session.uid = uid
                        Session.role = role

                        val destination = if (uid != null) "home/$role?uid=$uid" else "home/$role"
                        navController.navigate(destination) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() }
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
                        navController.navigate("login") {
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
                        if (currentRole == "admin") {
                            navController.navigate("league_referees")
                        }
                    },
                    onViewProfile = { navController.navigate("profile") },
                    onViewRefereeProfile = { refereeId -> navController.navigate("profile?uid=$refereeId") },
                    onViewChampionship = { navController.navigate("championship") },
                    onViewMap = { navController.navigate("map") },
                    onArbitrateMatch = { matchId -> navController.navigate("match_referee/$matchId") }
                )
            }
            composable("league_referees") {
                if (Session.role != "admin") {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                    return@composable
                }
                LeagueRefereesScreen(
                    onBack = { navController.popBackStack() },
                    onRefereeClick = { refereeId -> navController.navigate("profile?uid=$refereeId") }
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
                    onBack = { navController.popBackStack() },
                    onViewMatchReport = { matchId -> navController.navigate("match_report/$matchId") },
                    onLogoutSuccess = {
                        Session.uid = null
                        Session.role = null
                        navController.navigate("login") {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable("championship") {
                ChampionshipScreen(
                    onBack = { navController.popBackStack() },
                    onViewReport = { matchId -> navController.navigate("match_report/$matchId") }
                )
            }
            composable("map") {
                MapScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = "match_referee/{matchId}",
                arguments = listOf(navArgument("matchId") { type = NavType.StringType })
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                MatchRefereeScreen(
                    matchId = matchId,
                    onBack = { navController.popBackStack() },
                    onEndMatch = { id -> navController.navigate("match_summary/$id") }
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
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
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
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}