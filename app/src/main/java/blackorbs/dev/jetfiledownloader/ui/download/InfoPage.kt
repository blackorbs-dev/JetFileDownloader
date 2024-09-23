package blackorbs.dev.jetfiledownloader.ui.download

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.viewmodel.compose.viewModel
import blackorbs.dev.jetfiledownloader.R
import blackorbs.dev.jetfiledownloader.entities.ActionType
import blackorbs.dev.jetfiledownloader.entities.Download
import blackorbs.dev.jetfiledownloader.entities.Status
import blackorbs.dev.jetfiledownloader.entities.formatAsFileSize
import blackorbs.dev.jetfiledownloader.services.Notifier
import blackorbs.dev.jetfiledownloader.ui.TopIconButton
import blackorbs.dev.jetfiledownloader.ui.shareFiles
import blackorbs.dev.jetfiledownloader.ui.startActivityWithChooser
import blackorbs.dev.jetfiledownloader.ui.theme.JetTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
internal fun InfoPage(
    downloadId: Long,
    context: Context = LocalContext.current,
    downloadVm: DownloadVM =
        viewModel(context as ComponentActivity),
    onShouldGoBack: () -> Unit
){
    val download = downloadVm.download.value
    LaunchedEffect(downloadId) {
        downloadVm.getDownload(downloadId)
    }

    MainLayout(download,
        onDownloadAction = {
            when(it){
                ActionType.None -> onShouldGoBack()
                ActionType.Share ->
                    download?.run {context.shareFiles(listOf(filePath))}
                ActionType.Delete -> {
                    downloadVm.deleteSelectedItems()
                    onShouldGoBack()
                }
                ActionType.ShowInfo -> {
                    download?.run {
                        if(status.value == Status.Success){
                            context.startActivityWithChooser(
                                Notifier.getFileIntent(context, filePath),
                                R.string.open_file_using
                            )
                        }
                        else
                            Toast.makeText(
                                context, R.string.download_not_completed,
                                Toast.LENGTH_LONG).show()
                    }
                }
                else -> {}
            }
        }
    )
}

@Composable
fun MainLayout(
    download: Download?,
    onDownloadAction: (ActionType) -> Unit
){
    Column(
        Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    ),
                    startY = 500f
                ),
            )
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Row (
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ){
            TopIconButton(
                iconResId = R.drawable.ic_keyboard_backspace_24,
                contentDescResId = R.string.go_back
            ) { onDownloadAction(ActionType.None) }
            TopIconButton(
                iconResId = R.drawable.ic_share_24,
                contentDescResId = R.string.share
            ) { onDownloadAction(ActionType.Share) }
        }
        if(download == null){
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        else DownloadInfo(
            download = download,
            onDownloadAction = onDownloadAction
        )
    }
}

@Composable
fun DownloadInfo(
    download: Download,
    onDownloadAction: (ActionType) -> Unit
){
    Text(
        download.type.uppercase(),
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(top = 20.dp)
            .drawFileTypeBack(download.type)
            .padding(12.dp),
        color = MaterialTheme.colorScheme.background
    )
    Text(
        download.fileName, textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(20.dp),
        lineHeight = 1.6.em
    )

    val listItems = listOf(
        ListItem.RowItem(R.string.file_size, "${if(download.isNotCompleted)
            stringResource(R.string.formated_size, download.currentSize.formatAsFileSize()) else ""} ${
            download.totalSize.formatAsFileSize() }"),
        ListItem.RowItem(R.string.status, stringResource(download.status.value.titleResID)),
        ListItem.ColumnItem(R.drawable.ic_insert_link_24, download.url),
        ListItem.ColumnItem(R.drawable.ic_folder_copy_24, download.filePath),
        ListItem.RowItem(R.string.date, download.dateTime.format(
            DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:m a")
        ))
    )

    LazyColumn(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 4.dp)
    ) {
        itemsIndexed(items = listItems){ index, item ->
            if(item is ListItem.RowItem){
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            bottom = 2.dp, top = 12.dp,
                            start = 2.dp, end = 2.dp
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(item.titleResId),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(item.text,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }
            else if(item is ListItem.ColumnItem){
                Column(
                    Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painterResource(item.iconResId),
                        contentDescription = item.text,
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 8.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        item.text, maxLines = 3, style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 6.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if(index != listItems.size - 1){
                HorizontalDivider()
            }
        }
    }

    RowActionButtons(
        Modifier.padding(top = 10.dp),
        leftTitleRes = R.string.delete,
        rightTileRes = R.string.open,
        onLeftClicked = { onDownloadAction(ActionType.Delete) },
        onRightClicked = { onDownloadAction(ActionType.ShowInfo) }
    )
}

internal sealed interface ListItem{
    data class RowItem(@StringRes val titleResId: Int, val text: String): ListItem
    data class ColumnItem(@DrawableRes val iconResId: Int, val text: String): ListItem
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "UnrememberedMutableState")
@PreviewLightDark
@Composable
fun MainPreview(){
    JetTheme {
        Scaffold {
            MainLayout(
                Download(
                url = "https://www.google.com/download-test-document-psf-testtetdgd", fileName = "Esnglish-textagsgsghsghsdgfhjhgddhgfhgd.pdf", totalSize = 145670L,
                status = mutableStateOf(Status.Paused.apply { text = "Some error occured" })
            ).apply {
                currentSize = 12000
                filePath = "storage/emulated/Download/test.pdf"
                dateTime = LocalDateTime.now()
            }){}
        }
    }
}