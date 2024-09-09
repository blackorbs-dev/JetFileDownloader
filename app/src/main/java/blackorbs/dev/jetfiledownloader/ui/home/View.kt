package blackorbs.dev.jetfiledownloader.ui.home

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import blackorbs.dev.jetfiledownloader.R
import blackorbs.dev.jetfiledownloader.services.DownloadManager
import blackorbs.dev.jetfiledownloader.ui.Page
import blackorbs.dev.jetfiledownloader.ui.theme.JetTheme

fun NavGraphBuilder.homePage(
    webState: WebState,
    downloadManager: DownloadManager
){
//    navigation(route = Page.Home.name, startDestination = ""){
//
//    }
    composable(Page.Home.name){
        HomePage(
            webState = webState,
            downloadManager = downloadManager
        )
    }
}

@Composable
internal fun HomePage(webState: WebState, downloadManager: DownloadManager){

    Box(Modifier.fillMaxSize(), Alignment.TopCenter) {

        ComposeWeb(
            state = webState,
            onNewDownload = downloadManager::execute
        )

        if(webState.isError){
            ErrorLay { webState.navigator.reload() }
        }

        LinearProgressIndicator(progress = { webState.loadProgress }, Modifier.fillMaxWidth())
        if (webState.loadProgress < 1) {
            CircularProgressIndicator(Modifier.padding(top = 30.dp))
        }
    }
}

@Composable
fun ErrorLay(onRetry: () -> Unit){
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.no_connection),
            Modifier.padding(top = 30.dp, bottom = 10.dp),
            style = MaterialTheme.typography.headlineSmall
        )
        Icon(painterResource(R.drawable.ic_wifi_off_24), contentDescription = stringResource(
            id = R.string.no_connection
        ))
        Text(
            stringResource(R.string.error_message),
            Modifier.padding(bottom = 10.dp)
        )
        ElevatedButton(onClick = { onRetry() }) {
            Text(stringResource(R.string.retry))
        }
    }
}

enum class ActionType{
    GoBack, GoForward, Reload
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview
@Composable
fun ErrorLayPreview(){
    JetTheme {
        Scaffold {
            ErrorLay {
            }
        }
    }
}