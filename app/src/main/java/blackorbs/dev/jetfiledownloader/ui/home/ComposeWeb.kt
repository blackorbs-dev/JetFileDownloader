package blackorbs.dev.jetfiledownloader.ui.home

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ComposeWeb(
    state: WebState,
    webClient: WebClient? = null,
    chromeClient: ChromeClient? = null,
    onNewDownload: ( url: String, userAgent: String,
                     contentDisposition: String, mimeType: String,
                     contentLength: Long ) -> Unit
){
    val viewHolder = rememberSaveable(saver = WebViewSaver(state)) {
        ViewHolder()
    }

    if(webClient != null) state.webClient = webClient
    if(chromeClient != null) state.chromeClient = chromeClient

    if(state.shouldSet){
        LaunchedEffect(state.shouldSet) {
            with(state.navigator){
                viewHolder.webView?.handleNavigation()
            }
            state.shouldSet = false
        }
    }

    AndroidView(factory = { context ->
        WebView(context).apply {

            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.setSupportZoom(true)
            settings.useWideViewPort = true
            settings.textZoom = 100

            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )

            setDownloadListener(onNewDownload)

            webViewClient = state.webClient
            webChromeClient = state.chromeClient

            if(state.viewState == null) loadUrl(state.initUrl)
            else restoreState(state.viewState!!)

            viewHolder.webView = this
            state.shouldSet = true
        }
    }, update = { viewHolder.webView = it }, onRelease = { viewHolder.webView?.destroy() })

    BackHandler(state.navigator.canGoBack) {
        viewHolder.webView?.goBack()
    }

}

class WebViewSaver(private val state: WebState) : Saver<ViewHolder, Map<String, Any>>{
    private val bundle = "bundle"
    override fun SaverScope.save(value: ViewHolder): Map<String, Any> = mapOf(bundle to Bundle().apply {
        value.webView?.saveState(this)
    })

    override fun restore(value: Map<String, Any>): ViewHolder =
        ViewHolder().apply {
            state.viewState = value[bundle] as Bundle?
        }
}

class ViewHolder{
    var webView: WebView? = null
}

class WebState(
    val initUrl: String, scope: CoroutineScope,
    val navigator: WebNavigator = WebNavigator(scope)
) {
    internal var webClient: WebClient = WebClient(this)
    internal var chromeClient: ChromeClient = ChromeClient(this)

    internal var shouldSet: Boolean by mutableStateOf(false)

    internal var viewState: Bundle? = null

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

class WebNavigator(private val scope: CoroutineScope){
    private sealed interface NavAction{
        data object Back: NavAction
        data object Forward: NavAction
        data object Reload: NavAction
        data object StopLoading: NavAction

        data class LoadUrl(val url: String): NavAction
    }

    private val navActions: MutableSharedFlow<NavAction> = MutableSharedFlow(replay = 1)

    internal suspend fun WebView.handleNavigation(): Nothing = withContext(Dispatchers.Main){
        navActions.collect{ action ->
            when(action){
                NavAction.Back -> if(canGoBack()) goBack()
                NavAction.Forward -> if(canGoForward()) goForward()
                is NavAction.LoadUrl -> loadUrl(action.url)
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

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        state.isError = false
        state.lastLoadedUrl = url
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        if(view?.url == request?.url.toString()){
            state.isError = true
        }
        super.onReceivedError(view, request, error)
    }

    @Deprecated("Deprecated in Java", ReplaceWith(
        "super.onReceivedError(view, errorCode, description, failingUrl)",
        "android.webkit.WebViewClient"
    ))
    @Suppress("DEPRECATION")
    override fun onReceivedError(
        view: WebView?,
        errorCode: Int,
        description: String?,
        failingUrl: String?
    ) {
        if(view?.url == failingUrl){
            state.isError = true
        }
        super.onReceivedError(view, errorCode, description, failingUrl)
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
        super.doUpdateVisitedHistory(view, url, isReload)

        state.navigator.canGoBack = view.canGoBack()
        state.navigator.canGoForward = view.canGoForward()
    }
}