package com.example

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    initialUrl: String = "https://www.google.com",
    onClose: () -> Unit
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var canGoBack by remember { mutableStateOf(false) }

    BackHandler {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            onClose()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Simple Top Bar for Browser Actions
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp).background(AppConfig.CardBackground).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close Browser", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = currentUrl,
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { webView?.reload() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
            }
        }

        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    isFocusable = true
                    isFocusableInTouchMode = true
                    
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false // Great for TV Video playback
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            url?.let { currentUrl = it }
                            canGoBack = view?.canGoBack() ?: false
                            view?.requestFocus()
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            view?.requestFocus()
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            request?.url?.toString()?.let { urlString ->
                                // Simple Ad / Pop-up blocker logic
                                val adDomains = listOf(
                                    "googlesyndication.com",
                                    "adservice.google.com",
                                    "doubleclick.net",
                                    "adnxs.com",
                                    "outbrain.com",
                                    "taboola.com"
                                )
                                if (adDomains.any { urlString.contains(it) }) {
                                    return WebResourceResponse("text/plain", "UTF-8", null)
                                }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }
                    }

                    // Key events for DPAD nav in browser
                    setOnKeyListener { v, keyCode, event ->
                        if (event.action == KeyEvent.ACTION_DOWN) {
                            when (keyCode) {
                                KeyEvent.KEYCODE_DPAD_CENTER -> { return@setOnKeyListener false }
                                KeyEvent.KEYCODE_DPAD_DOWN -> {
                                    v.scrollBy(0, 100)
                                    return@setOnKeyListener false
                                }
                                KeyEvent.KEYCODE_DPAD_UP -> {
                                    v.scrollBy(0, -100)
                                    return@setOnKeyListener false
                                }
                                KeyEvent.KEYCODE_DPAD_LEFT -> {
                                    if (canGoBack) {
                                        goBack()
                                        return@setOnKeyListener true
                                    }
                                }
                            }
                        }
                        false
                    }

                    loadUrl(initialUrl)
                    webView = this
                    requestFocus()
                }
            },
            update = { view ->
                view.requestFocus()
            }
        )
    }
}
