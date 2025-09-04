package com.crowdfunding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crowdfunding.ads.InterstitialManager
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.crowdfunding.data.IntentionKeys
import com.crowdfunding.ui.auth.LoginScreen
import com.crowdfunding.ui.auth.RegisterScreen
import com.crowdfunding.ui.common.SplashScreen
import com.crowdfunding.ui.profile.ProfileScreen
import com.crowdfunding.ui.profile.PublicProfileScreen
import com.crowdfunding.ui.projects.IntentionListScreen
import com.crowdfunding.ui.projects.ProjectDetailScreen
import com.crowdfunding.ui.projects.ProjectsScreen

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val PROFILE = "profile/{email}"
    const val PROJECTS = "projects"
    const val PROJECT_DETAIL = "project_detail/{projectId}"
    const val INTENTION_LIST = "intention_list/{projectId}/{intentionType}"
    const val PUBLIC_PROFILE = "public_profile/{userId}"

    fun createProfileRoute(email: String) = "profile/$email"
    fun createProjectDetailRoute(projectId: String) = "project_detail/$projectId"
    fun createIntentionListRoute(projectId: String, intentionType: String) = "intention_list/$projectId/$intentionType"
    fun createPublicProfileRoute(userId: String) = "public_profile/$userId"
}

@Composable
fun AppNavigation(
    mainViewModel: MainViewModel = viewModel()
) {
    val navController = rememberNavController()
    val appState by mainViewModel.appState.collectAsState()

    LaunchedEffect(appState) {
        when (val state = appState) {
            is AppState.NeedsLogin -> navController.navigateAndClearStack(Routes.LOGIN)
            is AppState.NeedsActivation -> navController.navigateAndClearStack(Routes.createProfileRoute(state.user.email ?: ""))
            is AppState.Ready -> navController.navigateAndClearStack(Routes.PROJECTS)
            is AppState.Loading -> { /* Handled by NavHost startDestination */ }
        }
    }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) { SplashScreen() }

        // Unauthenticated Routes
        composable(Routes.LOGIN) {
            LoginScreen(onNavigateToRegister = { navController.navigate(Routes.REGISTER) })
        }
        composable(Routes.REGISTER) {
            RegisterScreen(onNavigateToLogin = { navController.popBackStack() })
        }
        composable(Routes.PROFILE) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            ProfileScreen(
                email = email,
                onActivated = { navController.navigateAndClearStack(Routes.PROJECTS) }
            )
        }

        // Authenticated and Activated Routes
        composable(Routes.PROJECTS) {
            val activity = LocalContext.current as Activity
            ProjectsScreen(
                onNavigateToProjectDetail = { projectId ->
                    InterstitialManager.maybeShow(activity, activity.getString(R.string.admob_interstitial_id)) {
                        navController.navigate(Routes.createProjectDetailRoute(projectId))
                    }
                }
            )
        }
        composable(Routes.PROJECT_DETAIL) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            ProjectDetailScreen(
                projectId = projectId,
                onNavigateToIntentionList = { projId, intentionType ->
                    navController.navigate(Routes.createIntentionListRoute(projId, intentionType))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.INTENTION_LIST) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val intentionType = backStackEntry.arguments?.getString("intentionType") ?: ""

            val intentionTitle = when (intentionType) {
                IntentionKeys.INVESTORS -> stringResource(R.string.intention_investors)
                IntentionKeys.DONORS -> stringResource(R.string.intention_donors)
                IntentionKeys.ADVERTISERS -> stringResource(R.string.intention_advertisers)
                else -> intentionType
            }

            IntentionListScreen(
                title = stringResource(R.string.intention_list_title_format, intentionTitle),
                projectId = projectId,
                intentionType = intentionType,
                onNavigateToPublicProfile = { userId ->
                    navController.navigate(Routes.createPublicProfileRoute(userId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.PUBLIC_PROFILE) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            PublicProfileScreen(
                userId = userId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

fun NavHostController.navigateAndClearStack(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { inclusive = true }
        launchSingleTop = true
        restoreState = false
    }
}
