package blackorbs.dev.jetfiledownloader.ui.home

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebView.WebViewTransport
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import androidx.webkit.WebViewFeature.isFeatureSupported
import blackorbs.dev.jetfiledownloader.R
import blackorbs.dev.jetfiledownloader.helpers.LinkHelper
import blackorbs.dev.jetfiledownloader.ui.startActivityWithChooser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.URISyntaxException

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ComposeWeb(
    state: WebState,
    webClient: WebClient? = null,
    chromeClient: ChromeClient? = null,
    onNewDownload: (String, String, String, String, Long) -> Unit
){
    if(webClient != null) state.webClient = webClient
    if(chromeClient != null) state.chromeClient = chromeClient

    state.webView?.let{
        LaunchedEffect(it) {
            with(state.navigator){
                it.handleNavigation()
            }
        }
    }

    AndroidView(factory = { context -> WebView(context).apply {
        with(CookieManager.getInstance()) {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(
                this@apply, true
            )
        }
        with(settings){
            textZoom = 100
            setSupportZoom(true)
            useWideViewPort = true
            javaScriptEnabled = true
            domStorageEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            userAgentString = System.getProperty("http.agent")
            if(resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK
                == Configuration.UI_MODE_NIGHT_YES
            ) setDarkMode()
        }
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setDownloadListener{ url, userAgent, contentDisposition,
                             mimeType, contentLength ->
            onNewDownload(url, userAgent,
                contentDisposition, mimeType, contentLength
            )
            state.loadProgress = 1f
        }
        webViewClient = state.webClient
        webChromeClient = state.chromeClient

        if(state.viewState == null) {
            state.webData?.run {
                (obj as WebViewTransport).webView = this@apply
                sendToTarget()
            }
            state.initUrl?.let {
                loadUrl(LinkHelper.getValidUrl(it))
            }
        }
        else {
            restoreState(state.viewState!!)
            if(state.favoriteUrl == null){
                state.onStateRestored?.invoke(state.lastLoadedUrl)
            }
            else {
                loadUrl(LinkHelper.getValidUrl(state.favoriteUrl!!))
                state.favoriteUrl = null
            }
        }
        state.webView = this
    }
    }, update = { state.webView = it }
    , onRelease = { webView ->
        state.viewState = Bundle().apply {
            webView.saveState(this)
        }
    })
}

class WebStateSaver(
    private val scope: CoroutineScope,
    private val onNewWindow: (Message?) -> Unit,
    private val onCloseWindow: () -> Unit
) : Saver<WebState, Map<String, Any?>>{

    private val id = "ID"
    private val webState = "WebStateKey"
    private val lastLoadedUrl = "LastLoadedUrl"

    override fun SaverScope.save(value: WebState): Map<String, Any?> = mapOf(
        id to value.id, lastLoadedUrl to value.lastLoadedUrl,
        webState to Bundle().apply { value.webView?.saveState(this) }
    )

    override fun restore(value: Map<String, Any?>): WebState = WebState(
        id = value[id] as Int, scope = scope,
        onNewWindow = onNewWindow, onCloseWindow = onCloseWindow
    ).apply {
        lastLoadedUrl = value[lastLoadedUrl] as String?
        viewState = value[webState] as Bundle
    }
}

class WebState(
    val id: Int,
    val initUrl: String? = null,
    val webData: Message? = null,
    val onNewWindow: (Message?) -> Unit,
    private val onCloseWindow: () -> Unit,
    scope: CoroutineScope,
    val navigator: WebNavigator = WebNavigator(scope, onCloseWindow)
) {
    internal var webClient: WebClient = WebClient(this)
    internal var chromeClient: ChromeClient = ChromeClient(this)

    internal var webView by mutableStateOf<WebView?>(null)

    internal var viewState: Bundle? = null
    internal var favoriteUrl: String? = null
    internal var onStateRestored: ((String?) -> Unit)? = null

    var lastLoadedUrl: String? by mutableStateOf(null)
        internal set

    var pageTitle: String? by mutableStateOf(null)
        internal set

    var pageIcon: Bitmap? by mutableStateOf(null)
        internal set

    var loadProgress by mutableFloatStateOf(1f)
        internal set

    var isError by mutableStateOf(false)
        internal set
}

