package blackorbs.dev.jetfiledownloader.ui

import android.util.Patterns
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DismissibleDrawerSheet
import androidx.compose.material3.DismissibleNavigationDrawer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import blackorbs.dev.jetfiledownloader.R
import blackorbs.dev.jetfiledownloader.helpers.LinkHelper
import blackorbs.dev.jetfiledownloader.ui.download.downloadPage
import blackorbs.dev.jetfiledownloader.ui.download.rememberDownloadManager
import blackorbs.dev.jetfiledownloader.ui.favorite.favoritePage
import blackorbs.dev.jetfiledownloader.ui.home.ActionType
import blackorbs.dev.jetfiledownloader.ui.home.homePage
import blackorbs.dev.jetfiledownloader.ui.theme.JetTheme
import kotlin.math.roundToInt

@Composable
fun MainScreen(mainServiceHolder: MainServiceHolder){
    val context = LocalContext.current
    val appState = rememberAppState()
    val downloadManager = rememberDownloadManager(
        mainServiceHolder = mainServiceHolder,
        onMessage = {
            appState.showMessage(context.getString(it))
        }
    )

    DismissibleNavigationDrawer(
        drawerContent = {
            NavBar(currentPage = appState.currentPage.value) { page ->
                appState.navController.load(page.name)
                appState.showOrHideDrawer()
            }
        },
        drawerState = appState.drawerState,
        gesturesEnabled = appState.drawerState.isOpen
    ) {
        Scaffold(
            topBar = {
                TopBar(
                    currentPage = appState.currentPage.value,
                    isDrawerClosed = appState.drawerState.isClosed,
                    onMenuClicked = { appState.showOrHideDrawer() },
                    onFavIconClicked = {
                        appState.showMessage("Fav icon clicked")
                    }
                ) { text ->
                    appState.webNavigator.loadUrl(
                        if(Patterns.WEB_URL.matcher(text).matches()) LinkHelper.getAsHttps(text)
                        else "https://www.google.com/search?q=$text"
                    )
                }
            },
            floatingActionButton = {
                if(appState.currentPage.value == Page.Home){
                    FloatingActionButtons(
                        onFabClicked = { actionType ->
                            when(actionType){
                                ActionType.GoBack -> appState.webNavigator.goBack()
                                ActionType.GoForward -> appState.webNavigator.goForward()
                                ActionType.Reload -> appState.webNavigator.reload()
                            }
                        }
                    )
                }
            },
            snackbarHost = { SnackbarHost(hostState = appState.snackbarHostState) }
        ) {
            NavHost(
                modifier = Modifier.padding(it),
                navController = appState.navController,
                startDestination = Page.Home.name
            ) {
                favoritePage()
                downloadPage(downloadManager)
                homePage(appState.webState, downloadManager)
            }
        }
    }
}

@Composable
fun NavBar(currentPage: Page, onNavItemClicked: (page: Page) -> Unit){
    DismissibleDrawerSheet {
        listOf(
            Page.Home, Page.Download, Page.Favorite,
        ).forEach{ item ->
            NavigationDrawerItem(
                selected = item == currentPage,
                onClick = { onNavItemClicked(item) },
                label = { Text(stringResource(item.titleRes)) },
                icon = { Icon(painterResource(item.iconRes), contentDescription = item.name) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(currentPage: Page, isDrawerClosed: Boolean, onMenuClicked: () -> Unit, onFavIconClicked: () -> Unit, onLoadAction: (text: String) -> Unit){
    TopAppBar(
        title = {
            if(currentPage == Page.Home){
                var value by remember { mutableStateOf("") }
                val focusManager = LocalFocusManager.current

                TextField(
                    value = value,
                    onValueChange = { value = it },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        focusManager.clearFocus()
                        onLoadAction(value)
                        value = ""
                    }),
                    placeholder = {
                        Text(stringResource(R.string.enter_url))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSecondary,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSecondary,
                        unfocusedPlaceholderColor =  MaterialTheme.colorScheme.onSecondary
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
            else{
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
            if(currentPage == Page.Home){
                IconButton(onClick = { onFavIconClicked() }) {
                    Icon(
                        painterResource(R.drawable.ic_favorite_24 ),
                        contentDescription = stringResource(R.string.favorite)
                    )
                }
            }
        }
    )
}

@Composable
fun FloatingActionButtons(onFabClicked: (actionType: ActionType) -> Unit){

    val fabState = rememberFABState()

    FloatingActionButton(onClick = { onFabClicked(ActionType.GoForward) }, Modifier.offset { fabState.posForward.value }) {
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription =  stringResource(R.string.navigate),
            Modifier.scale(1.5f),
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
    FloatingActionButton(onClick = { onFabClicked(ActionType.GoBack) }, Modifier.offset { fabState.posBack.value }) {
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            stringResource(R.string.navigate),
            Modifier.scale(1.5f),
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
    FloatingActionButton(onClick = {onFabClicked(ActionType.Reload) }, Modifier.offset {fabState.posRefresh.value}) {
        Icon(
            Icons.Filled.Refresh,
            stringResource(R.string.navigate),
            Modifier.scale(1.3f),
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
    FloatingActionButton(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = MaterialTheme.colorScheme.onTertiary,
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
            Modifier.scale(1.3f)
        )
    }
}

enum class Page(@StringRes var titleRes: Int, @DrawableRes val iconRes: Int){
    Home(R.string.home, R.drawable.ic_home_24),
    Download(R.string.download, R.drawable.ic_download_24),
    Favorite(R.string.favorite, R.drawable.ic_favorite_24)
}

fun NavController.load(route: String) = navigate(route){
    popUpTo(graph.findStartDestination().id){
        saveState = true
    }
    launchSingleTop = true
    restoreState = true
}

@Preview
@Composable
fun MainScreenPreview(){
    JetTheme {
        Scaffold(
            topBar = {
                TopBar(
                    currentPage = Page.Home,
                    isDrawerClosed = true,
                    onMenuClicked = {},
                    onFavIconClicked = {},
                    onLoadAction = {}
                )
            }
        ) {
            Surface(Modifier.padding(it)) {}
        }

    }
}

@Preview
@Composable
fun DrawerPreview(){
    JetTheme {
        NavBar(currentPage = Page.Home) {}
    }
}