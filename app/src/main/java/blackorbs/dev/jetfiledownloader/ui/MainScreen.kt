package blackorbs.dev.jetfiledownloader.ui

import android.annotation.SuppressLint
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DismissibleDrawerSheet
import androidx.compose.material3.DismissibleNavigationDrawer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import blackorbs.dev.jetfiledownloader.R
import blackorbs.dev.jetfiledownloader.entities.ActionType
import blackorbs.dev.jetfiledownloader.services.DownloadManager
import blackorbs.dev.jetfiledownloader.ui.download.downloadPage
import blackorbs.dev.jetfiledownloader.ui.download.rememberDownloadManager
import blackorbs.dev.jetfiledownloader.ui.favorite.favoritePage
import blackorbs.dev.jetfiledownloader.ui.home.WebAction
import blackorbs.dev.jetfiledownloader.ui.home.homePage
import blackorbs.dev.jetfiledownloader.ui.theme.JetTheme
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Composable
internal fun MainScreen(
    appState: AppState = rememberAppState(),
    downloadManager: DownloadManager = rememberDownloadManager(
        onMessage = appState::showMessage
    )
){
    DismissibleNavigationDrawer(
        drawerContent = {
            NavBar(currentPage = appState.currentPage.value) { page ->
                appState.navController.load(page)
                appState.showOrHideDrawer()
            }
        },
        drawerState = appState.drawerState,
        gesturesEnabled = appState.drawerState.isOpen
    ) {
        Scaffold(
            topBar = {
                if(appState.currentPage.value !is Page.FileInfo) {
                    TopBar(
                        currentPage = appState.currentPage.value,
                        isDrawerClosed = appState.drawerState.isClosed,
                        isFavorite = appState.favViewModel.isFavorite,
                        onMenuClicked = appState::showOrHideDrawer,
                        onFavIconClicked = appState::onAddFavorite,
                        onLoadAction = appState.currentWebState.navigator::loadUrl,
                        selectedItemsCount = downloadManager.selectedItemCount,
                        isPendingDelete = downloadManager.isPendingDelete,
                        onDownloadAction = downloadManager::onSelectedAction
                    )
                }
            },
            floatingActionButton = {
                if(appState.currentPage.value is Page.Home){
                    FloatingActionButtons(
                        onFabClicked = { actionType ->
                            when(actionType){
                                WebAction.GoBack ->
                                    appState.currentWebState.navigator.goBack()
                                WebAction.GoForward ->
                                    appState.currentWebState.navigator.goForward()
                                WebAction.Reload ->
                                    appState.currentWebState.navigator.reload()
                            }
                        }
                    )
                }
            },
            snackbarHost = { SnackbarHost(hostState = appState.snackbarHostState) }
        ) { paddingValues ->
            NavHost(
                modifier = Modifier.padding(paddingValues),
                navController = appState.navController,
                startDestination = Page.Home()
            ) {
                favoritePage(
                    onShouldLoadPage = appState::onLoadFavoriteUrl
                )
                downloadPage(
                    onDownloadRequest = downloadManager::execute,
                    onDismissBtnClicked = {
                        downloadManager.permissionManager
                            .updateNotifPermission(false)
                    },
                    onShouldRequestNotifPermission =
                        downloadManager.permissionManager::showNotifPermission,
                    onShowInfo = {
                        appState.navController.load(Page.FileInfo(it))
                    },
                    onCloseFileInfoPage = {
                        appState.navController.load(Page.DownloadList())
                    }
                )
                homePage(
                    webStateList = appState.webStateList,
                    onNewDownload = downloadManager::execute
                )
            }
            BackHandler(
                appState.currentPage.value !is Page.Home
                        || appState.currentWebState.navigator.canGoBack
            ) {
                when(appState.currentPage.value){
                    is Page.FileInfo -> appState.navController
                        .load(Page.DownloadList())
                    !is Page.Home -> appState.navController.load(Page.Home())
                    else -> appState.currentWebState.navigator.goBack()
                }
            }
        }
    }
}