class WebNavigator(
    private val scope: CoroutineScope,
    private val onCloseWindow: () -> Unit
){
    private sealed interface NavAction{
        data object Back: NavAction
        data object Forward: NavAction
        data object Reload: NavAction
        data object StopLoading: NavAction
        data class LoadUrl(val url: String): NavAction
    }

    private val navActions: MutableSharedFlow<NavAction> =
        MutableSharedFlow(replay = 1)

    internal suspend fun WebView.handleNavigation(): Nothing =
        withContext(Dispatchers.Main){
            navActions.collect{ action ->
                when(action){
                    NavAction.Back -> {
                        if(canGoBack) {
                            if(canGoBack()) goBack()
                            else onCloseWindow()
                        }
                    }
                    NavAction.Forward -> if(canGoForward()) goForward()
                    is NavAction.LoadUrl -> loadUrl(
                        LinkHelper.getValidUrl(action.url)
                    )
                    NavAction.Reload -> reload()
                    NavAction.StopLoading -> stopLoading()
                }
            }
        }

    fun loadUrl(url: String){
        scope.launch {
            navActions.emit(NavAction.LoadUrl(url))
        }
    }

    fun goBack(){
        scope.launch {
            navActions.emit(NavAction.Back)
        }
    }

    fun goForward(){
        scope.launch {
            navActions.emit(NavAction.Forward)
        }
    }

    fun reload(){
        scope.launch {
            navActions.emit(NavAction.Reload)
        }
    }

    @Suppress("Unused")
    fun stopLoading(){
        scope.launch {
            navActions.emit(NavAction.StopLoading)
        }
    }

    var canGoBack by mutableStateOf(false)
        internal set

    var canGoForward by mutableStateOf(false)
        internal set
}

open class ChromeClient(private val state: WebState): WebChromeClient(){

    override fun onCreateWindow(
        view: WebView?, isDialog: Boolean,
        isUserGesture: Boolean, resultMsg: Message?
    ): Boolean {
        if(isUserGesture && resultMsg != null){
            state.onNewWindow(resultMsg)
            return true
        }
        return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        state.pageTitle = title
    }

    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
        super.onReceivedIcon(view, icon)
        state.pageIcon = icon
    }

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        state.loadProgress = newProgress.toFloat()/100
    }

}

open class WebClient(private val state: WebState) : WebViewClient(){

    override fun shouldOverrideUrlLoading(
        view: WebView?, request: WebResourceRequest?
    ): Boolean {
        if(request != null) {
            if (URLUtil.isNetworkUrl(request.url.toString()))
                return super.shouldOverrideUrlLoading(view, request)
            try {
                val context = view!!.context
                val intent = if(request.url.toString().startsWith("intent://"))
                    Intent.parseUri(request.url.toString(), Intent.URI_INTENT_SCHEME)
                else Intent(Intent.ACTION_VIEW)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .setDataAndType(
                        request.url, context.contentResolver.getType(request.url)
                    )
                context.startActivityWithChooser(intent, R.string.load_using)
            } catch (e: URISyntaxException) {
                Timber.e("URISyntaxError:: ${e.message}")
            }
        }
        return true
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        state.isError = false
        state.lastLoadedUrl = url
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        state.loadProgress = 1f
    }

    override fun onReceivedError(
        view: WebView?, request: WebResourceRequest?, error: WebResourceError?
    ) {
        if(view?.url == request?.url.toString() && error != null){
            state.isError = true
        }
        super.onReceivedError(view, request, error)
    }

    override fun doUpdateVisitedHistory(
        view: WebView, url: String?, isReload: Boolean
    ) {
        super.doUpdateVisitedHistory(view, url, isReload)

        state.navigator.canGoBack = view.canGoBack() || state.id != 0
        state.navigator.canGoForward = view.canGoForward()
    }
}

@Suppress("Deprecation")
fun WebSettings.setDarkMode(){
    if(isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            isAlgorithmicDarkeningAllowed = true
        else WebSettingsCompat
            .setAlgorithmicDarkeningAllowed(this, true)
    }
    if(isFeatureSupported(WebViewFeature.FORCE_DARK)){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            forceDark = WebSettings.FORCE_DARK_ON
        else WebSettingsCompat.setForceDark(
            this, WebSettingsCompat.FORCE_DARK_ON
        )
    }
    if(isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)){
        WebSettingsCompat.setForceDarkStrategy(this,
            WebSettingsCompat
                .DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING
        )
    }
}