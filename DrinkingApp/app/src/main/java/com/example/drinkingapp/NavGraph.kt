package com.example.drinkingapp

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    gameRoomViewModel: GameRoomViewModel
){

    NavHost(
        navController = navController,
        startDestination = Screen.GetStarted.route
    ){
        composable(
            route = Screen.GetStarted.route
        ){
            GetStartedScreen(navController, gameRoomViewModel)
        }

        composable(
            route = Screen.GameMode.route + "/{username}",
            arguments = listOf(
                navArgument("username") {
                    type = NavType.StringType
                }
            )
        ){ entry ->
            val username = entry.arguments?.getString("username") ?: ""
            GameModeSelectionScreen(navController, gameRoomViewModel, username)
        }

        composable(
            route = Screen.LobbyHost.route + "/{lobbyKey}/{username}",
            arguments = listOf(
                navArgument("lobbyKey") {
                    type = NavType.StringType
                },
                navArgument("username") {
                    type = NavType.StringType
                }
            )
        ){ entry ->
            val lobbyKey = entry.arguments?.getString("lobbyKey") ?: ""
            val username = entry.arguments?.getString("username") ?: ""
            LobbyHostScreen(navController, gameRoomViewModel, lobbyKey, username)
        }

        composable(
            route = Screen.LobbyGuest.route + "/{lobbyKey}/{username}",
            arguments = listOf(
                navArgument("lobbyKey") {
                    type = NavType.StringType
                },
                navArgument("username") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            val lobbyKey = entry.arguments?.getString("lobbyKey") ?: ""
            val username = entry.arguments?.getString("username") ?: ""
            LobbyGuestScreen(navController, gameRoomViewModel, lobbyKey, username)
        }

        composable(
            route = Screen.CreatePrompt.route + "/{lobbyKey}",
            arguments = listOf(
                navArgument("lobbyKey") {
                    type = NavType.StringType
                }
            )
        ){ entry ->
            val lobbyKey = entry.arguments?.getString("lobbyKey") ?: ""
            CreatePrompt(navController, gameRoomViewModel, lobbyKey)
        }

        composable(
            route = Screen.Waiting.route + "/{lobbyKey}",
            arguments = listOf(
                navArgument("lobbyKey") {
                    type = NavType.StringType
                }
            )
        ){ entry ->
            val lobbyKey = entry.arguments?.getString("lobbyKey") ?: ""
            WaitingScreen(navController, gameRoomViewModel, lobbyKey)
        }

        composable(
            route = Screen.Questions.route + "/{lobbyKey}",
            arguments = listOf(
                navArgument("lobbyKey") {
                    type = NavType.StringType
                }
            )
        ){ entry ->
            val lobbyKey = entry.arguments?.getString("lobbyKey") ?: ""
            QuestionScreen(navController, gameRoomViewModel, lobbyKey)
        }

        composable(
            route = Screen.Result.route + "/{lobbyKey}",
            arguments = listOf(
                navArgument("lobbyKey") {
                    type = NavType.StringType
                }
            )
        ){ entry ->
            val lobbyKey = entry.arguments?.getString("lobbyKey") ?: ""
            ResultScreen(navController, gameRoomViewModel, lobbyKey)
        }


    }
    /*
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ){
        composable(
            route = Screen.Home.route
        ){
            HomeScreen(navController)
        }
        composable(
            route = "CreateScreen"
        ){
            CreateScreen(navController)
        }
        composable(
            route = "DetailScreen&userId={userId}",
            arguments = listOf(
                navArgument("userId") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ){ entry ->
            DetailScreen(navController, entry.arguments?.getInt("userId"))
        }
        composable(
            route = "DeleteScreen&userId={userId}",
            arguments = listOf(
                navArgument("userId") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ){ entry ->
            DeleteScreen(navController, entry.arguments?.getInt("userId"))
        }
        composable(
            route = "UpdateScreen&userId={userId}",
            arguments = listOf(
                navArgument("userId") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ){ entry ->
            UpdateScreen(navController, entry.arguments?.getInt("userId"))
        }
    }
     */
}