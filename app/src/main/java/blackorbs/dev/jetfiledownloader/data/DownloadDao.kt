package blackorbs.dev.jetfiledownloader.data

import androidx.compose.runtime.MutableState
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import blackorbs.dev.jetfiledownloader.entities.Download
import blackorbs.dev.jetfiledownloader.entities.Status

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(download: Download): Long

    @Query("UPDATE downloads SET currentSize = :size WHERE id = :id")
    suspend fun update(size: Long, id: Long)

    @Query("UPDATE downloads SET status = :status WHERE id = :id")
    suspend fun update(status: MutableState<Status>, id: Long)

    @Query("SELECT * FROM downloads ORDER BY datetime(dateTime) DESC LIMIT :limit OFFSET :offsetIndex")
    suspend fun getAll(offsetIndex: Long, limit: Int = 10): List<Download>
//    @Query("SELECT * FROM downloads ORDER BY datetime(dateTime) DESC")
//    fun getAll(): PagingSource<Int, Download>

    @Delete
    suspend fun delete(download: Download)
}