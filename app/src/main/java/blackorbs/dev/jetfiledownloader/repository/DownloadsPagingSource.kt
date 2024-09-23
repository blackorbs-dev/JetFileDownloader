package blackorbs.dev.jetfiledownloader.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import blackorbs.dev.jetfiledownloader.BaseAppModule
import blackorbs.dev.jetfiledownloader.entities.Download
import blackorbs.dev.jetfiledownloader.entities.Status
import java.io.File

class DownloadsPagingSource(private val appModule: BaseAppModule): PagingSource<Int, Download>() {
    private val limit = 20

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Download> {
        val page = params.key ?: 0
        val downloads = appModule.downloadDao.getAll(
            page.toLong()*limit+appModule.newDownloadsCount.intValue,
            limit = limit
        ).map { download ->
            if(download.status.value == Status.Error){
                appModule.errorDownloads.add(download)
            }
            appModule.ongoingDownloads.indexOfFirst {
                item -> item.id == download.id
            }.run {
                if(this != -1){
                    return@map appModule.ongoingDownloads[this]
                }
            }
            when{
                download.currentSize > 0 && !File(download.filePath).exists() ->
                    download.status.value = Status.Deleted
                download.status.value == Status.Queued
                        || download.status.value == Status.Ongoing ->
                    download.status.value = Status.Paused
            }
            download.apply { publishProgress() }
        }

        return LoadResult.Page( downloads, null,
            if(downloads.isEmpty()) null else page+1
        )
    }

    override fun getRefreshKey(state: PagingState<Int, Download>): Int? =
        state.anchorPosition?.let { anchorPosition ->
             val anchorPage = state.closestPageToPosition(anchorPosition)
             anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
}