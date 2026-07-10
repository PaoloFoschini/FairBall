package com.example.fairball

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
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
// import com.example.fairball.data.FirebaseDataSeeder
import com.example.fairball.ui.*
import com.example.fairball.ui.theme.FairBallTheme
import com.google.firebase.FirebaseApp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/**
 * Route della navigazione, centralizzate per evitare stringhe duplicate/refusi
 * tra le dichiarazioni `composable(route = ...)` e le chiamate `navigateSafe(...)`.
 */
private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home/{role}?uid={uid}"
    const val NOTIFICATIONS = "notifications"
    const val LEAGUE_REFEREES = "league_referees"
    const val PROFILE = "profile?uid={uid}"
    const val CHAMPIONSHIP = "championship"
    const val MAP = "map"
    const val MATCH_REFEREE = "match_referee/{matchId}"
    const val MATCH_SUMMARY = "match_summary/{matchId}"
    const val MATCH_REPORT = "match_report/{matchId}"

    fun home(role: String, uid: String?) = if (uid != null) "home/$role?uid=$uid" else "home/$role"
    fun profile(uid: String? = null) = if (uid != null) "profile?uid=$uid" else "profile"
    fun matchReferee(matchId: String) = "match_referee/$matchId"
    fun matchSummary(matchId: String) = "match_summary/$matchId"
    fun matchReport(matchId: String) = "match_report/$matchId"
}

/**
 * Navigazione tra le schermate.
 */
private fun NavController.isResumed(): Boolean =
    currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED

private fun NavController.navigateSafe(route: String, builder: NavOptionsBuilder.() -> Unit = {}) {
    if (isResumed()) navigate(route, builder)
}

private fun NavController.popBackStackSafe() {
    if (isResumed()) popBackStack()
}

/** Naviga sostituendo l'intero back stack: usato per i redirect post-login/logout. */
private fun NavController.navigateAndClearBackStack(route: String) {
    navigateSafe(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

private fun NavController.handleAuthSuccess(role: String, uid: String?) {
    Session.uid = uid
    Session.role = role
    navigateAndClearBackStack(Routes.home(role, uid))
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            FirebaseApp.initializeApp(this)
            // FirebaseDataSeeder.seedData()
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
        NavHost(navController = navController, startDestination = Routes.LOGIN) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = { role, uid -> navController.handleAuthSuccess(role, uid) },
                    onNavigateToRegister = { navController.navigateSafe(Routes.REGISTER) }
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    onRegisterSuccess = { role, uid -> navController.handleAuthSuccess(role, uid) },
                    onNavigateToLogin = { navController.popBackStackSafe() }
                )
            }
            composable(
                route = Routes.HOME,
                arguments = listOf(
                    navArgument("role") { type = NavType.StringType },
                    navArgument("uid") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) {
                val currentRole = Session.role
                if (currentRole == null) {
                    LaunchedEffect(Unit) {
                        navController.navigateAndClearBackStack(Routes.LOGIN)
                    }
                    return@composable
                }

                HomeScreen(
                    role = currentRole,
                    debugUid = Session.uid,
                    onViewReferees = { navController.navigateSafe(Routes.LEAGUE_REFEREES) { launchSingleTop = true } },
                    onViewProfile = { navController.navigateSafe(Routes.profile()) { launchSingleTop = true } },
                    onViewRefereeProfile = { refereeId -> navController.navigateSafe(Routes.profile(refereeId)) },
                    onViewChampionship = { navController.navigateSafe(Routes.CHAMPIONSHIP) { launchSingleTop = true } },
                    onViewMap = { navController.navigateSafe(Routes.MAP) { launchSingleTop = true } },
                    onArbitrateMatch = { matchId -> navController.navigateSafe(Routes.matchReferee(matchId)) },
                    onViewNotifications = { navController.navigateSafe(Routes.NOTIFICATIONS) { launchSingleTop = true } }
                )
            }
            composable(Routes.NOTIFICATIONS) {
                val uid = Session.uid
                if (uid == null) {
                    LaunchedEffect(Unit) { navController.popBackStackSafe() }
                } else {
                    NotificationsScreen(
                        uid = uid,
                        onBack = { navController.popBackStackSafe() },
                        onOpenMatch = { matchId -> navController.navigateSafe(Routes.matchReport(matchId)) }
                    )
                }
            }
            composable(Routes.LEAGUE_REFEREES) {
                LeagueRefereesScreen(
                    onBack = { navController.popBackStackSafe() },
                    onRefereeClick = { refereeId -> navController.navigateSafe(Routes.profile(refereeId)) }
                )
            }
            composable(
                route = Routes.PROFILE,
                arguments = listOf(
                    navArgument("uid") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { backStackEntry ->
                val uid = backStackEntry.arguments?.getString("uid")
                ProfileScreen(
                    refereeId = uid,
                    onBack = { navController.popBackStackSafe() },
                    onViewMatchReport = { matchId -> navController.navigateSafe(Routes.matchReport(matchId)) },
                    onLogoutSuccess = {
                        Session.clear()
                        navController.navigateAndClearBackStack(Routes.LOGIN)
                    },
                    currentTheme = currentTheme,
                    onThemeChange = onThemeChange
                )
            }
            composable(Routes.CHAMPIONSHIP) {
                ChampionshipScreen(
                    onBack = { navController.popBackStackSafe() },
                    onViewReport = { matchId -> navController.navigateSafe(Routes.matchReport(matchId)) }
                )
            }
            composable(Routes.MAP) {
                MapScreen(onBack = { navController.popBackStackSafe() })
            }
            composable(
                route = Routes.MATCH_REFEREE,
                arguments = listOf(navArgument("matchId") { type = NavType.StringType })
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                MatchRefereeScreen(
                    matchId = matchId,
                    onBack = { navController.popBackStackSafe() },
                    onEndMatch = { id -> navController.navigateSafe(Routes.matchSummary(id)) }
                )
            }
            composable(
                route = Routes.MATCH_SUMMARY,
                arguments = listOf(navArgument("matchId") { type = NavType.StringType })
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                MatchSummaryScreen(
                    matchId = matchId,
                    onFinish = {
                        val role = Session.role ?: "arbitro"
                        val uid = Session.uid
                        navController.navigateAndClearBackStack(Routes.home(role, uid))
                    },
                    onBack = { navController.popBackStackSafe() }
                )
            }
            composable(
                route = Routes.MATCH_REPORT,
                arguments = listOf(navArgument("matchId") { type = NavType.StringType })
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                MatchReportScreen(
                    matchId = matchId,
                    onClose = { navController.popBackStackSafe() }
                )
            }
        }
    }
}
