package blackorbs.dev.jetfiledownloader.repository

import android.app.Application
import androidx.paging.PagingSource
import androidx.paging.PagingState
import blackorbs.dev.jetfiledownloader.MainApp
import blackorbs.dev.jetfiledownloader.entities.Download
import blackorbs.dev.jetfiledownloader.entities.Status

class DownloadsPagingSource(app: Application?): PagingSource<Int, Download>() {
    private val limit = 20
    private val mainApp = app as MainApp
    private val dao = mainApp.database.downloadDao()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Download> {
        val page = params.key ?: 0
        val downloads = dao.getAll(
            page.toLong()*limit+mainApp.addNum,
            limit = limit
        ).map { download ->
//            Timber.e(
//                "Ongoing download not found!. " +
//                        "Possible reason: Download Service is not " +
//                        "yet connected on app start"
//            )
//            return LoadResult.Error(ServiceConnException())
            if(download.status.value == Status.Error){
                mainApp.errorDownloads.add(download)
            }
            val index = mainApp.ongoingDownloads.indexOfFirst {
                item -> item.id == download.id
            }
            if(index != -1){
                return@map mainApp.ongoingDownloads[index]
            }
            download
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

class ServiceConnException: Exception()