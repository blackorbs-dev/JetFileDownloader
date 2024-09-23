package blackorbs.dev.jetfiledownloader.fakes

import androidx.compose.runtime.MutableState
import blackorbs.dev.jetfiledownloader.data.DownloadDao
import blackorbs.dev.jetfiledownloader.entities.Download
import blackorbs.dev.jetfiledownloader.entities.Status

class FakeDownloadDao: DownloadDao {
    private val downloads = mutableListOf<Download>()

    override suspend fun add(download: Download): Long {
       downloads.indexOf(download).let {
           when(it){
               -1 -> downloads.add(download.apply {
                   id = downloads.size.toLong()
               })
               else -> downloads[it] = download
           }
       }
        return download.id
    }

    override suspend fun get(id: Long): Download =
        downloads[id.toInt()]

    override suspend fun update(size: Long, id: Long) =
        downloads[id.toInt()].run { currentSize = size }

    override suspend fun update(status: MutableState<Status>, id: Long) =
        downloads[id.toInt()].run { this.status = status }

    override suspend fun getAll(offsetIndex: Long, limit: Int): List<Download> =
        pagedList(offsetIndex, limit)

    override suspend fun delete(download: Download) {
        downloads.remove(download)
    }

    private fun pagedList(offset: Long, limit: Int): List<Download> {
        val endIndex = offset.toInt()+limit
        return if (downloads.isEmpty() || offset >= downloads.size) emptyList()
        else if(endIndex > downloads.size) downloads.subList(offset.toInt(), downloads.size)
        else downloads.subList(offset.toInt(), endIndex)
    }
}