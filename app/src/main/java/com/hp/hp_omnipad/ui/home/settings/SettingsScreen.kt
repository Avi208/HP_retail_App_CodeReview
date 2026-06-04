package com.hp.hp_omnipad.ui.home.settings

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hp.hp_omnipad.MainActivity
import com.hp.hp_omnipad.R
import com.hp.hp_omnipad.ui.home.settings.components.*
import com.hp.hp_omnipad.ui.theme.ThemeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun SettingsScreen(
    themeViewModel: ThemeViewModel,
    viewModel: SettingsViewModel = viewModel(),
    onPrivacyClick: () -> Unit = {}
) {

    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val state by viewModel.uiState
    val isDark by themeViewModel.isDarkTheme.collectAsState()
    val languageChanged by viewModel.languageChanged.collectAsStateWithLifecycle()
    
    var showBatteryWarningDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Pull to refresh state
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                viewModel.refresh()
                delay(300) // Brief delay for visual feedback
                isRefreshing = false
            }
        }
    )
    
    // Recreate activity when language changes
    LaunchedEffect(languageChanged) {
        if (languageChanged) {
            viewModel.resetLanguageChangedFlag()
            activity?.recreate()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {

            Spacer(modifier = Modifier.height(24.dp))

            // Header
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.manage_preferences),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Preferences Section (First)
            Text(
                text = stringResource(R.string.preferences),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            PreferencesCard(
                state = state,
                isDark = isDark,

                onToggleNotifications = {
                    if (!state.notificationsEnabled) {
                        // Turning ON - request permission
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            (activity as MainActivity).requestNotificationPermissionWithCallback { granted ->
                                if (granted) {
                                    viewModel.toggleNotifications()
                                }
                            }
                        } else {
                            viewModel.toggleNotifications()
                        }
                    } else {
                        // Turning OFF
                        viewModel.toggleNotifications()
                    }
                },

                onLanguageChange = { viewModel.changeLanguage(it) },

                onThemeChange = { dark ->
                    themeViewModel.setDarkTheme(dark)
                },

                onToggleStayAwake = {
                    if (!state.stayAwakeEnabled) {
                        showBatteryWarningDialog = true
                    } else {
                        viewModel.toggleStayAwake()
                    }
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Security Section
            SecurityRow()

            Spacer(modifier = Modifier.height(28.dp))

            // Developer Settings Section
            Text(
                text = stringResource(R.string.security_settings), // fallback or use specialized string if available
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            DeveloperSettingsCard(
                offloadEnabled = state.offloadAppEnabled,
                onToggleOffload = { enableOffload ->
                    if (enableOffload) {
                        viewModel.enableOffloadApp()
                    } else {
                        viewModel.disableOffloadAndDeleteVideos()
                    }
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // App Info Card (Privacy Policy)
            AppInfoCard(
                onPrivacyClick = onPrivacyClick
            )

            // Logout button commented out for now
            // Spacer(modifier = Modifier.height(28.dp))
            // LogoutButton()

            Spacer(modifier = Modifier.height(40.dp))
            }
            
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
    
    // Battery Warning Dialog
    if (showBatteryWarningDialog) {
        com.hp.hp_omnipad.ui.components.PremiumConfirmationDialog(
            icon = Icons.Default.BatteryAlert,
            iconColor = MaterialTheme.colorScheme.error,
            title = stringResource(R.string.battery_warning),
            message = stringResource(R.string.battery_warning_message),
            confirmText = stringResource(R.string.enable_anyway),
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                viewModel.toggleStayAwake()
                showBatteryWarningDialog = false
            },
            onDismiss = { showBatteryWarningDialog = false }
        )
    }
}