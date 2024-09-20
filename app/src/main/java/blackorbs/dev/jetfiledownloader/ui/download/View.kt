package blackorbs.dev.jetfiledownloader.ui.download

import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import blackorbs.dev.jetfiledownloader.MainApp
import blackorbs.dev.jetfiledownloader.R
import blackorbs.dev.jetfiledownloader.entities.ActionType
import blackorbs.dev.jetfiledownloader.entities.Download
import blackorbs.dev.jetfiledownloader.entities.LayoutState
import blackorbs.dev.jetfiledownloader.entities.Status
import blackorbs.dev.jetfiledownloader.entities.formatAsFileSize
import blackorbs.dev.jetfiledownloader.ui.Page
import blackorbs.dev.jetfiledownloader.ui.home.RetryBox
import blackorbs.dev.jetfiledownloader.ui.shareFiles
import blackorbs.dev.jetfiledownloader.ui.theme.JetTheme
import blackorbs.dev.jetfiledownloader.ui.theme.Red40
import blackorbs.dev.jetfiledownloader.ui.theme.statusColor
import blackorbs.dev.jetfiledownloader.ui.theme.typeColor
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun NavGraphBuilder.downloadPage(
    onDismissBtnClicked: () -> Unit,
    onShouldRequestNotifPermission: () -> Unit,
    onDownloadRequest: (Download) -> Unit,
    onShowInfo: (Long) -> Unit,
    onCloseFileInfoPage: () -> Unit
){
        composable<Page.DownloadList>{
            DownloadPage(
                onDismissBtnClicked = onDismissBtnClicked,
                onShouldRequestNotifPermission =
                onShouldRequestNotifPermission,
                onDownloadRequest = onDownloadRequest,
                onShowInfo = onShowInfo
            )
        }

        composable<Page.FileInfo>{
            InfoPage(
                it.toRoute<Page.FileInfo>().downloadId,
                onShouldGoBack = onCloseFileInfoPage
            )
        }
}

