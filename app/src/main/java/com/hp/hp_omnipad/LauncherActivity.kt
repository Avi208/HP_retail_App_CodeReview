package com.hp.hp_omnipad

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
/**
 * Exported launcher entry point (CWE-926).
 * [MainActivity] stays non-exported; only validated launcher intents reach the app UI.
 */
class LauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isValidLauncherIntent(intent)) {
            finish()
            return
        }

        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        )
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    private fun isValidLauncherIntent(intent: Intent): Boolean {
        if (intent.action != Intent.ACTION_MAIN) return false
        return intent.categories?.contains(Intent.CATEGORY_LAUNCHER) == true
    }
}
