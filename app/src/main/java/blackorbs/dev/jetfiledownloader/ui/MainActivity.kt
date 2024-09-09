package blackorbs.dev.jetfiledownloader.ui

import android.content.ServiceConnection
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import blackorbs.dev.jetfiledownloader.services.DownloadService
import blackorbs.dev.jetfiledownloader.ui.theme.JetTheme
import timber.log.Timber

class MainActivity : ComponentActivity() {

    private val mainServiceHolder = MainServiceHolder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JetTheme {
                MainScreen(
                    mainServiceHolder
                )
            }
        }
    }

    override fun onDestroy() {
        mainServiceHolder.downloadService?.let {
            Timber.d("Disconnecting from service...")
            mainServiceHolder.connection?.let { conn -> unbindService(conn) }
        }
        super.onDestroy()
    }
}

class MainServiceHolder{
    var downloadService: DownloadService? = null
    var connection: ServiceConnection? = null
}