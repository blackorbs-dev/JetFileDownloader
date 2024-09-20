package blackorbs.dev.jetfiledownloader.ui

import android.net.Uri
import android.os.Message
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import blackorbs.dev.jetfiledownloader.entities.Favorite
import blackorbs.dev.jetfiledownloader.helpers.LinkHelper
import blackorbs.dev.jetfiledownloader.ui.favorite.FavViewModel
import blackorbs.dev.jetfiledownloader.ui.home.WebState
import blackorbs.dev.jetfiledownloader.ui.home.WebStateSaver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun rememberAppState(
    scope: CoroutineScope = rememberCoroutineScope(),
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    navController: NavHostController = rememberNavController(),
    favViewModel: FavViewModel = viewModel(factory = FavViewModel.Factory)
): AppState {

    val backStackEntry = navController.currentBackStackEntryAsState()
    val currentPage = remember { derivedStateOf {
        backStackEntry.value?.destination?.run {
            if(hasRoute(Page.FileInfo::class))
                return@derivedStateOf backStackEntry.value!!.toRoute<Page.FileInfo>()
            if(hasRoute(Page.DownloadList::class))
                return@derivedStateOf backStackEntry.value!!.toRoute<Page.DownloadList>()
            if(hasRoute(Page.Favorites::class))
                return@derivedStateOf backStackEntry.value!!.toRoute<Page.Favorites>()
        }
        Page.Home()
    } }

    val webDataState = remember { mutableStateOf<Message?>(null) }

    val appState = remember{ AppState(
        scope, drawerState, snackbarHostState,
        navController, currentPage, webDataState, favViewModel
    ) }

    @Composable
    fun addNewWebpage(initUrl: String?, webData: Message?){
        appState.addNewWebPage( rememberSaveable(saver =
            WebStateSaver(
                scope = scope,
                onNewWindow = {webDataState.value = it},
                onCloseWindow = appState::removeWebPage
            )
        ) { appState.getWebState(initUrl, webData) })
    }

    webDataState.value?.let {
        addNewWebpage(initUrl = null, webData = it)
        webDataState.value = null
    }

    if(appState.webStateList.value.isEmpty()){
        addNewWebpage(initUrl = LinkHelper.INIT_URL, webData = null)
    }
    else{
        appState.currentWebState.lastLoadedUrl?.let {
            favViewModel.setIsFavorite(it)
        }
    }

    return appState
}

class AppState(
    private val scope: CoroutineScope,
    val drawerState: DrawerState,
    val snackbarHostState: SnackbarHostState,
    val navController: NavHostController,
    val currentPage: State<Page>,
    private val webDataState: MutableState<Message?>,
    val favViewModel: FavViewModel
) {
    private val _webStateList = mutableStateListOf<WebState>()
    val webStateList: State<List<WebState>> = derivedStateOf { _webStateList }

    val currentWebState get() = _webStateList.last()

    fun addNewWebPage(webState: WebState){
        _webStateList.add(webState)
    }

    fun removeWebPage(){
        _webStateList.removeLast()
    }

    fun getWebState(initUrl: String?, webData: Message?) = WebState(
        id = _webStateList.size, initUrl = initUrl,
        scope = scope, webData = webData,
        onNewWindow = { webDataState.value = it },
        onCloseWindow = this::removeWebPage
    ).apply {
        onStateRestored = { url ->
            url?.let { favViewModel.setIsFavorite(it) }
        }
    }

    fun onAddFavorite(){
        currentWebState.lastLoadedUrl?.let {
            favViewModel.add(Favorite(
                url = it, title =
                    currentWebState.pageTitle
                    ?: Uri.parse(it).host ?: "N/A"
            ))
        }
    }

    fun onLoadFavoriteUrl(url: String?){
        currentWebState.favoriteUrl = url
        navController.load(Page.Home())
    }

    fun showMessage(msg: String){
        scope.launch {
            snackbarHostState.showSnackbar(msg)
        }
    }

    fun showOrHideDrawer(){
        scope.launch {
            with(drawerState){
                if(isOpen) close() else open()
            }
        }
    }
}

@Composable
fun rememberFABState(fabOpened: MutableState<Boolean> = remember { mutableStateOf(false) }): FABState{
    val dYaction = remember { mutableFloatStateOf(0f) }
    val dXaction = remember { mutableFloatStateOf(0f) }
    val dYback = remember { mutableFloatStateOf(0f) }
    val dYforward = remember { mutableFloatStateOf(0f) }
    val dYrefresh = remember { mutableFloatStateOf(0f) }

    val posForward = animateIntOffsetAsState(
        targetValue = IntOffset(dXaction.floatValue.roundToInt(), dYforward.floatValue.roundToInt()),
        label = "Forward"
    )
    val posBack = animateIntOffsetAsState(
        IntOffset(dXaction.floatValue.roundToInt(),dYback.floatValue.roundToInt()),
        label = "Back"
    )
    val posRefresh = animateIntOffsetAsState(
        IntOffset(dXaction.floatValue.roundToInt(),dYrefresh.floatValue.roundToInt()),
        label = "Refresh"
    )

    when(fabOpened.value){
        true -> LaunchedEffect(fabOpened){
            dYforward.floatValue -= 630; dYback.floatValue -= 420; dYrefresh.floatValue -= 210
        }
        false -> {
            dYforward.floatValue = dYaction.floatValue
            dYback.floatValue = dYforward.floatValue
            dYrefresh.floatValue = dYforward.floatValue
        }
    }

    return remember {
        FABState(
            fabOpened, dYaction, dXaction, dYback, dYforward,
            dYrefresh, posForward, posBack, posRefresh
        )
    }

}

class FABState(
    val fabOpened: MutableState<Boolean>,
    val dYaction: MutableFloatState, val dXaction: MutableFloatState,
    val dYback: MutableFloatState, val dYforward: MutableFloatState,
    val dYrefresh: MutableFloatState, val posForward: State<IntOffset>,
    val posBack: State<IntOffset>, val posRefresh: State<IntOffset>
)