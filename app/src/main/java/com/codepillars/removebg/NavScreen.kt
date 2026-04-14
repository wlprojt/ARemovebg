package com.codepillars.removebg


import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Result : Screen("result")
    object Background : Screen("background")
}

@Composable
fun NavScreen(vm: MainViewModel) {
    val navController = rememberNavController()

    val resultBytes = vm.resultBytes.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            BgRemoverScreen(
                vm = vm,
                onNavigateResult = {
                    navController.navigate(Screen.Result.route) {
                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.Result.route) {
            ResultScreen(
                vm = vm,
                onNext = {
                    navController.navigate(Screen.Background.route)
                }
            )
        }

        composable(Screen.Background.route) {
            AddBackgroundScreen(
                vm = vm
            )
        }
    }
}