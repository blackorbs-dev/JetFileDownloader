package blackorbs.dev.jetfiledownloader

import android.app.Application
import androidx.compose.runtime.mutableIntStateOf
import androidx.room.Room
import blackorbs.dev.jetfiledownloader.data.Database
import blackorbs.dev.jetfiledownloader.entities.Download

class MainApp: Application() {

    private val database by lazy {
        Room.databaseBuilder(
            this, klass = Database::class.java, "app_data"
        ).build()
    }

    val downloadDao by lazy {
        database.downloadDao()
    }

    val favoriteDao by lazy {
        database.favoriteDao()
    }

    var newDownloadsCount = mutableIntStateOf(0)
    val ongoingDownloads = mutableListOf<Download>()
    val errorDownloads = mutableListOf<Download>()

    override fun onCreate() {
        super.onCreate()
        TimberLogger.init()
    }
}