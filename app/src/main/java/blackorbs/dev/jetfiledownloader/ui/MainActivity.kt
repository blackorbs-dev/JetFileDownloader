package blackorbs.dev.jetfiledownloader.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import blackorbs.dev.jetfiledownloader.R
import blackorbs.dev.jetfiledownloader.entities.Download
import blackorbs.dev.jetfiledownloader.services.Notifier
import blackorbs.dev.jetfiledownloader.ui.theme.JetTheme
import timber.log.Timber

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        setContent {
            JetTheme { MainScreen() }
        }
    }
}
class JetFileProvider : FileProvider(R.xml.file_paths)


// Main Extension Functions
internal fun Context.startActivityWithChooser(
    intent: Intent, @StringRes titleResId: Int
){
    try {
        startActivity(intent)
    }
    catch (e: ActivityNotFoundException){
        startActivity(
            Intent.createChooser(intent, getString(titleResId))
        )
    }
}

internal fun Context.shareFiles(filepathList: List<String>){
    val files = filepathList.mapNotNull {
        Notifier.getFileUri(this, it)
    }
    if(files.size == 1){
        shareFile(files[0]); return
    }
    if(files.isNotEmpty()){
        with(Intent(Intent.ACTION_SEND_MULTIPLE)){
            putParcelableArrayListExtra(
                Intent.EXTRA_STREAM,
                ArrayList(files)
            )
            putExtra(Intent.EXTRA_SUBJECT,
                getString(R.string.file_share_tag)
            )
            setType("*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivityWithChooser(this, R.string.file_share_tag)
        }
    }
}

internal fun Context.shareFile(uri: Uri){
    with(Intent(Intent.ACTION_SEND)){
        putExtra(Intent.EXTRA_STREAM, uri)
        setType(contentResolver.getType(uri))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivityWithChooser(this, R.string.file_share_tag)
    }
}

internal fun MutableList<Download>.addIfAbsent(download: Download) {
    if(download !in this) add(download)
}

internal fun List<Download>.getById(downloadId: Long): Download? {
    val index = indexOfFirst { it.id == downloadId }
    if(index != -1) return get(index)
    Timber.e("Download not in list")
    return null
}