@Composable
internal fun NavBar(currentPage: Page, onNavItemClicked: (page: Page) -> Unit){
    DismissibleDrawerSheet(
        Modifier.background(
            brush = Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.background
                ),
                start = Offset(Float.POSITIVE_INFINITY,1200f),
                end = Offset(0f, Float.POSITIVE_INFINITY)
            ),
            shape = RoundedCornerShape(bottomEnd = 24.dp)
        ),
        drawerContainerColor = Color.Transparent
    ) {
        Icon(
            painterResource(R.drawable.ic_download_for_offline_68),
            contentDescription = stringResource(R.string.app_name),
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 16.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Text(stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(top = 10.dp, bottom = 20.dp)
                .align(Alignment.CenterHorizontally)
        )
        HorizontalDivider()
        Spacer(Modifier.height(20.dp))
        listOf(
            Page.Home(), Page.DownloadList(), Page.Favorites(),
        ).forEach{ page ->
            NavigationDrawerItem(
                selected = page == currentPage,
                onClick = { onNavItemClicked(page) },
                label = { Text(stringResource(page.titleRes),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ) },
                icon = { Icon(painterResource(page.iconRes),
                    contentDescription = stringResource(page.titleRes)
                ) },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TopBar(
    currentPage: Page, isDrawerClosed: Boolean,
    isFavorite: Boolean = false, onMenuClicked: () -> Unit,
    onFavIconClicked: () -> Unit, onLoadAction: (text: String) -> Unit,
    selectedItemsCount: Int, isPendingDelete: Boolean, onDownloadAction: (ActionType) -> Unit
) {
    var value by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            if(currentPage is Page.Home){
                TextField(
                    value = value,
                    onValueChange = { value = it },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        focusManager.clearFocus()
                        onLoadAction(value)
                    }),
                    trailingIcon = {
                        if(value.isNotBlank() && isFocused) {
                            IconButton(onClick = { value = "" }) {
                                Icon(
                                    painterResource(R.drawable.ic_close_24),
                                    contentDescription = stringResource(R.string.cancel),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    },
                    singleLine = true, placeholder = {
                        Text(stringResource(R.string.enter_url))
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                        .padding(start = 10.dp, end = 10.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSecondary,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSecondary,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSecondary,
                        unfocusedPlaceholderColor =  MaterialTheme.colorScheme.onSecondary,
                        cursorColor = MaterialTheme.colorScheme.background
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
            else if(currentPage is Page.Main){
                Text(stringResource(currentPage.titleRes))
            }
        },
        navigationIcon = {
            IconButton(onClick = { onMenuClicked() }) {
                Icon(
                    if(isDrawerClosed) Icons.Filled.Menu
                    else Icons.Filled.Close,
                    contentDescription = stringResource(R.string.home)
                )
            }
        },
        actions = {
            if(currentPage is Page.Home){
                TopIconButton(
                    iconResId = if(isFavorite) R.drawable.ic_favorite_24
                                 else R.drawable.ic_favorite_border_24,
                    contentDescResId = R.string.favorite
                ) { onFavIconClicked() }
            }
            else if(currentPage is Page.DownloadList){
                if(isPendingDelete){
                    CircularProgressIndicator(Modifier.scale(0.6f))
                }
                else if(selectedItemsCount > 1){
                    TopIconButton(
                        iconResId = R.drawable.ic_share_24,
                        contentDescResId = R.string.share
                    ) { onDownloadAction(ActionType.Share) }
                    TopIconButton(
                        iconResId = R.drawable.ic_delete_sweep_24,
                        contentDescResId = R.string.delete
                    ) { onDownloadAction(ActionType.Delete) }
                }
            }
        }
    )
}

@Composable
internal fun TopIconButton(
    modifier: Modifier = Modifier,
    @DrawableRes iconResId: Int,
    @StringRes contentDescResId: Int,
    onClick: () -> Unit
){
    IconButton(modifier = modifier, onClick = onClick) {
        Icon(
            painterResource(iconResId),
            contentDescription = stringResource(contentDescResId)
        )
    }
}

@Composable
internal fun FloatingActionButtons(onFabClicked: (webAction: WebAction) -> Unit){

    val fabState = rememberFABState()

    FloatingActionButton(onClick = { onFabClicked(WebAction.GoForward) }, Modifier.offset { fabState.posForward.value }) {
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription =  stringResource(R.string.navigate),
            Modifier.scale(1.5f),
            tint = MaterialTheme.colorScheme.background
        )
    }
    FloatingActionButton(onClick = { onFabClicked(WebAction.GoBack) }, Modifier.offset { fabState.posBack.value }) {
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            stringResource(R.string.navigate),
            Modifier.scale(1.5f),
            tint = MaterialTheme.colorScheme.background
        )
    }
    FloatingActionButton(onClick = {onFabClicked(WebAction.Reload) }, Modifier.offset {fabState.posRefresh.value}) {
        Icon(
            Icons.Filled.Refresh,
            stringResource(R.string.navigate),
            Modifier.scale(1.3f),
            tint = MaterialTheme.colorScheme.background
        )
    }
    FloatingActionButton(
        onClick = { fabState.fabOpened.value = fabState.dYaction.floatValue==fabState.dYforward.floatValue },
        modifier = Modifier
            .offset {
                IntOffset(
                    fabState.dXaction.floatValue.roundToInt(),
                    fabState.dYaction.floatValue.roundToInt()
                )
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    fabState.fabOpened.value = false
                    change.consume()
                    fabState.dYaction.floatValue += dragAmount.y
                    fabState.dYforward.floatValue = fabState.dYaction.floatValue
                    fabState.dYback.floatValue = fabState.dYaction.floatValue
                    fabState.dYrefresh.floatValue = fabState.dYaction.floatValue
                    fabState.dXaction.floatValue += dragAmount.x
                }
            }
    ) {
        Icon(
            painterResource(
                if(fabState.dYforward.floatValue==fabState.dYaction.floatValue) R.drawable.ic_navigate_api_24
                else R.drawable.ic_close_24
            ),
            stringResource(R.string.navigate),
            Modifier.scale(1.3f),
            tint = MaterialTheme.colorScheme.background
        )
    }
}

@Serializable sealed interface Page{
    @Serializable open class Main(
        @StringRes val titleRes: Int,
        @DrawableRes val iconRes: Int
    ): Page

    @Serializable data class Home(
        @StringRes val titleResId: Int = R.string.home,
        @DrawableRes val iconResId: Int = R.drawable.ic_home_24
    ): Main(titleResId, iconResId)

    @Serializable data class DownloadList(
        @StringRes val titleResId: Int = R.string.download,
        @DrawableRes val iconResId: Int = R.drawable.ic_download_24
    ): Main(titleResId, iconResId)

    @Serializable data class FileInfo(val downloadId: Long): Page

    @Serializable data class Favorites(
        @StringRes val titleResId: Int = R.string.favorite,
        @DrawableRes val iconResId: Int = R.drawable.ic_favorite_24
    ): Main(titleResId, iconResId)
}

fun NavController.load(page: Page) = navigate(page){
    popUpTo(graph.findStartDestination().id){
        saveState = true
    }
    launchSingleTop = true
    restoreState = true
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@PreviewLightDark
@Composable
fun MainScreenPreview(){
    JetTheme {
        Scaffold(
            topBar = {
                TopBar(
                    currentPage = Page.FileInfo(-1),
                    isDrawerClosed = true,
                    onMenuClicked = {},
                    onFavIconClicked = {},
                    onLoadAction = {},
                    selectedItemsCount = 6,
                    isPendingDelete = false
                ){}
            },
            floatingActionButton = {
                FloatingActionButtons {}
            }
        ) {  }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES, showSystemUi = true)
@Preview(showSystemUi = true)
@Composable
fun DrawerPreview(){
    JetTheme {
        NavBar(currentPage = Page.Home()) {}
    }
}