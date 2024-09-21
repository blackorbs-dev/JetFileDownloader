package blackorbs.dev.jetfiledownloader.ui.home

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import blackorbs.dev.jetfiledownloader.R
import blackorbs.dev.jetfiledownloader.ui.Page
import blackorbs.dev.jetfiledownloader.ui.theme.JetTheme

fun NavGraphBuilder.homePage(
    webStateList: State<List<WebState>>,
    onNewDownload: (String, String, String, String, Long) -> Unit
){
    composable<Page.Home>{
        WebPage(
            webStateList = webStateList,
            onNewDownload = onNewDownload
        )
    }
}

@Composable
internal fun WebPage(
    webStateList: State<List<WebState>>,
    onNewDownload: (String, String, String, String, Long) -> Unit
){
    webStateList.value.forEach { webState ->
        Box(Modifier.fillMaxSize(), Alignment.TopCenter) {

            ComposeWeb(
                state = webState,
                onNewDownload = onNewDownload
            )

            if(webState.isError){
                ErrorLay { webState.navigator.reload() }
            }

            LinearProgressIndicator(
                progress = { webState.loadProgress },
                Modifier.fillMaxWidth()
            )
            if (webState.loadProgress < 1) {
                CircularProgressIndicator(Modifier.padding(top = 30.dp))
            }
        }
    }
}

@Composable
internal fun ErrorLay(onRetry: () -> Unit){
    Column(
        Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            ), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.no_connection),
            Modifier.padding(top = 30.dp, bottom = 10.dp),
            style = MaterialTheme.typography.headlineSmall
        )
        Icon(
            painterResource(R.drawable.ic_wifi_off_24),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            contentDescription = stringResource(id = R.string.no_connection)
        )
        RetryBox(onRetry = onRetry)
    }
}

@Composable
fun RetryBox(
    @StringRes titleResId: Int = R.string.error_message,
    onRetry: () -> Unit
){
    Text(
        stringResource(titleResId),
        Modifier.padding(bottom = 10.dp), textAlign = TextAlign.Center
    )
    ElevatedButton(
        onClick = { onRetry() },
        Modifier.widthIn(min = 150.dp),
        colors = ButtonDefaults.elevatedButtonColors().copy(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(stringResource(R.string.retry))
    }
}

enum class WebAction{ GoBack, GoForward, Reload }

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@PreviewLightDark
@Composable
fun ErrorLayPreview(){
    JetTheme {
        Scaffold {
            ErrorLay {
            }
        }
    }
}