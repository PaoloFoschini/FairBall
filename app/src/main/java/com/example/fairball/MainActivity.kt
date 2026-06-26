package com.example.fairball

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.fairball.data.FirebaseDataSeeder
import com.example.fairball.ui.AdminScreen
import com.example.fairball.ui.ChampionshipScreen
import com.example.fairball.ui.HomeScreen
import com.example.fairball.ui.LeagueRefereesScreen
import com.example.fairball.ui.LoginScreen
import com.example.fairball.ui.MapScreen
import com.example.fairball.ui.MatchRefereeScreen
import com.example.fairball.ui.MatchSummaryScreen
import com.example.fairball.ui.ProfileScreen
import com.example.fairball.ui.theme.FairBallTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        FirebaseDataSeeder.seedData()

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

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(onLoginSuccess = { role, uid ->
                val destination = if (uid != null) "home/$role?uid=$uid" else "home/$role"
                navController.navigate(destination) {
                    popUpTo("login") { inclusive = true }
                }
            })
        }
        composable(
            route = "home/{role}?uid={uid}",
            arguments = listOf(
                navArgument("role") { type = NavType.StringType },
                navArgument("uid") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "User"
            val uid = backStackEntry.arguments?.getString("uid")
            HomeScreen(
                role = role,
                debugUid = uid,
                onViewReferees = { navController.navigate("league_referees") },
                onViewProfile = {
                    if (role == "admin") {
                        navController.navigate("admin")
                    } else {
                        navController.navigate("profile")
                    }
                },
                onViewChampionship = { navController.navigate("championship") },
                onViewMap = { navController.navigate("map") },
                onArbitrateMatch = { matchId -> navController.navigate("match_referee/$matchId") }
            )
        }
        composable("admin") {
            AdminScreen(onBack = { navController.popBackStack() })
        }
        composable("league_referees") {
            LeagueRefereesScreen(onBack = { navController.popBackStack() })
        }
        composable("map") {
            MapScreen(onBack = { navController.popBackStack() })
        }
        composable("profile") {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("home/{role}?uid={uid}") { inclusive = true }
                    }
                }
            )
        }
        composable("championship") {
            ChampionshipScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = "match_referee/{matchId}",
            arguments = listOf(navArgument("matchId") { type = NavType.StringType })
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
            MatchRefereeScreen(
                matchId = matchId,
                onBack = { navController.popBackStack() },
                onEndMatch = { id ->
                    navController.navigate("match_summary/$id")
                }
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
                    navController.popBackStack("home/{role}?uid={uid}", inclusive = false)
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}