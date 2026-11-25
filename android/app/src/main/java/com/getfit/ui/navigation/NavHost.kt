package com.getfit.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.getfit.R
import com.getfit.ui.components.core.GfBottomBar
import com.getfit.ui.components.core.GfBottomFade
import com.getfit.ui.components.core.GfBottomNavItem
import com.getfit.ui.components.core.GfScreenSurface
import com.getfit.ui.components.core.TabScreenFrame
import com.getfit.ui.components.core.pressable
import com.getfit.ui.screens.*
import com.getfit.ui.theme.Dimens
import com.getfit.ui.theme.GetFitTheme
import com.getfit.ui.util.AuthPreferences
import com.getfit.ui.util.OnboardingPreferences
import com.getfit.ui.viewmodel.*
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Dashboard : Screen("dashboard", "Home", Icons.Default.Home)
    object Food : Screen("food", "Nutrition", Icons.Default.Restaurant)
    object Workout : Screen("workout", "Training", Icons.Default.FitnessCenter)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Analytics : Screen("analytics", "Analytics", Icons.AutoMirrored.Filled.TrendingUp)

    object Auth : Screen("auth", "Sign In")
    object OnboardingWelcome : Screen("onboarding_welcome", "Welcome")
    object OnboardingProfile : Screen("onboarding_profile", "Setup Profile")

    object AddCustomFood : Screen("add_custom_food", "Add Food")

    object WorkoutDetail : Screen(
        "workout_detail/{exerciseId}/{exerciseName}/{muscleGroup}/{sets}/{reps}/{dayName}",
        "Workout Detail"
    ) {
        fun createRoute(
            exerciseId: Long,
            exerciseName: String,
            muscleGroup: String,
            sets: Int,
            reps: String,
            dayName: String
        ): String {
            val encodedName = URLEncoder.encode(exerciseName, StandardCharsets.UTF_8.toString())
            val encodedMuscle = URLEncoder.encode(muscleGroup, StandardCharsets.UTF_8.toString())
            val encodedReps = URLEncoder.encode(reps, StandardCharsets.UTF_8.toString())
            val encodedDay = URLEncoder.encode(dayName, StandardCharsets.UTF_8.toString())
            return "workout_detail/$exerciseId/$encodedName/$encodedMuscle/$sets/$encodedReps/$encodedDay"
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GetFitNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        com.getfit.ui.util.HapticsPreferences.init(context)
    }
    val hapticsEnabled by com.getfit.ui.util.HapticsPreferences.hapticsEnabled.collectAsState()
    val defaultHaptics = LocalHapticFeedback.current
    val resolvedHaptics = remember(hapticsEnabled, defaultHaptics) {
        if (hapticsEnabled) defaultHaptics else object : androidx.compose.ui.hapticfeedback.HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {}
        }
    }

    val startDestination = remember {
        if (AuthPreferences.isLoggedIn(context) || OnboardingPreferences.hasCompletedOnboarding(context)) {
            Screen.Dashboard.route
        } else {
            Screen.OnboardingWelcome.route
        }
    }

    val bottomNavItems = listOf(
        Screen.Dashboard,
        Screen.Food,
        Screen.Workout,
        Screen.Profile
    )

    val gfBottomNavItems = remember {
        listOf(
            GfBottomNavItem(Screen.Dashboard.route, "Home", Icons.Default.Home),
            GfBottomNavItem(Screen.Food.route, "Nutrition", Icons.Default.Restaurant),
            GfBottomNavItem(Screen.Workout.route, "Training", Icons.Default.FitnessCenter),
            GfBottomNavItem(Screen.Profile.route, "Profile", Icons.Default.Person)
        )
    }

    val dashboardViewModel: DashboardViewModel = viewModel()
    val foodViewModel: FoodViewModel = viewModel()
    val workoutViewModel: WorkoutViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val analyticsViewModel: AnalyticsViewModel = viewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Check if bottom bar should be visible
    val isBottomBarVisible = remember(currentRoute) { bottomNavItems.any { it.route == currentRoute } }
    val tabRoutes = remember {
        setOf(Screen.Dashboard.route, Screen.Food.route, Screen.Workout.route, Screen.Profile.route)
    }
    val pushedRoutes = remember {
        setOf(Screen.Analytics.route, Screen.WorkoutDetail.route, Screen.AddCustomFood.route, Screen.Auth.route)
    }

    val focusManager = LocalFocusManager.current
    androidx.compose.runtime.CompositionLocalProvider(LocalHapticFeedback provides resolvedHaptics) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GetFitTheme.colors.background)
            .imePadding()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = {
                val target = targetState.destination.route
                if (target in pushedRoutes) {
                    fadeIn(tween(120, easing = LinearOutSlowInEasing)) +
                        slideInHorizontally(tween(240, easing = FastOutSlowInEasing)) { (it * 0.10f).toInt() }
                } else {
                    fadeIn(tween(100, delayMillis = 50, easing = LinearOutSlowInEasing))
                }
            },
            exitTransition = {
                val initial = initialState.destination.route
                val target = targetState.destination.route
                if (initial in tabRoutes && target in pushedRoutes) {
                    ExitTransition.None
                } else {
                    fadeOut(tween(50, easing = FastOutLinearInEasing))
                }
            },
            popEnterTransition = {
                val initial = initialState.destination.route
                val target = targetState.destination.route
                if (initial in pushedRoutes && target in tabRoutes) {
                    EnterTransition.None
                } else {
                    fadeIn(tween(100, delayMillis = 50, easing = LinearOutSlowInEasing))
                }
            },
            popExitTransition = {
                val initial = initialState.destination.route
                if (initial in pushedRoutes) {
                    fadeOut(tween(110)) +
                        slideOutHorizontally(tween(200, easing = FastOutLinearInEasing)) { (it * 0.10f).toInt() }
                } else {
                    fadeOut(tween(50, easing = FastOutLinearInEasing))
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.OnboardingWelcome.route) {
                GfScreenSurface {
                    OnboardingWelcomeScreen(
                        onNext = {
                            navController.navigate(Screen.Auth.route)
                        }
                    )
                }
            }

            composable(Screen.Auth.route) {
                val canGoBack = navController.previousBackStackEntry != null && 
                    navController.previousBackStackEntry?.destination?.route != Screen.OnboardingWelcome.route
                GfScreenSurface {
                    AuthScreen(
                        viewModel = profileViewModel,
                        onAuthSuccess = { isNewUser ->
                            if (isNewUser) {
                                navController.navigate(Screen.OnboardingProfile.route) {
                                    popUpTo(Screen.OnboardingWelcome.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.OnboardingWelcome.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                        onContinueWithoutSignIn = {
                            navController.navigate(Screen.OnboardingProfile.route) {
                                popUpTo(Screen.Auth.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onNavigateBack = if (canGoBack) {
                            { navController.popBackStack() }
                        } else null
                    )
                }
            }

            composable(Screen.OnboardingProfile.route) {
                GfScreenSurface {
                    OnboardingProfileScreen(
                        viewModel = profileViewModel,
                        onSaveAndContinue = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.OnboardingWelcome.route) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }

            composable(Screen.Dashboard.route) {
                GfScreenSurface {
                    TabScreenFrame(
                        route = Screen.Dashboard.route,
                        onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) }
                    ) {
                        DashboardScreen(
                            viewModel = dashboardViewModel,
                            onNavigateToFood = {
                                foodViewModel.resetToDiary()
                                navController.navigate(Screen.Food.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onNavigateToHydration = {
                                foodViewModel.navigateToHydration()
                                navController.navigate(Screen.Food.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onNavigateToWorkout = {
                                workoutViewModel.resetToWorkoutTab()
                                navController.navigate(Screen.Workout.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }

            composable(route = Screen.Analytics.route) {
                val isTransitionSettled = transition.currentState == transition.targetState
                GfScreenSurface {
                    AnalyticsScreen(
                        viewModel = analyticsViewModel,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        isTransitionSettled = isTransitionSettled
                    )
                }
            }

            composable(Screen.Food.route) {
                GfScreenSurface {
                    TabScreenFrame(
                        route = Screen.Food.route,
                        onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) }
                    ) {
                        FoodScreen(
                            viewModel = foodViewModel,
                            onNavigateToAddCustomFood = {
                                navController.navigate(Screen.AddCustomFood.route)
                            },
                            onNavigateToAuth = {
                                navController.navigate(Screen.Auth.route)
                            }
                        )
                    }
                }
            }

            composable(route = Screen.AddCustomFood.route) {
                GfScreenSurface {
                    AddCustomFoodScreen(
                        viewModel = foodViewModel,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }

            composable(Screen.Workout.route) {
                GfScreenSurface {
                    TabScreenFrame(
                        route = Screen.Workout.route,
                        onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) }
                    ) {
                        WorkoutScreen(
                            viewModel = workoutViewModel,
                            onNavigateToWorkoutDetail = { id, name, muscle, sets, reps, dayName ->
                                navController.navigate(
                                    Screen.WorkoutDetail.createRoute(id, name, muscle, sets, reps, dayName)
                                )
                            }
                        )
                    }
                }
            }

            composable(
                route = Screen.WorkoutDetail.route,
                arguments = listOf(
                    navArgument("exerciseId") { type = NavType.LongType },
                    navArgument("exerciseName") { type = NavType.StringType },
                    navArgument("muscleGroup") { type = NavType.StringType },
                    navArgument("sets") { type = NavType.IntType },
                    navArgument("reps") { type = NavType.StringType },
                    navArgument("dayName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val exerciseId = backStackEntry.arguments?.getLong("exerciseId") ?: 0L
                val exerciseNameRaw = backStackEntry.arguments?.getString("exerciseName") ?: ""
                val muscleGroupRaw = backStackEntry.arguments?.getString("muscleGroup") ?: ""
                val sets = backStackEntry.arguments?.getInt("sets") ?: 3
                val repsRaw = backStackEntry.arguments?.getString("reps") ?: "8-10"
                val dayNameRaw = backStackEntry.arguments?.getString("dayName") ?: "Workout"

                val exerciseName = URLDecoder.decode(exerciseNameRaw, StandardCharsets.UTF_8.toString())
                val muscleGroup = URLDecoder.decode(muscleGroupRaw, StandardCharsets.UTF_8.toString())
                val reps = URLDecoder.decode(repsRaw, StandardCharsets.UTF_8.toString())
                val dayName = URLDecoder.decode(dayNameRaw, StandardCharsets.UTF_8.toString())

                GfScreenSurface {
                    WorkoutDetailScreen(
                        exerciseId = exerciseId,
                        exerciseName = exerciseName,
                        muscleGroup = muscleGroup,
                        targetSets = sets,
                        targetReps = reps,
                        workoutPlanDay = dayName,
                        viewModel = workoutViewModel,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }

            composable(Screen.Profile.route) {
                GfScreenSurface {
                    TabScreenFrame(
                        route = Screen.Profile.route,
                        onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) }
                    ) {
                        ProfileScreen(
                            viewModel = profileViewModel,
                            onNavigateToAuth = {
                                navController.navigate(Screen.Auth.route)
                            },
                            onLogout = {
                                navController.navigate(Screen.OnboardingWelcome.route) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isBottomBarVisible,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(80)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                GfBottomFade(modifier = Modifier.align(Alignment.BottomCenter))

                val haptic = LocalHapticFeedback.current
                val routeIndex = bottomNavItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
                var pendingIndex by remember { mutableStateOf<Int?>(null) }
                LaunchedEffect(currentRoute) {
                    if (pendingIndex != null && routeIndex == pendingIndex) {
                        pendingIndex = null
                    }
                }
                val selectedIndex = pendingIndex ?: routeIndex

                GfBottomBar(
                    items = gfBottomNavItems,
                    selectedIndex = selectedIndex,
                    onItemSelected = { index ->
                        val screen = bottomNavItems[index]
                        if (index == selectedIndex || currentRoute == screen.route) return@GfBottomBar
                        pendingIndex = index
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        when (screen) {
                            Screen.Food -> foodViewModel.resetToDiary()
                            Screen.Workout -> workoutViewModel.resetToWorkoutTab()
                            else -> Unit
                        }
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Dashboard.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
    }
}
