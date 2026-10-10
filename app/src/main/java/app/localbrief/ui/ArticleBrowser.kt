@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package app.localbrief.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.net.URI

@Composable fun ArticleBrowser(
    initialUrl: String, initialTitle: String, dark: Boolean, saved: Boolean,
    snackbar: SnackbarHostState, onPage: (String, String) -> Unit,
    onSave: () -> Unit, onClose: () -> Unit, notify: (String) -> Unit
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var url by rememberSaveable { mutableStateOf(initialUrl) }
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var web by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var canBack by remember { mutableStateOf(false) }
    var canForward by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var rendererGone by remember { mutableStateOf(false) }
    var instance by remember { mutableIntStateOf(0) }
    val pageCallback by rememberUpdatedState(onPage)
    val text = if (dark) WarmWhite else Forest
    fun history(view: WebView) { canBack = view.canGoBack(); canForward = view.canGoForward() }
    fun external(link: String = url) {
        if (!BrowserPolicy.canLoad(link)) return
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, link.toUri()).addCategory(Intent.CATEGORY_BROWSABLE)) }
            .onFailure { notify("No browser could open this page.") }
    }
    fun retry() {
        error = null
        if (rendererGone || web == null) { rendererGone = false; instance++ } else web?.reload()
    }
    BackHandler { if (canBack && !rendererGone) web?.goBack() else onClose() }
    DisposableEffect(owner, web) {
        val view = web
        val observer = LifecycleEventObserver { _, event ->
            if (!rendererGone) when (event) {
                Lifecycle.Event.ON_RESUME -> view?.onResume()
                Lifecycle.Event.ON_PAUSE -> view?.onPause()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                Row(Modifier.statusBarsPadding().fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "Close article", tint = text) }
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(title.ifBlank { "Article" }, color = text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium)
                        Text(runCatching { URI(url).host }.getOrNull().orEmpty(), color = if (dark) Mint else MutedForest,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = onSave) {
                        Icon(if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, null, Modifier.size(20.dp), tint = text)
                        Spacer(Modifier.width(6.dp)); Text(if (saved) "Saved" else "Save", color = text)
                    }
                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, url); putExtra(Intent.EXTRA_SUBJECT, title)
                        }
                        runCatching { context.startActivity(Intent.createChooser(intent, "Share link")) }
                            .onFailure { notify("Couldn't open the share menu.") }
                    }) { Icon(Icons.Outlined.Share, null, Modifier.size(20.dp), tint = text)
                        Spacer(Modifier.width(6.dp)); Text("Share", color = text) }
                    IconButton(onClick = { external() }) {
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, "Open externally", tint = text)
                    }
                }
                if (progress < 100 && error == null) LinearProgressIndicator(progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(), color = if (dark) Mint else Forest)
            }
        },
        bottomBar = {
            Row(Modifier.navigationBarsPadding().fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(onClick = { web?.goBack() }, enabled = canBack && !rendererGone) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Previous page", tint = if (canBack) text else Sage)
                }
                IconButton(onClick = { web?.goForward() }, enabled = canForward && !rendererGone) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Next page", tint = if (canForward) text else Sage)
                }
                IconButton(onClick = { if (progress < 100 && error == null) { web?.stopLoading(); progress = 100 } else retry() }) {
                    Icon(if (progress < 100 && error == null) Icons.Outlined.Close else Icons.Outlined.Refresh,
                        if (progress < 100 && error == null) "Stop loading" else "Reload page", tint = text)
                }
            }
        }) { inset ->
        Column(Modifier.padding(inset).fillMaxSize()) {
            if (error != null) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(error!!, color = text)
                    Row { TextButton(onClick = { retry() }) { Text("Try again", color = text) }
                        TextButton(onClick = { external() }) { Text("Open in browser", color = text) } }
                }
            }
            if (!rendererGone) key(instance) {
                AndroidView(modifier = Modifier.weight(1f).fillMaxWidth(), factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        setBackgroundColor(WarmWhite.toArgb())
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = false
                            allowContentAccess = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            safeBrowsingEnabled = true
                            javaScriptCanOpenWindowsAutomatically = false
                            setSupportMultipleWindows(false)
                            mediaPlaybackRequiresUserGesture = true
                            setGeolocationEnabled(false)
                            builtInZoomControls = true
                            displayZoomControls = false
                            useWideViewPort = true
                            loadWithOverviewMode = true
                        }
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView, value: Int) { progress = value; history(view) }
                            override fun onReceivedTitle(view: WebView, value: String?) {
                                if (!value.isNullOrBlank()) { title = value.take(250); pageCallback(url, title) }
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                if (BrowserPolicy.canLoad(request.url.toString())) return false
                                if (request.isForMainFrame && request.hasGesture()) notify("This link needs an external app. Open this page externally to continue.")
                                return true
                            }
                            override fun onPageStarted(view: WebView, value: String?, favicon: Bitmap?) {
                                if (value != null && BrowserPolicy.canLoad(value)) {
                                    url = value; title = "Article"; error = null; progress = 0
                                    pageCallback(url, title); history(view)
                                }
                            }
                            override fun doUpdateVisitedHistory(view: WebView, value: String?, isReload: Boolean) {
                                if (value != null && BrowserPolicy.canLoad(value)) {
                                    url = value; pageCallback(url, title); history(view)
                                }
                            }
                            override fun onPageFinished(view: WebView, value: String?) {
                                if (value != null && BrowserPolicy.canLoad(value)) { url = value; pageCallback(url, title) }
                                progress = 100; history(view)
                            }
                            override fun onReceivedError(view: WebView, request: WebResourceRequest, failure: WebResourceError) {
                                if (request.isForMainFrame) { error = "Couldn't load this page. Check your connection or open it in your browser."; progress = 100 }
                            }
                            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                                if (request.isForMainFrame && response.statusCode >= 400) {
                                    error = "This site returned an error (${response.statusCode}). You can retry or open it in your browser."; progress = 100
                                }
                            }
                            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, failure: SslError) {
                                handler.cancel(); error = "This page couldn't establish a secure connection."; progress = 100
                            }
                            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                                (view.parent as? ViewGroup)?.removeView(view); view.destroy(); web = null
                                rendererGone = true; error = "The page viewer stopped. Try again to reopen this page."; progress = 100
                                return true
                            }
                        }
                        setDownloadListener { link, _, _, _, _ ->
                            if (BrowserPolicy.canLoad(link)) external(link)
                            else notify("Open the page externally to download this file.")
                        }
                        web = this
                        if (BrowserPolicy.canLoad(url)) loadUrl(url) else { error = "This page needs a valid HTTPS link."; progress = 100 }
                    }
                }, onRelease = { view ->
                    if (!rendererGone) { view.stopLoading(); view.webChromeClient = null; view.destroy() }
                    if (web === view) web = null
                })
            }
        }
    }
}
