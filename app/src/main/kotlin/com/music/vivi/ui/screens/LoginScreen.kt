/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.music.innertube.YouTube
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.AccountChannelHandleKey
import com.music.vivi.constants.AccountEmailKey
import com.music.vivi.constants.AccountNameKey
import com.music.vivi.constants.DataSyncIdKey
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.constants.VisitorDataKey
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.InfoLabel
import com.music.vivi.ui.component.snackbar.SnackbarManager
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.normalizeDataSyncId
import com.music.vivi.utils.rememberPreference
import com.music.vivi.utils.reportException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun LoginScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var visitorData by rememberPreference(VisitorDataKey, "")
    var dataSyncId by rememberPreference(DataSyncIdKey, "")
    var innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    var accountName by rememberPreference(AccountNameKey, "")
    var accountEmail by rememberPreference(AccountEmailKey, "")
    var accountChannelHandle by rememberPreference(AccountChannelHandleKey, "")

    var readyToConfirm by remember { mutableStateOf(false) }
    var isConfirming by remember { mutableStateOf(false) }

    var liveDelegatedSessionId by remember { mutableStateOf("") }

    var webView by remember { mutableStateOf<WebView?>(null) }

    var headerHeightPx by remember { mutableIntStateOf(0) }
    val headerHeight = with(LocalDensity.current) { headerHeightPx.toDp() }

    fun confirmLogin() {
        val currentUrl = webView?.url ?: return
        if (isConfirming) return
        isConfirming = true

        innerTubeCookie = CookieManager.getInstance().getCookie(currentUrl)
        if (liveDelegatedSessionId.isNotBlank()) {
            dataSyncId = liveDelegatedSessionId
        }

        coroutineScope.launch {
            // Initialize YouTube object with new authentication data
            YouTube.cookie = innerTubeCookie
            YouTube.dataSyncId = normalizeDataSyncId(dataSyncId)
            YouTube.visitorData = visitorData

            Timber.d("Login: YouTube object initialized, validating...")

            val result = withTimeoutOrNull(20_000.milliseconds) {
                var attempt = YouTube.accountInfo()
                if (attempt.isFailure) {
                    delay(750)
                    attempt = YouTube.accountInfo()
                }
                attempt
            }

            if (result == null) {
                Timber.e("Login: Authentication validation timed out")
                isConfirming = false
                SnackbarManager.show(R.string.login_failed)
                return@launch
            }

            result.onSuccess {
                accountName = it.name
                accountEmail = it.email.orEmpty()
                accountChannelHandle = it.channelHandle.orEmpty()

                Timber.d("Login: Successfully logged in as ${it.name}, restarting app...")

                webView?.apply {
                    stopLoading()
                    clearHistory()
                    clearCache(true)
                    clearFormData()
                }

                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(intent)
                Runtime.getRuntime().exit(0)
            }.onFailure {
                Timber.e(it, "Login: Authentication validation failed")
                isConfirming = false // Allow retry
                reportException(it)
                SnackbarManager.show(R.string.login_failed)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                )
                .fillMaxSize()
                .padding(top = headerHeight),
            factory = { webViewContext ->
                WebView(webViewContext).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            loadUrl("javascript:Android.onRetrieveVisitorData(window.yt.config_.VISITOR_DATA)")
                            loadUrl("javascript:Android.onRetrieveDataSyncId(window.yt.config_.DATASYNC_ID)")

                            if (url?.startsWith("https://music.youtube.com") == true) {
                                readyToConfirm = true
                            }
                        }
                    }
                    settings.apply {
                        javaScriptEnabled = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                    }
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onRetrieveVisitorData(newVisitorData: String?) {
                            if (newVisitorData != null) {
                                visitorData = newVisitorData
                            }
                        }
                        @JavascriptInterface
                        fun onRetrieveDataSyncId(newDataSyncId: String?) {
                            if (newDataSyncId != null) {
                                dataSyncId = newDataSyncId
                            }
                        }
                    }, "Android")
                    webView = this
                    loadUrl("https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com")
                }
            }
        )

        Column(modifier = Modifier.onGloballyPositioned { headerHeightPx = it.size.height }) {
            TopAppBar(
                title = { Text(stringResource(R.string.login)) },
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
                },
                actions = {
                    if (readyToConfirm) {
                        if (isConfirming) {
                            CircularProgressIndicator(modifier = Modifier.padding(12.dp).size(20.dp))
                        } else {
                            androidx.compose.material3.IconButton(onClick = ::confirmLogin) {
                                Icon(
                                    painterResource(R.drawable.check),
                                    contentDescription = stringResource(R.string.login_confirm)
                                )
                            }
                        }
                    }
                }
            )

            if (readyToConfirm && !isConfirming) {
                InfoLabel(text = stringResource(R.string.login_brand_account_hint))
            }
        }
    }

    LaunchedEffect(readyToConfirm) {
        if (!readyToConfirm) return@LaunchedEffect
        while (isActive && !isConfirming) {
            webView?.evaluateJavascript(
                "(function(){try{return (window.ytcfg&&window.ytcfg.get)?(window.ytcfg.get('DELEGATED_SESSION_ID')||''):'';}catch(e){return '';}})();"
            ) { result ->
                val id = result?.trim('"').orEmpty().takeUnless { it == "null" }.orEmpty()
                if (id != liveDelegatedSessionId) {
                    liveDelegatedSessionId = id
                }
            }
            delay(1000)
        }
    }

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }
}
