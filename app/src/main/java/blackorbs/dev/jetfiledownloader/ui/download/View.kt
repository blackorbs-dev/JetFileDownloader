package blackorbs.dev.jetfiledownloader.ui.download

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.paging.compose.collectAsLazyPagingItems
import blackorbs.dev.jetfiledownloader.R
import blackorbs.dev.jetfiledownloader.entities.ActionType
import blackorbs.dev.jetfiledownloader.entities.Download
import blackorbs.dev.jetfiledownloader.entities.Status
import blackorbs.dev.jetfiledownloader.services.DownloadManager
import blackorbs.dev.jetfiledownloader.ui.Page
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun NavGraphBuilder.downloadPage(
    downloadManager: DownloadManager
){
    composable(Page.Download.name){
        DownloadPage(downloadManager = downloadManager)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DownloadPage(
    context: Context = LocalContext.current,
    downloadVm: DownloadVm = viewModel(context as ComponentActivity),
    downloadManager: DownloadManager
){
    val downloads = downloadVm.downloads.collectAsLazyPagingItems()
    val groupedData: State<Map<LocalDate, List<Download>>> = remember {
        derivedStateOf {
            downloadVm.newDownloads.value.sortedByDescending { it.dateTime }
                .plus(downloads.itemSnapshotList.items)
                .groupBy { it.dateTime.toLocalDate() }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(5.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if(downloadManager.permissionManager.permissionNum == 1 || downloadVm.showNotifBox.value){
            item {
                NotifBox(
                    onDismiss = {
                        downloadManager.permissionManager
                            .updateNotifPermission(false) },
                    onShouldRequest = downloadManager.permissionManager::showNotifPermission
                )
            }
        }
        groupedData.value.forEach { (date, downloadList) ->
            stickyHeader(key = date) {
                Text(date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")))
            }

            items(items = downloadList, key = {it.id}){
                DownloadItem(
                    context = context, download = it,
                    onClick = downloadManager::execute
                )
            }
        }
    }
}

@Composable
fun DownloadItem(context: Context, download: Download?, onClick: (Download) -> Unit){

    fun isNotCompleted() = download!!.status.value != Status.Success
            && download.status.value != Status.Deleted

    fun isPending() = download!!.status.value == Status.Queued
            || download.status.value == Status.Ongoing

    download?.let {
        Card(onClick = {onClick(download.apply{ actionType = ActionType.None })}, Modifier.fillMaxWidth()) {
            Text(
                download.dateTime.toLocalTime().format(
                    DateTimeFormatter.ofPattern("h:mm a")
                ),
                Modifier
                    .background(
                        color = MaterialTheme.colorScheme.secondary
                    )
                    .padding(3.dp)
            )
            Row {
                Text(
                    download.fileName.substring(it.fileName.lastIndexOf('.')+1),
                    Modifier
                        .background(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = RoundedCornerShape(15.dp)
                        )
                        .padding(10.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text(download.fileName)
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = download.statusColor)){
                                append(stringResource(download.status.value.titleResID))
                            }
                            append(
                                " (${
                                    if(isNotCompleted()) "${download.sizePercent}% — "
                                    else ""
                                }${download.totalMB(context)})"
                            )
                        }
                    )
                }
                if(isNotCompleted()){
                    IconButton(onClick = {
                        onClick(download.apply{
                            actionType = if(isPending()) ActionType.Pause
                                        else ActionType.Resume
                        })
                    }) {
                        Icon(painterResource(
                            if(isPending()) R.drawable.ic_pause_circle_24
                            else R.drawable.ic_play_circle_24
                        ),
                            contentDescription = stringResource(download.status.value.titleResID)
                        )
                    }
                }
            }
            if(download.status.value == Status.Error){
                Text(download.status.value.text)
            }
        }
    }
}

@Composable
fun NotifBox(onDismiss: () -> Unit, onShouldRequest: () -> Unit){
    OutlinedCard {
        Text(
            stringResource(R.string.permission_message),
            Modifier.padding(10.dp, 10.dp, 10.dp, bottom = 5.dp)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(
                onClick = onDismiss,
                Modifier.padding(start = 10.dp, bottom = 5.dp)
            ) {
                Text(stringResource(R.string.dismiss))
            }
            Button(
                onClick = onShouldRequest,
                Modifier.padding(end = 10.dp, bottom = 5.dp)
            ) {
                Text(stringResource(R.string.grant_permission))
            }
        }
    }
}