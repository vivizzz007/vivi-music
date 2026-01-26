package com.music.vivi.ui.screens.settings

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.DiscordTokenKey
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
/**
 * Screen that extracts the Discord token by loading the Discord login page in a WebView.
 * Attempts to intercept the token from local storage or Webpack chunks after login.
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordLoginScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var discordToken by rememberPreference(DiscordTokenKey, "")
    var webView: WebView? = null

    AndroidView(
        modifier = Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                WebView.setWebContentsDebuggingEnabled(true)

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true

                CookieManager.getInstance().apply {
                    removeAllCookies(null)
                    flush()
                }

                WebStorage.getInstance().deleteAllData()

                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onRetrieveToken(token: String) {
                            // Timber.d("Token: %s", token) // REMOVED FOR SECURITY
                            if (token != "null" && token != "error") {
                                discordToken = token
                                scope.launch(Dispatchers.Main) {
                                    webView?.loadUrl("about:blank")
                                    navController.navigateUp()
                                }
                            }
                        }
                    },
                    "Android"
                )

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        if (url.contains("/channels/@me") || url.contains("/app")) {
                            view.evaluateJavascript(
                                """
                                (function() {
                                    try {
                                        // Attempt 1: Webpack extraction (most reliable for Discord Web)
                                        let token = (webpackChunkdiscord_app.push([[''],{},e=>{m=[];for(let c in e.c)m.push(e.c[c])}]),m).find(m=>m?.exports?.default?.getToken!==undefined).exports.default.getToken();
                                        if (token) {
                                            Android.onRetrieveToken(token);
                                            return;
                                        }
                                    } catch (e) {
                                        console.log("Webpack extraction failed");
                                    }

                                    try {
                                        // Attempt 2: LocalStorage (Legacy)
                                        let token = localStorage.getItem("token");
                                        if (token) {
                                            Android.onRetrieveToken(token.replace(/^"|"$/g, '')); // Remove quotes safely
                                            return;
                                        }
                                    } catch (e) {
                                        console.log("LocalStorage extraction failed");
                                    }

                                    try {
                                        // Attempt 3: Iframe fallback
                                        var i = document.createElement('iframe');
                                        document.body.appendChild(i);
                                        setTimeout(function() {
                                            try {
                                                var alt = i.contentWindow.localStorage.token;
                                                if (alt) {
                                                    Android.onRetrieveToken(alt.replace(/^"|"$/g, ''));
                                                }
                                            } catch (e) {}
                                        }, 1000);
                                    } catch(e) {}
                                })();
                                """.trimIndent(),
                                null
                            )
                        }
                    }

                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = false
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
                        if (message != "null" && message != "error") {
                            discordToken = message
                            scope.launch(Dispatchers.Main) {
                                view.loadUrl("about:blank")
                                navController.navigateUp()
                            }
                        }
                        result.confirm()
                        return true
                    }
                }

                webView = this
                loadUrl("https://discord.com/login")
            }
        }
    )

    TopAppBar(
        title = { Text(stringResource(R.string.action_login)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        }
    )

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }
}
