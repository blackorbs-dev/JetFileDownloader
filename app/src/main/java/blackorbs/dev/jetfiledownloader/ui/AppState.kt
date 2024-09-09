package blackorbs.dev.jetfiledownloader.ui

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import blackorbs.dev.jetfiledownloader.helpers.LinkHelper
import blackorbs.dev.jetfiledownloader.ui.home.WebNavigator
import blackorbs.dev.jetfiledownloader.ui.home.WebState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun rememberAppState(
    scope: CoroutineScope = rememberCoroutineScope(),
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    navController: NavHostController = rememberNavController(),
    webState: WebState = remember { WebState(initUrl =  LinkHelper.INIT_URL, scope = scope) }
): AppState {

    val backStackEntry = navController.currentBackStackEntryAsState()
    val currentPage = remember {
        derivedStateOf { Page.valueOf(
            backStackEntry.value?.destination?.route ?: Page.Home.name
        ) }
    }

    return remember {
        AppState(
            scope, drawerState, snackbarHostState, navController,
            currentPage, webState, webState.navigator
        )
    }
}

class AppState(
    private val scope: CoroutineScope,
    val drawerState: DrawerState,
    val snackbarHostState: SnackbarHostState,
    val navController: NavHostController,
    val currentPage: State<Page>,
    val webState: WebState,
    val webNavigator: WebNavigator
) {

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