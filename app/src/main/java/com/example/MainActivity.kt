package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.model.LanguageManager
import com.example.ui.components.CalyBottomBar
import com.example.ui.components.CalyTopBar
import com.example.ui.components.PaywallModal
import com.example.ui.screens.*
import com.example.ui.theme.CalyTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.CalyViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: CalyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val userProfile by viewModel.userProfile.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()
            val showPaywall by viewModel.showPaywall.collectAsState()

            val isRtl = LanguageManager.isRtl(userProfile.languageCode)
            val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                CalyTheme(darkTheme = userProfile.isDarkMode) {
                    // System Back Navigation Handling
                    BackHandler(enabled = currentScreen != AppScreen.TODAY) {
                        viewModel.navigateBack()
                    }

                    Scaffold(
                        topBar = {
                            CalyTopBar(
                                viewModel = viewModel,
                                userProfile = userProfile,
                                onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) },
                                onOpenBilling = { viewModel.navigateTo(AppScreen.BILLING) }
                            )
                        },
                        bottomBar = {
                            CalyBottomBar(
                                currentScreen = currentScreen,
                                languageCode = userProfile.languageCode,
                                onNavigate = { screen -> viewModel.navigateTo(screen) }
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentScreen) {
                                AppScreen.TODAY -> TodayScreen(
                                    viewModel = viewModel,
                                    userProfile = userProfile,
                                    onNavigate = { viewModel.navigateTo(it) }
                                )
                                AppScreen.CAMERA -> CameraLogScreen(
                                    viewModel = viewModel,
                                    onNavigate = { viewModel.navigateTo(it) }
                                )
                                AppScreen.INSIGHTS -> InsightsScreen(
                                    viewModel = viewModel,
                                    userProfile = userProfile,
                                    onNavigate = { viewModel.navigateTo(it) }
                                )
                                AppScreen.RECIPES -> RecipesScreen(
                                    viewModel = viewModel,
                                    onNavigate = { viewModel.navigateTo(it) }
                                )
                                AppScreen.CALY_CHAT -> CalyChatScreen(
                                    viewModel = viewModel
                                )
                                AppScreen.GLP1_MODE -> Glp1Screen(
                                    viewModel = viewModel,
                                    userProfile = userProfile,
                                    onNavigate = { viewModel.navigateTo(it) }
                                )
                                AppScreen.SETTINGS -> SettingsScreen(
                                    viewModel = viewModel,
                                    userProfile = userProfile,
                                    onNavigate = { viewModel.navigateTo(it) }
                                )
                                AppScreen.BILLING -> BillingScreen(
                                    viewModel = viewModel,
                                    onNavigate = { viewModel.navigateTo(it) }
                                )
                            }
                        }

                        // Paywall Dialog on 4th meal limit
                        if (showPaywall) {
                            PaywallModal(
                                onDismiss = { viewModel.dismissPaywall() },
                                onUpgrade = { viewModel.upgradeToPremium() },
                                onOpenBilling = { viewModel.navigateTo(AppScreen.BILLING) }
                            )
                        }
                    }
                }
            }
        }
    }
}
