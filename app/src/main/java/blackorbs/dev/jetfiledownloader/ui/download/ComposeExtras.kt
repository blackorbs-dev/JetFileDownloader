package blackorbs.dev.jetfiledownloader.ui.download

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import blackorbs.dev.jetfiledownloader.R
import blackorbs.dev.jetfiledownloader.services.DownloadManager
import blackorbs.dev.jetfiledownloader.services.rememberPermissionManager
import blackorbs.dev.jetfiledownloader.ui.theme.JetTheme
import kotlinx.coroutines.CoroutineScope
import java.io.File

@Composable
fun rememberDownloadManager(
    context: Context = LocalContext.current,
    scope: CoroutineScope = rememberCoroutineScope(),
    downloadFolder: File = File(
        Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        ), "JetDownloaderFiles"
    ),
    downloadVm: DownloadVM = viewModel(factory = DownloadVM.Factory),
    onMessage: (String) -> Unit
): DownloadManager {

    val showDialog = rememberSaveable { mutableStateOf(false) }
    val showError = rememberSaveable { mutableStateOf(false) }

    val manager = remember { DownloadManager(
        context, scope, downloadFolder,
        downloadVm, showDialog, showError
    ) }

    ComposeLifecycle {_, event ->
        when(event){
            Lifecycle.Event.ON_CREATE, Lifecycle.Event.ON_START,
            Lifecycle.Event.ON_RESUME, Lifecycle.Event.ON_PAUSE,
            Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_ANY -> {}
            Lifecycle.Event.ON_DESTROY -> manager.disconnectService()
        }
    }

    if(showDialog.value){
        PopupDialog(
            titleRes = R.string.file_exist_title,
            onDismiss = { manager.handleFileRename("") }
        ) {
            RenameLayout(
                fileName = manager.fileName,
                isError = showError.value,
                isFailedDownload = manager.isFailedDownload,
                onDialogAction = manager::handleFileRename
            )
        }
    }
    else showError.value = false

    return manager.apply {
        permissionManager = rememberPermissionManager(
            context = context, onMessage = onMessage,
            executePendingDownloads = this::executePending,
            onNotifPermission = this::onNotifPermission
        )
    }
}

@Composable
internal fun RenameLayout(
    fileName: String,
    isError: Boolean,
    isFailedDownload: Boolean,
    onDialogAction: (String) -> Unit
){
    val text = remember { mutableStateOf(fileName) }
    val focusManager = LocalFocusManager.current

    OutlineEditBox(
        text = text, isError = isError,
        labelResId = R.string.new_filename_label,
        errorResId = R.string.file_still_exist
    )

    if(isFailedDownload){
        TextButton(
            onClick = { onDialogAction("") },
            Modifier.padding(end = 10.dp)
        ) {
            Text(stringResource(R.string.resume_download))
        }
    }
    TextButton(
        enabled = text.value.isNotBlank(),
        modifier = Modifier.padding(end = 10.dp),
        onClick = {
            focusManager.clearFocus(); onDialogAction(text.value) },
    ) {
        Text(stringResource(R.string.continue_download))
    }
    TextButton(
        onClick = {onDialogAction("")},
        Modifier.padding(end = 10.dp, bottom = 10.dp)
    ) {
        Text(stringResource(R.string.cancel))
    }
}

@Composable
internal fun PopupDialog(
    @StringRes titleRes: Int,
    onDismiss: () -> Unit,
    horizontalAlignment: Alignment.Horizontal = Alignment.End,
    content: @Composable () -> Unit
){
    Dialog(onDismissRequest = onDismiss) {
        Card(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(horizontalAlignment = horizontalAlignment) {
                Text(
                    stringResource(titleRes),
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(16.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.headlineSmall
                )
                content()
            }
        }
    }
}

@Composable
internal fun OutlineEditBox(
    text: MutableState<String>,
    @StringRes labelResId: Int,
    isError: Boolean = false,
    maxLines: Int = 1,
    @StringRes errorResId: Int = R.string.error,
){
    OutlinedTextField(
        value = text.value, onValueChange = { text.value = it },
        singleLine = maxLines == 1,
        isError = isError, maxLines = maxLines,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 10.dp, end = 10.dp),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done
        ),
        label = {Text(stringResource(labelResId))}
    )
    if(isError){
        Text(
            stringResource(errorResId),
            Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun ComposeLifecycle(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onEvent: (LifecycleOwner, Lifecycle.Event) -> Unit
){
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver(onEvent)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@PreviewLightDark
@Composable
fun RenameDialogPreview(){
    JetTheme {
        Scaffold {
            PopupDialog(
                R.string.file_exist_title,
                onDismiss = { /*TODO*/ }
            ) {
                RenameLayout(
                    fileName = "testssagfagdsgashdgfyfgda.pdf",
                    isError = true, isFailedDownload = true) {
                }
            }
        }
    }
}

