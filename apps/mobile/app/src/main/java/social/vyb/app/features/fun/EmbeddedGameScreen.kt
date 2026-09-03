package social.vyb.app.features.funhub

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import social.vyb.app.BuildConfig
import social.vyb.app.DebugQaAuthToken
import social.vyb.app.data.network.requireIdToken
import social.vyb.app.ui.VybMuted

private val VYB_WEB_ORIGIN = BuildConfig.WEB_BASE_URL.trimEnd('/')

private class NativeGameBridge(private val reportError: (String) -> Unit) {
    @JavascriptInterface
    fun onBootstrapError(message: String) = reportError(message)
}

@Composable
fun LocalHtmlGameScreen(assetFolder: String, modifier: Modifier = Modifier) {
    GameWebView(
        initialUrl = "file:///android_asset/games/$assetFolder/index.html",
        bootstrapToken = null,
        destinationPath = null,
        modifier = modifier,
    )
}

@Composable
fun AuthenticatedWebGameScreen(gameSlug: String, modifier: Modifier = Modifier) {
    var token by remember { mutableStateOf<String?>(null) }
    var authError by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableStateOf(0) }

    LaunchedEffect(retryKey) {
        token = null
        authError = null
        token = runCatching {
            runCatching { FirebaseAuth.getInstance().currentUser?.requireIdToken() }
                .getOrNull()
                ?: DebugQaAuthToken.value?.takeIf { BuildConfig.DEBUG }
                ?: error("Sign in again to play online.")
        }.getOrElse {
            authError = it.message ?: "Online game authentication failed."
            null
        }
    }

    when {
        authError != null -> Box(modifier.fillMaxSize().background(Color(0xFF061326)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(authError!!, color = VybMuted, modifier = Modifier.padding(20.dp))
                Button(onClick = { retryKey++ }) { Text("Try again") }
            }
        }
        token == null -> Box(modifier.fillMaxSize().background(Color(0xFF061326)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        else -> GameWebView(
            initialUrl = "$VYB_WEB_ORIGIN/native-game-bootstrap",
            bootstrapToken = token,
            destinationPath = "/hub/gameshub/$gameSlug",
            modifier = modifier,
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun GameWebView(
    initialUrl: String,
    bootstrapToken: String?,
    destinationPath: String?,
    modifier: Modifier,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loading by remember(initialUrl) { mutableStateOf(true) }
    var pageError by remember(initialUrl) { mutableStateOf<String?>(null) }

    BackHandler(enabled = webView?.canGoBack() == true) { webView?.goBack() }
    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }

    Box(modifier.fillMaxSize().background(Color(0xFF061326))) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    isClickable = true
                    isFocusable = true
                    isFocusableInTouchMode = true
                    setOnTouchListener { view, event ->
                        // Compose parents can otherwise retain the gesture stream and make
                        // controls inside the embedded game appear visible but untappable.
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                                view.parent?.requestDisallowInterceptTouchEvent(true)
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                                view.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                        false
                    }
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = initialUrl.startsWith("file:")
                    settings.allowContentAccess = false
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    if (initialUrl.startsWith("file:")) {
                        settings.cacheMode = WebSettings.LOAD_NO_CACHE
                        clearCache(true)
                    }
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                    addJavascriptInterface(
                        NativeGameBridge { message ->
                            post {
                                pageError = message.ifBlank { "Online game session could not be opened." }
                                loading = false
                            }
                        },
                        "VybNativeGame",
                    )
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        private var bootstrapStarted = false

                        override fun onPageFinished(view: WebView, url: String) {
                            if (bootstrapToken != null && destinationPath != null && !bootstrapStarted) {
                                bootstrapStarted = true
                                val quotedToken = JSONObject.quote(bootstrapToken)
                                val quotedPath = JSONObject.quote(destinationPath)
                                view.evaluateJavascript(
                                    """
                                    (async function () {
                                      try {
                                        const response = await fetch('/api/auth/session', {
                                          method: 'POST',
                                          credentials: 'include',
                                          headers: {'content-type': 'application/json'},
                                          body: JSON.stringify({idToken: $quotedToken})
                                        });
                                        if (!response.ok) throw new Error('Session ' + response.status);
                                        window.location.replace($quotedPath);
                                      } catch (error) {
                                        const message = String(error && error.message || error);
                                        document.body.dataset.nativeGameError = message;
                                        if (window.VybNativeGame) window.VybNativeGame.onBootstrapError(message);
                                      }
                                    })();
                                    """.trimIndent(),
                                    null,
                                )
                            } else if (bootstrapToken == null || url.contains(destinationPath.orEmpty())) {
                                loading = false
                            }
                        }

                        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                            if (request.isForMainFrame) {
                                pageError = error.description?.toString() ?: "Game page could not load."
                                loading = false
                            }
                        }
                    }
                    // Always bootstrap over the existing first-party cookie jar. The successful
                    // session response overwrites every Vyb auth cookie before navigation. An
                    // asynchronous removeAllCookies call can finish after that response and erase
                    // the newly-created session, leaving online games unauthenticated.
                    loadUrl(initialUrl)
                    webView = this
                }
            },
        )
        if (loading) Box(Modifier.fillMaxSize().background(Color(0xCC061326)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        pageError?.let { error ->
            Box(Modifier.fillMaxSize().background(Color(0xEE061326)), contentAlignment = Alignment.Center) {
                Text(error, color = VybMuted, modifier = Modifier.padding(22.dp))
            }
        }
    }
}
