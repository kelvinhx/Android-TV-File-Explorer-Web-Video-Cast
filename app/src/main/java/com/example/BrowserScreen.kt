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
                        setSupportMultipleWindows(false) // Disable popups
                        userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
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
                            
                            // Inject cursor for premium virtual pointer on TV screen (smart TCL, Samsung TV visual aid)
                            view?.evaluateJavascript(
                                """
                                (function() {
                                    if (window.tvCursorInitialized) return;
                                    window.tvCursorInitialized = true;

                                    const cursor = document.createElement('div');
                                    cursor.id = 'tv-virtual-cursor';
                                    cursor.style.position = 'fixed';
                                    cursor.style.width = '26px';
                                    cursor.style.height = '26px';
                                    cursor.style.borderRadius = '50%';
                                    cursor.style.backgroundColor = 'rgba(10, 132, 255, 0.45)';
                                    cursor.style.border = '2px solid #0A84FF';
                                    cursor.style.boxShadow = '0 0 12px rgba(10, 132, 255, 0.9)';
                                    cursor.style.left = '50%';
                                    cursor.style.top = '50%';
                                    cursor.style.transform = 'translate(-13px, -13px)';
                                    cursor.style.zIndex = '99999999';
                                    cursor.style.pointerEvents = 'none';
                                    cursor.style.transition = 'left 0.1s cubic-bezier(0.25, 0.8, 0.25, 1), top 0.1s cubic-bezier(0.25, 0.8, 0.25, 1)';
                                    document.body.appendChild(cursor);

                                    let x = window.innerWidth / 2;
                                    let y = window.innerHeight / 2;
                                    const speed = 40;

                                    function updateCursor() {
                                        cursor.style.left = x + 'px';
                                        cursor.style.top = y + 'px';
                                    }
                                    updateCursor();

                                    window.addEventListener('keydown', function(e) {
                                        let handled = false;
                                        switch(e.keyCode) {
                                            case 37: // Left (DPAD LEFT)
                                                x = Math.max(15, x - speed);
                                                handled = true;
                                                break;
                                            case 38: // Up (DPAD UP)
                                                y = Math.max(15, y - speed);
                                                if (y < 80) {
                                                    window.scrollBy(0, -120);
                                                }
                                                handled = true;
                                                break;
                                            case 39: // Right (DPAD RIGHT)
                                                x = Math.min(window.innerWidth - 15, x + speed);
                                                handled = true;
                                                break;
                                            case 40: // Down (DPAD DOWN)
                                                y = Math.min(window.innerHeight - 15, y + speed);
                                                if (y > window.innerHeight - 80) {
                                                    window.scrollBy(0, 120);
                                                }
                                                handled = true;
                                                break;
                                            case 13: // Enter / Center (DPAD CENTER)
                                                const el = document.elementFromPoint(x, y);
                                                if (el) {
                                                    el.click();
                                                    if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.contentEditable === 'true') {
                                                        el.focus();
                                                    }
                                                }
                                                handled = true;
                                                break;
                                        }
                                        if (handled) {
                                            e.preventDefault();
                                            e.stopPropagation();
                                            updateCursor();
                                        }
                                    }, true);
                                })();
                                """.trimIndent(), null
                            )
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            request?.url?.toString()?.let { urlString ->
                                val lowerUrl = urlString.lowercase()
                                
                                // Extensive Ad / Pop-up / Tracker blocker logic
                                val adDomains = listOf(
                                    "googlesyndication.com", "adservice.google.com", "doubleclick.net",
                                    "adnxs.com", "outbrain.com", "taboola.com", "popads.net",
                                    "propellerads.com", "adsterra.com", "onclickads.net",
                                    "exoclick.com", "criteo.com", "adroll.com", "quantserve.com",
                                    "scorecardresearch.com", "zedo.com", "yieldmanager.com",
                                    "pubmatic.com", "rubiconproject.com", "appnexus.com",
                                    "amazon-adsystem.com", "rlcdn.com", "adform.net",
                                    "bidswitch.net", "casalemedia.com", "crwdcntrl.net",
                                    "demdex.net", "bluekai.com", "mathtag.com",
                                    "mxptint.net", "openx.net", "adskeeper.co.uk",
                                    "mgid.com", "revcontent.com", "adblade.com",
                                    "sharethrough.com", "teads.tv", "spotxchange.com",
                                    "tremorhub.com", "vungle.com", "unityads.unity3d.com",
                                    "chartboost.com", "inmobi.com", "startappexchange.com",
                                    "adcolony.com", "tapjoy.com", "mocean.mobi",
                                    "smaato.net", "inner-active.mobi", "ads.", "analytics."
                                )
                                
                                val isPopupOrTracking = lowerUrl.contains("/popup") || 
                                                        lowerUrl.contains("/click") ||
                                                        lowerUrl.contains("tracking") ||
                                                        lowerUrl.contains("banner") ||
                                                        lowerUrl.contains("popunder")
                                
                                if (adDomains.any { lowerUrl.contains(it) } || isPopupOrTracking) {
                                    return WebResourceResponse("text/plain", "UTF-8", java.io.ByteArrayInputStream(ByteArray(0)))
                                }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }
                    }

                    // Key events for DPAD nav in browser: pass to WebView so JS capturer can handle
                    setOnKeyListener { _, _, _ ->
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
