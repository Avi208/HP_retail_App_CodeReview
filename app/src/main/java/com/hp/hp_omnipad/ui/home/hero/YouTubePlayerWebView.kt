package com.hp.hp_omnipad.ui.home.hero

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import com.hp.hp_omnipad.utils.SecureWebViewClient
import com.hp.hp_omnipad.utils.SecurityResponseHeaders
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayerWebView(
    youtubeUrl: String,
    modifier: Modifier = Modifier,
    autoplay: Boolean = true,
    onVideoEnd: () -> Unit = {},
    restoreFullscreen: Boolean = false
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val videoId = remember(youtubeUrl) { extractYouTubeVideoId(youtubeUrl) }

    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    // remembers if user entered fullscreen
    var wasFullscreen by remember { mutableStateOf(false) }

    val currentOnVideoEnd by rememberUpdatedState(onVideoEnd)

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.apply {
                javaScriptEnabled = true
                mediaPlaybackRequiresUserGesture = false
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                allowContentAccess = true
                allowFileAccess = true
                javaScriptCanOpenWindowsAutomatically = true
                databaseEnabled = true
            }

            addJavascriptInterface(object {

                @JavascriptInterface
                fun onPlayerStateChange(state: Int) {
                    if (state == 0) {
                        post {
                            val shouldRestoreFullscreen = wasFullscreen

                            // close current fullscreen
                            customView = null
                            customViewCallback?.onCustomViewHidden()
                            customViewCallback = null

                            activity?.requestedOrientation =
                                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

                            currentOnVideoEnd()

                            // restore fullscreen after next video loads
                            if (shouldRestoreFullscreen) {
                                wasFullscreen = true
                            }
                        }
                    }
                }

                @JavascriptInterface
                fun onPlayerError(errorCode: Int) {
                    post {
                        currentOnVideoEnd()
                    }
                }

            }, "AndroidInterface")

            webChromeClient = object : WebChromeClient() {

                override fun onShowCustomView(
                    view: View?,
                    callback: CustomViewCallback?
                ) {
                    if (customView != null) {
                        callback?.onCustomViewHidden()
                        return
                    }

                    customView = view
                    customViewCallback = callback
                    wasFullscreen = true

                    activity?.requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

                    // WebView reparents the video surface when entering fullscreen
                    // which causes playback to pause. Resume after the surface settles.
                    postDelayed({
                        evaluateJavascript(
                            "if(player && player.getPlayerState && player.getPlayerState() !== 1) { player.playVideo(); }",
                            null
                        )
                    }, 300)
                }

                override fun onHideCustomView() {
                    if (customView == null) return

                    customView = null
                    customViewCallback?.onCustomViewHidden()
                    customViewCallback = null

                    wasFullscreen = false

                    activity?.requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

                    // Same surface reparenting happens on exit — resume if paused
                    postDelayed({
                        evaluateJavascript(
                            "if(player && player.getPlayerState && player.getPlayerState() !== 1) { player.playVideo(); }",
                            null
                        )
                    }, 300)
                }
            }

            webViewClient = SecureWebViewClient()

            setBackgroundColor(android.graphics.Color.BLACK)
        }
    }

    DisposableEffect(videoId, autoplay) {

        if (videoId != null) {
            val autoplayParam = if (autoplay) "1" else "0"
            val restoreFullscreenParam = if (restoreFullscreen) "true" else "false"

            val embedHtml = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta http-equiv="Strict-Transport-Security"
                        content="max-age=31536000; includeSubDomains">
                    <meta http-equiv="X-Content-Type-Options" content="nosniff">
                    <meta http-equiv="X-Frame-Options" content="SAMEORIGIN">
                    <meta http-equiv="Content-Security-Policy"
                        content="${SecurityResponseHeaders.YOUTUBE_EMBED_CSP}">
                    <meta http-equiv="Referrer-Policy"
                        content="strict-origin-when-cross-origin">
                    <meta http-equiv="X-XSS-Protection" content="1; mode=block">
                    <meta name="viewport"
                        content="width=device-width, initial-scale=1.0">

                    <style>
                        * {
                            margin: 0;
                            padding: 0;
                            box-sizing: border-box;
                        }

                        html, body {
                            width: 100%;
                            height: 100%;
                            background: black;
                            overflow: hidden;
                        }

                        /* Player fills the whole viewport */
                        #player-wrapper {
                            position: relative;
                            width: 100%;
                            height: 100%;
                        }

                        #player {
                            width: 100%;
                            height: 100%;
                        }

                        iframe {
                            width: 100% !important;
                            height: 100% !important;
                        }

                        /*
                         * ─── BRANDING BLOCKER OVERLAYS ──────────────────────────────
                         *
                         * Transparent divs sit above the iframe (z-index: 999) and
                         * silently absorb touch/click events so YouTube branding links
                         * can never fire.  Each zone is sized as a % of the player so
                         * it scales correctly on every screen size / orientation.
                         *
                         * What each zone blocks (see screenshot layout):
                         *
                         *  ┌───────────────────────────────────────────────────────┐
                         *  │ [TOP-INFO: channel avatar + title ← 78% →]  [controls]│ 0–14 %
                         *  │                                                       │
                         *  │              (video content – untouched)              │
                         *  │                                                       │
                         *  │[SHARE] time  ═══ seek bar ═══ [MORE+LOGO] [fullscr.] │ 89–100 %
                         *  └───────────────────────────────────────────────────────┘
                         *   7% wide                          16% wide    7% (kept)
                         *
                         * Controls deliberately left open:
                         *   • Seek / progress bar (center of bottom strip)
                         *   • Play / Pause (centre tap on video)
                         *   • Fullscreen button (bottom-right 7 %)
                         *   • Mute / CC / Quality icons (top-right, outside zone 1)
                         */

                        .yt-block {
                            position: absolute;
                            z-index: 999;
                            background: transparent;
                            /* suppress the blue tap flash on Android WebView */
                            -webkit-tap-highlight-color: transparent;
                            pointer-events: all;
                        }

                        /*
                         * Zone 1 — TOP INFO BAR
                         * Blocks: channel logo circle, video title text, channel name.
                         * Visible when the video is paused or the user hovers.
                         * Leaves the top-right corner open (mute / CC / quality icons
                         * that YouTube renders there when controls=1).
                         */
                        .yt-block-top-info {
                            top: 0;
                            left: 0;
                            width: 78%;
                            height: 14%;
                        }

                        /*
                         * Zone 2 — SHARE BUTTON (bottom-left of controls bar)
                         * Blocks: the arrow/share icon at the far left of the bar.
                         * The time display (e.g. "0:00 / 0:44") is just text and
                         * safe to leave exposed.
                         */
                        .yt-block-bottom-share {
                            bottom: 0;
                            left: 0;
                            width: 7%;
                            height: 11%;
                        }

                        /*
                         * Zone 3 — "MORE VIDEOS" ICON + YOUTUBE LOGO
                         * Blocks: the 3×3 grid "More videos" icon and the YouTube
                         * wordmark/logo that sit between the seek bar and the
                         * fullscreen button.
                         * Width: 16 %, offset 7 % from right so the fullscreen button
                         * (rightmost ~7 %) stays fully tappable.
                         */
                        .yt-block-bottom-branding {
                            bottom: 0;
                            right: 7%;
                            width: 30%;
                            height: 11%;
                        }

                        /*
                         * Zone 4 — END-SCREEN VIDEO CARDS
                         * When a video ends, YouTube overlays clickable recommendation
                         * thumbnails in the top-right ~30 % of the video area.
                         * This zone absorbs those taps without covering the centre
                         * play/pause area or any bottom controls.
                         * Only activates visually at end-of-video; harmless otherwise.
                         */
                        .yt-block-endscreen {
                            top: 14%;        /* start below the top-info zone */
                            right: 0;
                            width: 38%;
                            height: 55%;     /* leaves lower area for controls */
                        }
                    </style>
                </head>

                <body>
                    <div id="player-wrapper">

                        <!-- The YouTube IFrame API renders here -->
                        <div id="player"></div>

                        <!-- Zone 1: channel logo + title (top-left) -->
                        <div class="yt-block yt-block-top-info"></div>

                        <!-- Zone 2: share button (bottom-left controls) -->
                        <div class="yt-block yt-block-bottom-share"></div>

                        <!-- Zone 3: "More videos" icon + YouTube logo (bottom-right controls) -->
                        <div class="yt-block yt-block-bottom-branding"></div>

                        <!-- Zone 4: end-screen recommendation cards (top-right video area) -->
                        <div class="yt-block yt-block-endscreen"></div>

                    </div>

                    <script>
                        var tag = document.createElement('script');
                        tag.src = "https://www.youtube.com/iframe_api";

                        var firstScriptTag =
                            document.getElementsByTagName('script')[0];

                        firstScriptTag.parentNode.insertBefore(
                            tag,
                            firstScriptTag
                        );

                        var player;
                        var nextTriggered = false;

                        function onYouTubeIframeAPIReady() {
                            player = new YT.Player('player', {
                                height: '100%',
                                width: '100%',
                                videoId: '$videoId',

                                playerVars: {
                                    autoplay: $autoplayParam,
                                    playsinline: 1,
                                    rel: 0,
                                    modestbranding: 1,
                                    iv_load_policy: 3,
                                    fs: 1,
                                    controls: 1
                                },

                                events: {
                                    onReady: onPlayerReady,
                                    onStateChange: onPlayerStateChange,
                                    onError: onPlayerError
                                }
                            });
                        }
                        
                        var autoFullscreen = $restoreFullscreenParam;

                        function onPlayerReady(event) {
                            nextTriggered = false;
                            if (autoFullscreen) {
                                autoFullscreen = false;
                                setTimeout(function() {
                                    var btn = document.querySelector('.ytp-fullscreen-button');
                                    if (btn) btn.click();
                                }, 800);
                            }
                        }

                        function onPlayerStateChange(event) {
                            if (event.data == 0 && !nextTriggered) {
                                nextTriggered = true;

                                if (window.AndroidInterface) {
                                    window.AndroidInterface.onPlayerStateChange(0);
                                }
                            }
                        }

                        function onPlayerError(event) {
                            if (window.AndroidInterface) {
                                window.AndroidInterface.onPlayerError(event.data);
                            }
                        }
                    </script>

                </body>
                </html>
            """.trimIndent()

            webView.loadDataWithBaseURL(
                "https://www.youtube-nocookie.com",
                embedHtml,
                "text/html",
                "UTF-8",
                null
            )

            // auto restore fullscreen for next video
            if (wasFullscreen) {
                webView.postDelayed({
                    webView.evaluateJavascript(
                        """
                        javascript:
                        var btn = document.querySelector('.ytp-fullscreen-button');
                        if(btn){
                            btn.click();
                        }
                        """.trimIndent(),
                        null
                    )
                }, 3000)
            }
        }

        onDispose {
            webView.stopLoading()
            webView.loadUrl("about:blank")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView.destroy()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )
    }

    if (customView != null) {
        Dialog(
            onDismissRequest = {
                webView.webChromeClient?.onHideCustomView()
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            (customView?.parent as? ViewGroup)
                                ?.removeView(customView)

                            addView(
                                customView,
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private fun extractYouTubeVideoId(url: String): String? {
    if (url.isBlank()) return null

    val patterns = listOf(
        Regex("""youtube\.com/watch\?(?:.*&)?v=([-a-zA-Z0-9_]{11})"""),
        Regex("""youtu\.be/([-a-zA-Z0-9_]{11})"""),
        Regex("""youtube\.com/embed/([-a-zA-Z0-9_]{11})"""),
        Regex("""youtube\.com/v/([-a-zA-Z0-9_]{11})"""),
        Regex("""youtube\.com/shorts/([-a-zA-Z0-9_]{11})""")
    )

    for (pattern in patterns) {
        val match = pattern.find(url)
        if (match != null) {
            return match.groupValues[1]
        }
    }

    if (url.matches(Regex("""^[-a-zA-Z0-9_]{11}$"""))) {
        return url
    }

    return null
}