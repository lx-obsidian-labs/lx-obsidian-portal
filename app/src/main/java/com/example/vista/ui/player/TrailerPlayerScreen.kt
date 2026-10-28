package com.example.vista.ui.player

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.vista.ui.theme.*

/** Origin the trailer embed is served from; the only one the trailer WebView may navigate to. */
private const val YOUTUBE_EMBED_ORIGIN = "https://www.youtube.com/"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TrailerPlayerScreen(
    navController: NavController,
    videoKey: String,
    title: String
) {
    val youtubeUrl = "https://www.youtube.com/embed/$videoKey?autoplay=1&rel=0"
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // WebView for YouTube player
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    webViewClient = object : WebViewClient() {
                        // Trailers only ever come from YouTube. Keeping the main frame on that origin
                        // stops a redirect from parking arbitrary JavaScript in this WebView.
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: android.webkit.WebResourceRequest?
                        ): Boolean {
                            if (request?.isForMainFrame != true) return false
                            // Spelled out rather than using `url`, which inside a WebViewClient
                            // resolves to WebView.url (the last loaded page, nullable) rather than the
                            // navigation being judged - that shadowing would compare the wrong string.
                            val target = request.url?.toString().orEmpty()
                            return !target.startsWith(YOUTUBE_EMBED_ORIGIN)
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            errorCode: Int,
                            description: String?,
                            failingUrl: String?
                        ) {
                            hasError = true
                            isLoading = false
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            if (newProgress == 100) {
                                isLoading = false
                            }
                        }
                    }
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    // The YouTube player needs neither local file nor content provider reads.
                    settings.allowContentAccess = false
                    settings.allowFileAccess = false
                    settings.setGeolocationEnabled(false)
                    // The embed page is https; refuse plaintext sub-resources as the app does elsewhere.
                    settings.mixedContentMode =
                        android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    settings.setSupportZoom(false)
                    loadUrl(youtubeUrl)
                }
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = { webView ->
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.clearHistory()
                webView.removeAllViews()
                webView.destroy()
            }
        )

        // Loading indicator
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Accent
            )
        }

        // Error state
        if (hasError) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    tint = MutedText,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Failed to load trailer",
                    color = PrimaryText,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Please check your internet connection",
                    color = MutedText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Top bar overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.width(48.dp)) // Balance the back button
            }
        }
    }
}
