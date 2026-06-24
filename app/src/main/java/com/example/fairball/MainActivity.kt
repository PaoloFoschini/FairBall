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
import com.example.fairball.ui.*
import com.example.fairball.ui.theme.FairBallTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        
        // Popola i dati se necessario (puoi commentarlo dopo il primo avvio)
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
            LoginScreen(onLoginSuccess = { role ->
                navController.navigate("home/$role") {
                    popUpTo("login") { inclusive = true }
                }
            })
        }
        composable(
            route = "home/{role}",
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "User"
            HomeScreen(
                role = role,
                onViewReferees = { navController.navigate("league_referees") },
                onViewProfile = { navController.navigate("profile") },
                onViewChampionship = { navController.navigate("championship") },
                onViewMap = { navController.navigate("map") }
            )
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
                        popUpTo("home/{role}") { inclusive = true }
                    }
                }
            )
        }
        composable("championship") {
            ChampionshipScreen(onBack = { navController.popBackStack() })
        }
    }
}
