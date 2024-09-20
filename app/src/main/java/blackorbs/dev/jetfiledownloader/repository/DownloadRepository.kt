package blackorbs.dev.jetfiledownloader.repository

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import blackorbs.dev.jetfiledownloader.MainApp
import blackorbs.dev.jetfiledownloader.entities.Download
import blackorbs.dev.jetfiledownloader.entities.Status
import kotlinx.coroutines.flow.Flow

class DownloadRepository(private val app: Application?) : BaseDownloadRepository {
    private val dao = (app as MainApp).downloadDao

    override fun getAll() = Pager(
        config = PagingConfig(pageSize = 20),
        pagingSourceFactory = { DownloadsPagingSource(app) }
    ).flow

    override suspend fun add(download: Download) = dao.add(download)

    override suspend fun get(id: Long): Download = dao.get(id)

    override suspend fun update(size: Long, id: Long) = dao.update(size, id)

    override suspend fun update(status: MutableState<Status>, id: Long) = dao.update(status, id)

    override suspend fun delete(download: Download) = dao.delete(download)
}

interface BaseDownloadRepository {
    fun getAll(): Flow<PagingData<Download>>

    suspend fun add(download: Download): Long

    suspend fun get(id: Long): Download

    suspend fun update(size: Long, id: Long)

    suspend fun update(status: MutableState<Status>, id: Long)

    suspend fun delete(download: Download)
}