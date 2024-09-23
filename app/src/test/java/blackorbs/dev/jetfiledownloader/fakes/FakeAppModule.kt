package blackorbs.dev.jetfiledownloader.fakes

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import blackorbs.dev.jetfiledownloader.BaseAppModule
import blackorbs.dev.jetfiledownloader.data.DownloadDao
import blackorbs.dev.jetfiledownloader.data.FavoriteDao
import blackorbs.dev.jetfiledownloader.entities.Download

class FakeAppModule(
    override val favoriteDao: FavoriteDao =
        FakeFavoriteDao(),
    override val downloadDao: DownloadDao =
        FakeDownloadDao(),
    override var newDownloadsCount: MutableIntState =
        mutableIntStateOf(0),
    override val ongoingDownloads: MutableList<Download> =
        mutableListOf(),
    override val errorDownloads: MutableList<Download> =
        mutableListOf()
) : BaseAppModule