@Composable
internal fun DownloadPage(
    context: Context = LocalContext.current,
    downloadVm: DownloadVM = viewModel(context as ComponentActivity),
    onDismissBtnClicked: () -> Unit,
    onShouldRequestNotifPermission: () -> Unit,
    onDownloadRequest: (Download) -> Unit,
    onShowInfo: (Long) -> Unit
){
    val mainApp = context.applicationContext as MainApp
    val downloads = downloadVm.downloads.collectAsLazyPagingItems()
    val itemCount =
        downloads.itemCount + mainApp.newDownloadsCount.intValue

    fun handleOnclick(download: Download){
        when(download.actionType) {
            ActionType.None -> {
                if(downloadVm.selectedItemCount.value > 0
                    && !downloadVm.isPendingDelete)
                    downloadVm.setSelection(download)
                else {
                    onDownloadRequest(download)
                }
            }
            ActionType.Pause, ActionType.Resume -> {
                onDownloadRequest(download)
            }
            ActionType.Select -> {
                downloadVm.setSelection(download)
            }
            ActionType.Delete -> { downloadVm.deleteSelectedItems() }
            ActionType.UndoDelete -> {
                download.isPendingDelete.value = false
                downloadVm.setSelection(download)
            }
            ActionType.ShowInfo -> { onShowInfo(download.id) }
            ActionType.Share -> {
                context.shareFiles(listOf(download.filePath))
            }
        }
    }

    Column {
        AnimatedVisibility(downloadVm.showNotifBox.value) {
            NotifBox(
                onDismiss = onDismissBtnClicked,
                onShouldRequest = onShouldRequestNotifPermission
            )
        }
        LazyColumnWithLoadingState(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            loadState = downloads.loadState, itemCount = itemCount,
            notLoadingTextResId = R.string.downloaded_files,
            onRetry = downloads::retry
        ){
            var lastDate: LocalDate? = null
            downloadVm.newDownloads.value.forEach {
                lastDate = downloadItem(
                    it, lastDate,
                    selectedItemCount = downloadVm.selectedItemCount,
                    onClick = ::handleOnclick
                )
            }
            for(index in 0 until downloads.itemCount){
                lastDate = downloadItem(
                    downloads[index], lastDate,
                    selectedItemCount = downloadVm.selectedItemCount,
                    onClick = ::handleOnclick
                )
            }
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
internal fun LazyListScope.downloadItem(
    download: Download?, lastDate: LocalDate?,
    selectedItemCount: State<Int>,
    onClick: (Download) -> Unit
): LocalDate?{
    val date = download?.dateTime?.toLocalDate()
    download?.let {
        if(download.status.value == Status.Deleted)
            return lastDate
        if(lastDate != date){
            item(key = date?.dayOfMonth) {
                Spacer(Modifier.height(16.dp))
            }
            stickyHeader(key = date?.toString()) {
                Text(
                    date!!.format(),
                    Modifier
                        .animateItem()
                        .padding(bottom = 2.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(
                            start = 12.dp, end = 12.dp,
                            top = 6.dp, bottom = 6.dp
                        ),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        item(key = download.id) {
            DownloadItem(
                modifier = Modifier.animateItem(),
                download = download, onClick = onClick,
                selectedItemCount = selectedItemCount
            )
        }
    }
    return date
}

@Composable
internal fun DownloadItem(
    modifier: Modifier = Modifier,
    selectedItemCount: State<Int>,
    download: Download, onClick: (Download) -> Unit
){
    val density = LocalDensity.current

    ElevatedCard(modifier = modifier
        .padding(5.dp)
        .pointerInput(Unit) {
            detectTapGestures(
                onLongPress = {
                    onClick(download.apply {
                        actionType = ActionType.Select
                    })
                },
                onTap = {
                    onClick(download.apply {
                        actionType = ActionType.None
                    })
                }
            )
        }
    ) {
        Box {
            Row(Modifier.onGloballyPositioned {
                download.height = with(density){ it.size.height.toDp() }
            }) {
                Column {
                    Text(
                        download.dateTime.toLocalTime().format(
                            DateTimeFormatter.ofPattern("h:mm a")
                        ).uppercase(),
                        Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(bottomEnd = 8.dp)
                            )
                            .padding(start = 4.dp, end = 4.dp),
                        color = MaterialTheme.colorScheme.background,
                        fontSize = 12.sp
                    )
                    Text(
                        download.type.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.background, maxLines = 1, modifier =
                        Modifier
                            .padding(start = 12.dp, top = 10.dp, end = 10.dp, bottom = 10.dp)
                            .drawFileTypeBack(download.type)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        download.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(
                            top = 20.dp,
                            end = if (download.isNotCompleted)
                                0.dp else 12.dp
                        )
                    )
                    val percent =
                        animateIntAsState(download.sizePercent.intValue, label = "percent")
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = statusColor(download.status.value))) {
                                append(stringResource(download.status.value.titleResID))
                            }
                            append(
                                " (${
                                    if (download.isNotCompleted) "${
                                        percent.value
                                    }% — "
                                    else ""
                                }${download.totalSize.formatAsFileSize()})"
                            )
                        },
                        fontSize = 14.sp,
                    )
                    if (download.status.value == Status.Error) {
                        Text(
                            download.status.value.text, fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                if (download.isNotCompleted) {
                    FilledIconButton(
                        onClick = {
                            onClick(download.apply {
                                actionType = if (download.isPending) ActionType.Pause
                                else ActionType.Resume
                            })
                        },
                        Modifier.padding(top = 4.dp, end = 4.dp)
                    ) {
                        Icon(
                            painterResource(
                                if (download.isPending) R.drawable.ic_pause_circle_24
                                else R.drawable.ic_play_circle_24
                            ),
                            modifier = Modifier.sizeIn(minWidth = 35.dp, minHeight = 35.dp),
                            contentDescription = stringResource(download.status.value.titleResID)
                        )
                    }
                }
            }
            if(download.isSelected.value){
                val state by remember {
                    derivedStateOf {
                        when{
                            download.isPendingDelete.value ->
                                LayoutState.PendingDelete
                            selectedItemCount.value > 1 ->
                                LayoutState.SelectionMode
                            else -> LayoutState.None
                        }
                    }
                }
                val backColor by animateColorAsState(
                    if(state == LayoutState.PendingDelete)
                        Red40.copy(alpha = 0.9f)
                    else MaterialTheme.colorScheme.surfaceDim,
                    label = "BackColor"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(download.height)
                        .drawBehind {
                            drawRect(backColor)
                        },
                    contentAlignment = Alignment.Center
                ){
                    AnimatedContent(targetState = state, label = "ExtraActions") {
                        when(it){
                            LayoutState.None -> DownloadExtraActions{
                                onClick(download.apply { actionType = it })
                            }
                            LayoutState.PendingDelete -> UndoBox {
                                onClick(download.apply {
                                    actionType = ActionType.UndoDelete
                                })
                            }
                            LayoutState.SelectionMode -> {
                                val c = MaterialTheme.colorScheme.primary
                                Icon(
                                    painterResource(R.drawable.ic_check_24),
                                    contentDescription = stringResource(R.string.selected),
                                    modifier = Modifier
                                        .wrapContentSize()
                                        .size(50.dp)
                                        .drawBehind { drawCircle(color = c) }
                                        .padding(5.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun UndoBox(onUndo: () -> Unit){
    val color = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.background
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.file_deleted),
            color = textColor
        )
        Text(
            stringResource(R.string.undo),
            Modifier
                .padding(top = 3.dp)
                .background(
                    color = color,
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable { onUndo() }
                .padding(
                    start = 8.dp, end = 8.dp,
                    top = 3.dp, bottom = 3.dp
                ),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
internal fun DownloadExtraActions(
    onClick: (ActionType) -> Unit
){
    val color = MaterialTheme.colorScheme.primary
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val h = size.height
                val w = size.width
                val s = Size(0.12f * w, h * 0.49f)
                drawRect(
                    color = color,
                    size = Size(0.12f * w, h)
                )
                drawRect(
                    color = color,
                    size = s, topLeft = Offset(w - s.width, 0f)
                )
                drawRect(
                    color = color,
                    size = s, topLeft = Offset(w - s.width, h - s.height)
                )
            }
    ) {
        ActionIconButton(
            iconResId = R.drawable.ic_delete_forever_24,
            descResId = R.string.delete
        ) { onClick(ActionType.Delete) }
        Column(
            Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceAround
        ) {
            ActionIconButton(
                iconResId = R.drawable.ic_ios_share_24,
                descResId = R.string.share
            ) {onClick(ActionType.Share) }
            ActionIconButton(
                iconResId = R.drawable.ic_info_outline_24,
                descResId = R.string.info
            ) {onClick(ActionType.ShowInfo) }
        }
    }
}

@Composable
internal fun ActionIconButton(
    modifier: Modifier = Modifier,
    @DrawableRes iconResId: Int,
    @StringRes descResId: Int,
    onClick: () -> Unit
){
    IconButton(
        onClick = onClick,
        modifier = modifier
            .padding(start = 10.dp, end = 10.dp)
            .size(26.dp)
    ) {
        Icon(
            painterResource(iconResId),
            contentDescription = stringResource(descResId),
            Modifier.size(26.dp),
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
internal fun NotifBox(onDismiss: () -> Unit, onShouldRequest: () -> Unit){
    OutlinedCard(Modifier.padding(5.dp)) {
        Text(
            stringResource(R.string.permission_message),
            Modifier.padding(10.dp, 10.dp, 10.dp, bottom = 5.dp),
            color = MaterialTheme.colorScheme.primary
        )
        RowActionButtons(
            leftTitleRes = R.string.dismiss,
            rightTileRes = R.string.grant_permission,
            onLeftClicked = onDismiss, onRightClicked = onShouldRequest
        )
    }
}

@Composable
internal fun RowActionButtons(
    modifier: Modifier = Modifier,
    @StringRes leftTitleRes: Int,
    @StringRes rightTileRes: Int,
    onLeftClicked: () -> Unit,
    onRightClicked: () -> Unit
){
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        OutlinedButton(
            onClick = onLeftClicked,
            Modifier.padding(start = 10.dp, bottom = 5.dp)
        ) {
            Text(stringResource(leftTitleRes),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Button(
            onClick = onRightClicked,
            Modifier.padding(end = 10.dp, bottom = 5.dp),
            colors = ButtonDefaults.buttonColors().copy(
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                stringResource(rightTileRes)
            )
        }
    }
}

@Composable
fun LazyColumnWithLoadingState(
    modifier: Modifier, itemCount: Int,
    loadState: CombinedLoadStates,
    notLoadingTextResId: Int,
    onRetry: () -> Unit,
    content: LazyListScope.() -> Unit
){
    LazyColumn(
        modifier = if(itemCount == 0) modifier
        else Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if(itemCount == 0)
            Arrangement.Center else Arrangement.Top
    ){
        content()
        loadStateLayout(
            loadState = loadState.append,
            itemCount, onRetry = onRetry
        )
        loadStateLayout(
            loadState = loadState.refresh,
            itemCount = itemCount,
            notLoadingTextResId = notLoadingTextResId,
            onRetry = onRetry
        )
    }
}

internal fun LazyListScope.loadStateLayout(
    loadState: LoadState, itemCount: Int,
    @StringRes notLoadingTextResId: Int = -1,
    onRetry: () -> Unit
){
    val isAppend = notLoadingTextResId == -1
    item {
        if(isAppend) Spacer(Modifier.height(16.dp))
        when(loadState){
            is LoadState.Error ->
                RetryBox(
                    titleResId = R.string.loading_failed,
                    onRetry = onRetry
                )
            LoadState.Loading ->
                CircularProgressIndicator()
            is LoadState.NotLoading ->
                if((isAppend && loadState.endOfPaginationReached
                    && itemCount > 12)
                    || (!isAppend && itemCount == 0)){
                    Text(
                        if(isAppend)
                            stringResource(R.string.no_more_items)
                        else stringResource(
                            R.string.empty_list_info,
                            stringResource(notLoadingTextResId)
                        )
                    )
                }
        }
        if(isAppend) Spacer(Modifier.height(6.dp))
    }
}

@Composable
internal fun LocalDate.format(): String =
    if(this == LocalDate.now()) LocalContext.current.getString(R.string.today)
    else if(this == LocalDate.now().minusDays(1))
        LocalContext.current.getString(R.string.yesterday)
    else format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))

@Composable
internal fun Modifier.drawFileTypeBack(
    type: String, color: Color = typeColor(type)
) = drawWithCache {
    val h = size.height
    val w = size.width
    val w8 = w * 0.8f
    val w2 = w * 0.2f
    val h8 = h * 0.8f

    val envelope = Path().apply {
        arcTo(
            rect = Rect(w * 0.15f, 0f, w * 0.45f, h * 0.3f),
            startAngleDegrees = 225f, sweepAngleDegrees = 45f, forceMoveTo = false
        )
        arcTo(
            rect = Rect(w * 0.6f, 0f, w8, h * 0.2f),
            startAngleDegrees = 270f, sweepAngleDegrees = 90f, forceMoveTo = false
        )
        arcTo(
            rect = Rect(w * 0.6f, h8, w8, h),
            startAngleDegrees = 0f, sweepAngleDegrees = 90f, forceMoveTo = false
        )
        arcTo(
            rect = Rect(0f, h8, w2, h),
            startAngleDegrees = 90f, sweepAngleDegrees = 90f, forceMoveTo = false
        )
        arcTo(
            rect = Rect(0f, h * 0.15f, w * 0.3f, h * 0.45f),
            startAngleDegrees = 180f, sweepAngleDegrees = 45f, forceMoveTo = false
        )
        close()
        moveTo(w * 0.25f, 0f)
        arcTo(
            rect = Rect(w * 0.15f, h * 0.15f, w * 0.25f, h * 0.25f),
            startAngleDegrees = 0f, sweepAngleDegrees = 90f, forceMoveTo = false
        )
        lineTo(0f, h * 0.25f)
    }

    val rect = Path().apply {
        moveTo(w2, h * 0.4f)
        arcTo(
            rect = Rect(w8, h * 0.4f, w, h * 0.6f),
            startAngleDegrees = 270f, sweepAngleDegrees = 90f, forceMoveTo = false
        )
        arcTo(
            rect = Rect(w8, h * 0.65f, w, h * 0.85f),
            startAngleDegrees = 0f, sweepAngleDegrees = 90f, forceMoveTo = false
        )
        arcTo(
            rect = Rect(w2, h * 0.65f, w * 0.4f, h * 0.85f),
            startAngleDegrees = 90f, sweepAngleDegrees = 90f, forceMoveTo = false
        )
        arcTo(
            rect = Rect(w2, h * 0.4f, w * 0.4f, h * 0.6f),
            startAngleDegrees = 180f, sweepAngleDegrees = 90f, forceMoveTo = false
        )
    }

    onDrawWithContent {

        drawPath(path = envelope, color = color, style = Stroke(width = 4f))
        drawPath(path = rect, color = color)

        translate(left = w * 0.11f, top = h * 0.13f) {
            this@onDrawWithContent.drawContent()
        }
    }
}.padding(5.dp)

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "UnrememberedMutableState")
@PreviewLightDark
@Composable
fun DownloadPageItemsPreview(){
    JetTheme {
        Scaffold {
            LazyColumn {
                item {  NotifBox(onDismiss = { }) {
                }}
                item {
                    DownloadItem(selectedItemCount = mutableIntStateOf(2), download = Download(
                        url = "", fileName = "Esnglish-textagsgsghsghsdgfhjhgddhgfhgd.pdf", totalSize = 1200L,
                        status = mutableStateOf(Status.Success.apply { text = "Some error occured" })
                    ).apply {
                        currentSize = 120
                        filePath = ""
                        dateTime = LocalDateTime.now()
                    }
                    ) {}
                    DownloadItem(selectedItemCount = mutableIntStateOf(0), download = Download(
                        url = "", fileName = "Esnglish-.pdf", totalSize = 1200L,
                        status = mutableStateOf(Status.Error.apply { text = "Some error occured" })
                    ).apply {
                        currentSize = 120
                        filePath = ""
                        dateTime = LocalDateTime.now()
                    }
                    ) {}
                }
            }
        }
    }
}