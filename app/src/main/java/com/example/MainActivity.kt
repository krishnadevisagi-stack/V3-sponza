package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AdViewModel
import com.example.viewmodel.WalletViewModel
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    
    private val viewModel: AdViewModel by viewModels()
    private val walletViewModel: WalletViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MobileAds.initialize(this) {}
        setContent {
            MyApplicationTheme {
                val securityStatus by viewModel.securityStatus.collectAsState()

                if (securityStatus != null && !securityStatus!!.isSecure()) {
                    SecurityBlockScreen(
                        threats = securityStatus!!,
                        onRescan = { viewModel.runSecurityCheck() }
                    )
                } else {
                    NavigationHost(viewModel = viewModel, walletViewModel = walletViewModel)
                }
            }
        }
    }
}

@Composable
fun NavigationHost(
    viewModel: AdViewModel,
    walletViewModel: WalletViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val userProfile by viewModel.userProfile.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = modifier
    ) {
        composable("splash") {
            SplashScreen(
                viewModel = viewModel,
                onNavigateNext = { targetRoute ->
                    navController.navigate(targetRoute) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            LoginScreen(
                viewModel = viewModel,
                onNavigateToInterests = { username, email ->
                    navController.navigate("interests/$username/$email")
                }
            )
        }

        composable(
            route = "interests/{username}/{email}",
            arguments = listOf(
                navArgument("username") { type = NavType.StringType },
                navArgument("email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            val email = backStackEntry.arguments?.getString("email") ?: ""
            InterestSelectionScreen(
                username = username,
                email = email,
                viewModel = viewModel,
                onComplete = {
                    navController.navigate("disclaimer") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("disclaimer") {
            DisclaimerScreen(
                viewModel = viewModel,
                onProceed = {
                    navController.navigate("main") {
                        popUpTo("disclaimer") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            // If user somehow gets logged out, redirect them automatically
            val loggedIn = userProfile?.isLoggedIn ?: false
            LaunchedEffect(loggedIn) {
                if (userProfile != null && !loggedIn) {
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            }
            MainScreenContainer(viewModel = viewModel, walletViewModel = walletViewModel)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreenContainer(
    viewModel: AdViewModel,
    walletViewModel: WalletViewModel
) {
    var selectedTab by remember { mutableStateOf("home") }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("main_bottom_nav_bar")
            ) {
                // Tab 1: Home
                NavigationBarItem(
                    selected = selectedTab == "home",
                    onClick = { selectedTab = "home" },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == "home") Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_home")
                )

                // Tab 2: Reels
                NavigationBarItem(
                    selected = selectedTab == "reels",
                    onClick = { selectedTab = "reels" },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == "reels") Icons.Filled.Movie else Icons.Outlined.Movie,
                            contentDescription = "Reels"
                        )
                    },
                    label = { Text("Reels", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_reels")
                )

                // Tab 3: Profile
                NavigationBarItem(
                    selected = selectedTab == "profile",
                    onClick = { selectedTab = "profile" },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == "profile") Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "Profile"
                        )
                    },
                    label = { Text("Profile", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_profile")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when (selectedTab) {
                "home" -> HomeScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                )
                "reels" -> ReelsScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                )
                "profile" -> ProfileScreen(
                    viewModel = viewModel,
                    walletViewModel = walletViewModel,
                    modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                )
            }
        }
    }
}

