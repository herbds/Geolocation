package com.project.geolocation.ui.screens

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.project.geolocation.network.PendingDestination

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LeafletMapWebView(
    currentLocation: Location?,
    pendingDestination: PendingDestination? = null,
    onDestinationCleared: () -> Unit = {},
    onRouteStatusChanged: (isOffRoute: Boolean, distance: Double) -> Unit = { _, _ -> }, // ✅ NUEVO
    modifier: Modifier = Modifier
) {
    val isPageLoaded = remember { mutableStateOf(false) }
    val webViewState = remember { mutableStateOf<WebView?>(null) }

    // Update destination when it changes
    LaunchedEffect(pendingDestination) {
        if (isPageLoaded.value && pendingDestination != null) {
            webViewState.value?.let { webView ->
                val lat = pendingDestination.latitude
                val lon = pendingDestination.longitude
                val jsCode = "javascript:setDestination($lat, $lon)"
                Log.d("WebViewMap", "🎯 Setting destination: $jsCode")

                webView.post {
                    webView.evaluateJavascript(jsCode) { result ->
                        Log.d("WebViewMap", "🎯 Destination set result: $result")
                    }
                }
            }
        } else if (isPageLoaded.value && pendingDestination == null) {
            webViewState.value?.let { webView ->
                val jsCode = "javascript:clearDestination()"
                Log.d("WebViewMap", "🧹 Clearing destination")

                webView.post {
                    webView.evaluateJavascript(jsCode) { result ->
                        Log.d("WebViewMap", "🧹 Destination cleared result: $result")
                    }
                }
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                setBackgroundColor(Color.TRANSPARENT)

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    databaseEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = false
                    displayZoomControls = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                }

                // ✅ ACTUALIZADO: JavaScript Interface con detección de ruta
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onDestinationCleared() {
                        Log.d("WebViewMap", "🧹 Destination cleared from web")
                        onDestinationCleared()
                    }

                    // ✅ NUEVO: Callback para estado de ruta
                    @JavascriptInterface
                    fun onRouteStatusChanged(status: String, distance: Double) {
                        Log.d("WebViewMap", "🛣️ Route status: $status (${distance.toInt()}m)")
                        val isOffRoute = status == "off_route"
                        onRouteStatusChanged(isOffRoute, distance)
                    }
                }, "Android")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        isPageLoaded.value = true
                        Log.d("WebViewMap", "✅ Página cargada: $url")

                        currentLocation?.let { location ->
                            val lat = location.latitude
                            val lon = location.longitude
                            val jsCode = "updateLocation($lat, $lon)"
                            Log.d("WebViewMap", "📍 Ejecutando: $jsCode")

                            postDelayed({
                                evaluateJavascript(jsCode) { result ->
                                    Log.d("WebViewMap", "✓ Resultado: $result")
                                }

                                pendingDestination?.let { dest ->
                                    val destCode = "javascript:setDestination(${dest.latitude}, ${dest.longitude})"
                                    Log.d("WebViewMap", "🎯 Setting initial destination: $destCode")
                                    evaluateJavascript(destCode) { result ->
                                        Log.d("WebViewMap", "🎯 Initial destination result: $result")
                                    }
                                }
                            }, 500)
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(message: android.webkit.ConsoleMessage): Boolean {
                        Log.d("WebViewConsole", "${message.message()} -- línea ${message.lineNumber()}")
                        return true
                    }
                }

                webViewState.value = this
                Log.d("WebViewMap", "🌐 Cargando HTML...")
                loadUrl("file:///android_asset/leaflet_map.html")
            }
        },
        update = { webView ->
            if (isPageLoaded.value) {
                currentLocation?.let { location ->
                    val lat = location.latitude
                    val lon = location.longitude
                    val jsCode = "updateLocation($lat, $lon)"

                    webView.evaluateJavascript(jsCode) { result ->
                        Log.d("WebViewMap", "🔄 Actualizado: $result")
                    }
                }
            }
        }
    )
}