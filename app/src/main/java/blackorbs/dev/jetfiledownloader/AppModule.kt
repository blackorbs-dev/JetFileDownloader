package blackorbs.dev.jetfiledownloader

import android.content.Context
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.room.Room
import blackorbs.dev.jetfiledownloader.data.Database
import blackorbs.dev.jetfiledownloader.data.DownloadDao
import blackorbs.dev.jetfiledownloader.data.FavoriteDao
import blackorbs.dev.jetfiledownloader.entities.Download

class AppModule(private val context: Context): BaseAppModule {

    private val database by lazy {
        Room.databaseBuilder(
            context, klass = Database::class.java, "app_data"
        ).build()
    }

    override val downloadDao by lazy {
        database.downloadDao()
    }

    override val favoriteDao by lazy {
        database.favoriteDao()
    }
    override val ongoingDownloads by lazy {
        mutableListOf<Download>()
    }

    override val errorDownloads by lazy {
        mutableListOf<Download>()
    }

    override var newDownloadsCount = mutableIntStateOf(0)
}

interface BaseAppModule{
    val favoriteDao: FavoriteDao
    val downloadDao: DownloadDao

    var newDownloadsCount: MutableIntState
    val ongoingDownloads: MutableList<Download>
    val errorDownloads: MutableList<Download>
}