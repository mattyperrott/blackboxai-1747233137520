package io.epher.chat

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.epher.chat.node.NodeJSBridge
import io.epher.chat.ui.TransportSheet

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val nav = rememberNavController()

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Epher") },
                        actions = {
                            IconButton(onClick = { nav.navigate("privacy") }) {
                                Icon(Icons.Default.Security, contentDescription = "Privacy")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                // Apply padding to avoid content underlapping topBar
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)) {

                    NavHost(nav, startDestination = "chat", Modifier.fillMaxSize()) {
                        composable("chat") {
                            val webView = remember {
                                WebView(this@MainActivity).apply {
                                    // JavaScript enabled — safe: only loading local assets
                                    settings.javaScriptEnabled = true
                                    settings.safeBrowsingEnabled = true
                                    settings.allowFileAccess = true
                                    settings.domStorageEnabled = true
                                    settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                                    settings.setSupportMultipleWindows(false)
                                    settings.javaScriptCanOpenWindowsAutomatically = false
                                    settings.allowContentAccess = true
                                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                    settings.userAgentString = "EpherSecureChat/1.0"

                                    // Added for better scaling and responsiveness
                                    settings.loadWithOverviewMode = true
                                    settings.useWideViewPort = true

                                    webViewClient = object : android.webkit.WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?, request: android.webkit.WebResourceRequest?
                                        ): Boolean {
                                            val url = request?.url ?: return true
                                            return !(url.scheme == "file" && url.path?.startsWith("/android_asset/") == true)
                                        }
                                    }

                                    val bridge = NodeJSBridge(context, this)
                                    addJavascriptInterface(bridge, "AndroidBridge")
                                    loadUrl("file:///android_asset/index.html")
                                }
                            }
                            AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
                        }
                        composable("privacy") {
                            TransportSheet()
                        }
                    }
                }
            }
        }
    }
}