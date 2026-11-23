package com.getfit.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.getfit.ui.components.ThemeCapsuleToggle
import com.getfit.ui.components.core.GfBottomBar
import com.getfit.ui.components.core.GfBottomNavItem
import com.getfit.ui.screens.*
import com.getfit.ui.theme.Dimens
import com.getfit.ui.theme.GetFitTheme
import com.getfit.ui.util.AuthPreferences
import com.getfit.ui.util.OnboardingPreferences
import com.getfit.ui.util.ThemePreferences
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

@Composable
fun GetFitNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
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

    val foodViewModel: FoodViewModel = viewModel()
    val workoutViewModel: WorkoutViewModel = viewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Check if bottom bar should be visible
    val isBottomBarVisible = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (isBottomBarVisible) {
                CleanTopHeader(
                    currentRoute = currentRoute,
                    onNavigateToAnalytics = {
                        navController.navigate(Screen.Analytics.route)
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize()
            ) {
            composable(Screen.OnboardingWelcome.route) {
                OnboardingWelcomeScreen(
                    onNext = {
                        navController.navigate(Screen.Auth.route)
                    }
                )
            }

            composable(Screen.Auth.route) {
                val profileViewModel: ProfileViewModel = viewModel()
                val canGoBack = navController.previousBackStackEntry != null && 
                    navController.previousBackStackEntry?.destination?.route != Screen.OnboardingWelcome.route
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

            composable(Screen.OnboardingProfile.route) {
                val profileViewModel: ProfileViewModel = viewModel()
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

            composable(Screen.Dashboard.route) {
                val dashboardViewModel: DashboardViewModel = viewModel()
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

            composable(Screen.Analytics.route) {
                val analyticsViewModel: AnalyticsViewModel = viewModel()
                AnalyticsScreen(
                    viewModel = analyticsViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Food.route) {
                FoodScreen(
                    viewModel = foodViewModel,
                    onNavigateToAddCustomFood = {
                        navController.navigate(Screen.AddCustomFood.route)
                    }
                )
            }

            composable(Screen.AddCustomFood.route) {
                AddCustomFoodScreen(
                    viewModel = foodViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Workout.route) {
                WorkoutScreen(
                    viewModel = workoutViewModel,
                    onNavigateToWorkoutDetail = { id, name, muscle, sets, reps, dayName ->
                        navController.navigate(
                            Screen.WorkoutDetail.createRoute(id, name, muscle, sets, reps, dayName)
                        )
                    }
                )
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
                val workoutViewModel: WorkoutViewModel = viewModel()
                val exerciseId = backStackEntry.arguments?.getLong("exerciseId") ?: 0L
                val exerciseNameRaw = backStackEntry.arguments?.getString("exerciseName") ?: ""
                val muscleGroupRaw = backStackEntry.arguments?.getString("muscleGroup") ?: ""
                val sets = backStackEntry.arguments?.getInt("sets") ?: 4
                val repsRaw = backStackEntry.arguments?.getString("reps") ?: "8-10"
                val dayNameRaw = backStackEntry.arguments?.getString("dayName") ?: "Workout"

                val exerciseName = URLDecoder.decode(exerciseNameRaw, StandardCharsets.UTF_8.toString())
                val muscleGroup = URLDecoder.decode(muscleGroupRaw, StandardCharsets.UTF_8.toString())
                val reps = URLDecoder.decode(repsRaw, StandardCharsets.UTF_8.toString())
                val dayName = URLDecoder.decode(dayNameRaw, StandardCharsets.UTF_8.toString())

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

            composable(Screen.Profile.route) {
                val profileViewModel: ProfileViewModel = viewModel()
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

        if (isBottomBarVisible) {
            // Non-interactive gradient fade: transparent to background, ~140dp tall, extending behind navigation bar inset
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                GetFitTheme.colors.background.copy(alpha = 0.85f),
                                GetFitTheme.colors.background
                            )
                        )
                    )
            )

            val selectedIndex = bottomNavItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
            GfBottomBar(
                items = gfBottomNavItems,
                selectedIndex = selectedIndex,
                onItemSelected = { index ->
                    val screen = bottomNavItems[index]
                    if (screen.route == Screen.Dashboard.route) {
                        if (currentRoute != Screen.Dashboard.route) {
                            val popped = navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                            if (!popped) {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                }
                            }
                        }
                    } else if (screen.route == Screen.Food.route) {
                        foodViewModel.resetToDiary()
                        if (currentRoute != Screen.Food.route) {
                            navController.navigate(Screen.Food.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    } else if (screen.route == Screen.Workout.route) {
                        workoutViewModel.resetToWorkoutTab()
                        if (currentRoute != Screen.Workout.route) {
                            navController.navigate(Screen.Workout.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    } else if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
}

@Composable
private fun CleanTopHeader(
    currentRoute: String?,
    onNavigateToAnalytics: () -> Unit
) {
    val title = when (currentRoute) {
        Screen.Food.route -> "Nutrition"
        Screen.Workout.route -> "Training"
        Screen.Profile.route -> "Profile"
        else -> "Home"
    }

    Surface(
        color = GetFitTheme.colors.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.screenGutter, vertical = Dimens.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentRoute == Screen.Dashboard.route || currentRoute == null) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_getfit_logo),
                        contentDescription = "GetFit",
                        colorFilter = ColorFilter.tint(GetFitTheme.colors.textPrimary),
                        modifier = Modifier.height(28.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GetFitTheme.colors.textPrimary
                    )
                }

                // Secondary Analytics Button
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = GetFitTheme.colors.surface2,
                    border = BorderStroke(1.dp, GetFitTheme.colors.hairline),
                    modifier = Modifier.clickable { onNavigateToAnalytics() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Dimens.md, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.xs)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = "Analytics",
                            tint = GetFitTheme.colors.textPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Analytics",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = GetFitTheme.colors.textPrimary
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(GetFitTheme.colors.hairline)
            )
        }
    }
}
