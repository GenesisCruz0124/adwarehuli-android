package com.genesiscruz.adwarehuli.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.genesiscruz.adwarehuli.data.pm.PermissionChecker
import com.genesiscruz.adwarehuli.ui.appdetail.AppDetailScreen
import com.genesiscruz.adwarehuli.ui.dashboard.DashboardScreen
import com.genesiscruz.adwarehuli.ui.monitor.MonitorScreen
import com.genesiscruz.adwarehuli.ui.onboarding.OnboardingScreen
import com.genesiscruz.adwarehuli.ui.scanner.ScannerScreen

@Composable
fun AppNavGraph(permissionChecker: PermissionChecker, navController: NavHostController = rememberNavController()) {
    val startDestination = if (permissionChecker.canScan()) Routes.DASHBOARD else Routes.ONBOARDING

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onContinue = {
                navController.navigate(Routes.DASHBOARD) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            })
        }
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onRunScan = { navController.navigate(Routes.SCANNER) },
                onOpenMonitor = { navController.navigate(Routes.MONITOR) },
                onOpenAppDetail = { pkg -> navController.navigate(Routes.appDetail(pkg)) }
            )
        }
        composable(Routes.MONITOR) {
            MonitorScreen(onOpenAppDetail = { pkg -> navController.navigate(Routes.appDetail(pkg)) })
        }
        composable(Routes.SCANNER) {
            ScannerScreen(onOpenAppDetail = { pkg -> navController.navigate(Routes.appDetail(pkg)) })
        }
        composable(
            route = Routes.APP_DETAIL,
            arguments = listOf(navArgument("packageName") { type = NavType.StringType })
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName").orEmpty()
            AppDetailScreen(packageName = packageName, onBack = { navController.popBackStack() })
        }
    }
}
