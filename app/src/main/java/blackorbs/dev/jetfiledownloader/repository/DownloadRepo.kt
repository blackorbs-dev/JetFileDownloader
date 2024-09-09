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

class DownloadRepo(private val app: Application?) : BaseDownloadRepo {
    private val dao = (app as MainApp).database.downloadDao()

    override fun getAll(): Flow<PagingData<Download>> =
        Pager(
            config = PagingConfig(
                pageSize = 20, enablePlaceholders = false
            ),
            pagingSourceFactory = { DownloadsPagingSource(app) }
        ).flow

    override suspend fun add(download: Download) = dao.add(download)

    override suspend fun update(size: Long, id: Long) = dao.update(size, id)

    override suspend fun update(status: MutableState<Status>, id: Long) = dao.update(status, id)

    override suspend fun delete(download: Download) = dao.delete(download)
}

interface BaseDownloadRepo {
    fun getAll(): Flow<PagingData<Download>>

    suspend fun add(download: Download): Long

    suspend fun update(size: Long, id: Long)

    suspend fun update(status: MutableState<Status>, id: Long)

    suspend fun delete(download: Download)
}