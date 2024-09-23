package blackorbs.dev.jetfiledownloader.repository

import androidx.compose.runtime.MutableState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import blackorbs.dev.jetfiledownloader.BaseAppModule
import blackorbs.dev.jetfiledownloader.entities.Download
import blackorbs.dev.jetfiledownloader.entities.Status
import kotlinx.coroutines.flow.Flow

class DownloadRepository(private val appModule: BaseAppModule) : BaseDownloadRepository {

    override fun getAll() = Pager(
        config = PagingConfig(pageSize = 20),
        pagingSourceFactory = { DownloadsPagingSource(appModule) }
    ).flow

    override suspend fun add(download: Download) = appModule.downloadDao.add(download)

    override suspend fun get(id: Long): Download = appModule.downloadDao.get(id)

    override suspend fun update(size: Long, id: Long) = appModule.downloadDao.update(size, id)

    override suspend fun update(status: MutableState<Status>, id: Long) = appModule.downloadDao.update(status, id)

    override suspend fun delete(download: Download) = appModule.downloadDao.delete(download)
}

interface BaseDownloadRepository {
    fun getAll(): Flow<PagingData<Download>>

    suspend fun add(download: Download): Long

    suspend fun get(id: Long): Download

    suspend fun update(size: Long, id: Long)

    suspend fun update(status: MutableState<Status>, id: Long)

    suspend fun delete(download: Download)
}