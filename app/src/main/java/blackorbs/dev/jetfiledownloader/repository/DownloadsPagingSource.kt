package blackorbs.dev.jetfiledownloader.repository

import android.app.Application
import androidx.paging.PagingSource
import androidx.paging.PagingState
import blackorbs.dev.jetfiledownloader.MainApp
import blackorbs.dev.jetfiledownloader.entities.Download
import blackorbs.dev.jetfiledownloader.entities.Status
import java.io.File

class DownloadsPagingSource(app: Application?): PagingSource<Int, Download>() {
    private val limit = 20
    private val mainApp = app as MainApp

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Download> {
        val page = params.key ?: 0
        val downloads = mainApp.downloadDao.getAll(
            page.toLong()*limit+mainApp.newDownloadsCount.intValue,
            limit = limit
        ).map { download ->
            if(download.status.value == Status.Error){
                mainApp.errorDownloads.add(download)
            }
            mainApp.ongoingDownloads.indexOfFirst {
                item -> item.id == download.id
            }.run {
                if(this != -1){
                    return@map mainApp.ongoingDownloads[this]
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