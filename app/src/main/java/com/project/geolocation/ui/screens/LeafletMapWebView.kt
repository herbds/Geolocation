package com.project.geolocation.ui.screens

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LeafletMapWebView(
    currentLocation: Location?,
    modifier: Modifier = Modifier
) {
    val isPageLoaded = remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Configuración del WebView
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

                // WebViewClient
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        isPageLoaded.value = true
                        Log.d("WebViewMap", "✅ Página cargada: $url")

                        currentLocation?.let { location ->
                            val lat = location.latitude
                            val lon = location.longitude
                            val jsCode = "javascript:updateLocation($lat, $lon)"
                            Log.d("WebViewMap", "📍 Ejecutando: $jsCode")

                            // Esperar un poco para que Leaflet se inicialice
                            postDelayed({
                                evaluateJavascript(jsCode) { result ->
                                    Log.d("WebViewMap", "✓ Resultado: $result")
                                }
                            }, 500)
                        }
                    }
                }

                // WebChromeClient para ver errores
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(message: android.webkit.ConsoleMessage): Boolean {
                        Log.d("WebViewConsole", "${message.message()} -- línea ${message.lineNumber()}")
                        return true
                    }
                }

                Log.d("WebViewMap", "🌐 Cargando HTML...")
                loadUrl("file:///android_asset/leaflet_map.html")
            }
        },
        update = { webView ->
            if (isPageLoaded.value) {
                currentLocation?.let { location ->
                    val lat = location.latitude
                    val lon = location.longitude
                    val jsCode = "javascript:updateLocation($lat, $lon)"

                    webView.evaluateJavascript(jsCode) { result ->
                        Log.d("WebViewMap", "🔄 Actualizado: $result")
                    }
                }
            }
        }
    )
}