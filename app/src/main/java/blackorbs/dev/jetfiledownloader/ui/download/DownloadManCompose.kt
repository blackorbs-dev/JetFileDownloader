package blackorbs.dev.jetfiledownloader.ui.download

import android.content.Context
import android.os.Environment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import blackorbs.dev.jetfiledownloader.R
import blackorbs.dev.jetfiledownloader.services.DownloadManager
import blackorbs.dev.jetfiledownloader.ui.MainServiceHolder
import kotlinx.coroutines.CoroutineScope
import java.io.File

@Composable
fun rememberDownloadManager(
    context: Context = LocalContext.current,
    scope: CoroutineScope = rememberCoroutineScope(),
    downloadFolder: File = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "JetDownloaderFiles"
    ),
    mainServiceHolder: MainServiceHolder,
    downloadVm: DownloadVm = viewModel<DownloadVm>(factory = DownloadVm.Factory),
    onMessage: (Int) -> Unit
): DownloadManager {

    val showDialog = remember { mutableStateOf(false) }

    val manager = remember { DownloadManager(
        context, scope, downloadFolder,
        mainServiceHolder, downloadVm,
        showDialog, onMessage
    ) }

    if(showDialog.value){
        RenameDialog(
            fileName = manager.fileName,
            isFailedDownload = manager.isFailedDownload,
            onDismissRequest = {showDialog.value = false},
            onDialogAction = manager::handleFileRename
        )
    }

    return manager
}

@Composable
fun RenameDialog(
    fileName: String,
    isFailedDownload: Boolean,
    onDismissRequest: () -> Unit,
    onDialogAction: (String) -> Unit
){
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.End) {
                Text(
                    stringResource(R.string.file_exist_title),
                    Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = TextUnit(20f, TextUnitType.Sp)
                )

                var text by remember { mutableStateOf(fileName) }
                val focusManager = LocalFocusManager.current

                TextField(
                    value = text, onValueChange = { text = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                        .padding(top = 5.dp, bottom = 10.dp),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    )
                )
                if(isFailedDownload){
                    TextButton(onClick = { onDialogAction("") }) {
                        Text(stringResource(R.string.resume_download))
                    }
                }
                TextButton(
                    enabled = text.isNotBlank(),
                    onClick = { focusManager.clearFocus(); onDialogAction(text) },
                ) {
                    Text(stringResource(R.string.rename_download))
                }
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.dismiss))
                }
            }
        }
    }
}

