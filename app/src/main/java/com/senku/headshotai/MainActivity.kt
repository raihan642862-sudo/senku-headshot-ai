package com.senku.headshotai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.senku.headshotai.ui.screens.DashboardScreen
import com.senku.headshotai.ui.screens.PerformanceScreen
import com.senku.headshotai.ui.screens.SensitivityScreen
import com.senku.headshotai.ui.screens.SettingsCoachScreen
import com.senku.headshotai.ui.screens.TrainingScreen
import com.senku.headshotai.ui.theme.SenkuCharcoalElevated
import com.senku.headshotai.ui.theme.SenkuCyan
import com.senku.headshotai.ui.theme.SenkuDeepSurface
import com.senku.headshotai.ui.theme.SenkuHeadshotAITheme
import com.senku.headshotai.ui.theme.SenkuNearBlack
import com.senku.headshotai.ui.theme.SenkuTextPrimary
import com.senku.headshotai.ui.theme.SenkuTextSecondary
import com.senku.headshotai.ui.viewmodel.SenkuMainViewModel

enum class SenkuNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Default.Dashboard),
    SENSITIVITY("sensitivity", "AI Sens", Icons.Default.Tune),
    TRAINING("training", "Training", Icons.Default.GpsFixed),
    PERFORMANCE("performance", "Performance", Icons.Default.Speed),
    SETTINGS("settings", "Settings", Icons.Default.Psychology)
}

class MainActivity : ComponentActivity() {

    private val viewModel: SenkuMainViewModel by viewModels {
        val app = application as SenkuApplication
        SenkuMainViewModel.Factory(
            application = app,
            dao = app.database.senkuDao(),
            prefsRepo = app.preferencesRepository,
            hardwareMonitor = app.hardwareMonitor
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SenkuHeadshotAITheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                SenkuMainAppScaffold(
                    state = state,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun SenkuMainAppScaffold(
    state: com.senku.headshotai.ui.viewmodel.SenkuUiState,
    viewModel: SenkuMainViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: SenkuNavDestination.HOME.route

    fun navigateToTab(dest: SenkuNavDestination) {
        navController.navigate(dest.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(SenkuNearBlack),
        containerColor = SenkuNearBlack,
        bottomBar = {
            NavigationBar(
                containerColor = SenkuDeepSurface,
                tonalElevation = 8.dp
            ) {
                SenkuNavDestination.entries.forEach { dest ->
                    val selected = currentRoute == dest.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navigateToTab(dest) },
                        icon = {
                            Icon(
                                imageVector = dest.icon,
                                contentDescription = dest.label
                            )
                        },
                        label = {
                            Text(
                                text = dest.label,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SenkuCyan,
                            selectedTextColor = SenkuCyan,
                            indicatorColor = SenkuCharcoalElevated,
                            unselectedIconColor = SenkuTextSecondary,
                            unselectedTextColor = SenkuTextSecondary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = SenkuNavDestination.HOME.route
            ) {
                composable(SenkuNavDestination.HOME.route) {
                    DashboardScreen(
                        state = state,
                        onNavigateToSensitivity = { navigateToTab(SenkuNavDestination.SENSITIVITY) },
                        onNavigateToTraining = { navigateToTab(SenkuNavDestination.TRAINING) },
                        onNavigateToPerformance = { navigateToTab(SenkuNavDestination.PERFORMANCE) },
                        onLoadProfile = viewModel::loadProfile,
                        onDeleteProfile = viewModel::deleteProfile
                    )
                }
                composable(SenkuNavDestination.SENSITIVITY.route) {
                    SensitivityScreen(
                        state = state,
                        onSelectPlaystyle = viewModel::selectPlaystyle,
                        onSelectWeaponClass = viewModel::selectWeaponClass,
                        onGenerateAiSensitivity = viewModel::generateAiRecommendedSensitivity,
                        onUpdateManualSensitivity = viewModel::updateManualSensitivity,
                        onApplyAiCoachingDelta = viewModel::applyAiCoachingAdjustments,
                        onSaveProfile = viewModel::saveCurrentProfileToDatabase
                    )
                }
                composable(SenkuNavDestination.TRAINING.route) {
                    TrainingScreen(
                        state = state,
                        onRecordSession = viewModel::recordTrainingSession,
                        onClearHistory = viewModel::clearTrainingHistory
                    )
                }
                composable(SenkuNavDestination.PERFORMANCE.route) {
                    PerformanceScreen(
                        state = state,
                        onRefreshTelemetry = viewModel::refreshPerformanceNow
                    )
                }
                composable(SenkuNavDestination.SETTINGS.route) {
                    SettingsCoachScreen(
                        state = state,
                        onApplyAiCoachingDelta = viewModel::applyAiCoachingAdjustments,
                        onToggleRealme12xProfile = viewModel::setForceRealme12xOptimization,
                        onToggleHaptics = viewModel::setHapticFeedback,
                        onChangeTargetSpeedPercent = viewModel::setTargetSpeedPercent
                    )
                }
            }

            // Transient Status Toast Banner
            AnimatedVisibility(
                visible = state.statusBannerMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SenkuCharcoalElevated)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = state.statusBannerMessage.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = SenkuCyan
                    )
                }
            }
        }
    }
}
