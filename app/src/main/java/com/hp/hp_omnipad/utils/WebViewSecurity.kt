package com.hp.hp_omnipad.utils

import android.webkit.WebView
import com.hp.hp_omnipad.BuildConfig

/**
 * Application-wide WebView hardening invoked from the main entry activity (CWE-693).
 */
object WebViewSecurity {

    fun initialize() {
        if (!BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(false)
        }
    }
}
