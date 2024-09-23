package blackorbs.dev.jetfiledownloader

import android.content.Context
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.room.Room
import blackorbs.dev.jetfiledownloader.data.Database
import blackorbs.dev.jetfiledownloader.data.DownloadDao
import blackorbs.dev.jetfiledownloader.data.FavoriteDao
import blackorbs.dev.jetfiledownloader.entities.Download
import timber.log.Timber

class FakeAppModule(context: Context) : BaseAppModule{

    init {
        Timber.d("Initializing FakeAppModule...")
    }

    private val database = Room.inMemoryDatabaseBuilder(
        context, Database::class.java
    ).allowMainThreadQueries().build()

    override val favoriteDao: FavoriteDao =
        database.favoriteDao()
    override val downloadDao: DownloadDao =
        database.downloadDao()
    override var newDownloadsCount: MutableIntState =
        mutableIntStateOf(0)
    override val ongoingDownloads: MutableList<Download> =
        mutableListOf()
    override val errorDownloads: MutableList<Download> =
        mutableListOf()
}