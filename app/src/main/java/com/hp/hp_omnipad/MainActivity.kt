package com.hp.hp_omnipad

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.hp.hp_omnipad.navigation.AppNavGraph
import com.hp.hp_omnipad.ui.home.settings.SettingsViewModel
import com.hp.hp_omnipad.ui.theme.HpOmnipadTheme
import com.hp.hp_omnipad.ui.theme.ThemeViewModel
import com.hp.hp_omnipad.utils.VideoSyncManager
import com.hp.hp_omnipad.utils.WebViewSecurity
import java.util.Locale
import android.view.KeyEvent

class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()
    
    var onNotificationPermissionResult: ((Boolean) -> Unit)? = null

    private val requestNotificationPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            onNotificationPermissionResult?.invoke(isGranted)
            onNotificationPermissionResult = null
        }
    
    override fun attachBaseContext(newBase: Context) {
        val languageCode = SettingsViewModel.getLanguageCode(newBase)
        val context = updateLocale(newBase, languageCode)
        super.attachBaseContext(context)
    }
    
    private fun isAllowedLaunchIntent(intent: Intent): Boolean {
        val action = intent.action ?: return true
        return action == Intent.ACTION_MAIN &&
            intent.categories?.contains(Intent.CATEGORY_LAUNCHER) == true
    }

    private fun updateLocale(context: Context, languageCode: String): Context {
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return context.createConfigurationContext(config)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null && !isAllowedLaunchIntent(intent)) {
            finish()
            return
        }

        installSplashScreen()

        super.onCreate(savedInstanceState)

        WebViewSecurity.initialize()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setHideOverlayWindows(true)
        }

        askNotificationPermission()

        setContent {

            val isDark by themeViewModel.isDarkTheme.collectAsState()
            val navController = rememberNavController()

            HpOmnipadTheme(
                darkTheme = isDark,
                dynamicColor = false
            ) {
                AppNavGraph(
                    navController = navController,
                    themeViewModel = themeViewModel
                )
            }
        }
    }

    fun askNotificationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            val permissionState = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )

            if (permissionState != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }
    
    fun requestNotificationPermissionWithCallback(onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionState = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
            
            if (permissionState == PackageManager.PERMISSION_GRANTED) {
                onResult(true)
            } else {
                val prefs = getSharedPreferences("hp_omnipad_prefs", Context.MODE_PRIVATE)
                val hasRequestedBefore = prefs.getBoolean("notification_permission_requested", false)
                
                if (!hasRequestedBefore) {
                    prefs.edit { putBoolean("notification_permission_requested", true) }
                    onNotificationPermissionResult = onResult
                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    openAppNotificationSettings()
                    onResult(false)
                }
            }
        } else {
            onResult(true)
        }
    }
    
    fun openAppNotificationSettings() {
        val intent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_TAB) {
            return true // consume Tab key; block focus traversal
        }
        return super.dispatchKeyEvent(event)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up any incomplete downloads when app is closed
        if (isFinishing) {
            Log.d("MainActivity", "🧹 App closing - cleaning up incomplete downloads")
            VideoSyncManager.cleanupCurrentDownload()
        }
    }
}
