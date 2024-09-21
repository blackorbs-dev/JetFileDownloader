package blackorbs.dev.jetfiledownloader.data

import androidx.compose.runtime.mutableStateOf
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import blackorbs.dev.jetfiledownloader.entities.Status
import blackorbs.dev.jetfiledownloader.util.TestUtil.testDownload
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@SmallTest
@RunWith(RobolectricTestRunner::class)
class DownloadDaoTest {

    private lateinit var database: Database
    private lateinit var downloadDao: DownloadDao

    private val download = testDownload()

    @Before
    fun setup(){
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            Database::class.java
        ).allowMainThreadQueries().build()

        downloadDao = database.downloadDao()
    }

    @Test
    fun `add and get download item to database`() = runTest{
        assertEquals(true, downloadDao.getAll(0).isEmpty())
        val id = downloadDao.add(download)
        assertEquals(1, downloadDao.getAll(0).size)
        val d = downloadDao.get(id)
        assertEquals(download.url, d.url)
        assertEquals(download.fileName, d.fileName)
        assertEquals(download.filePath, d.filePath)
        downloadDao.add(d.apply {
            url = "kefblog.com"
        })
        assertEquals(1, downloadDao.getAll(0).size)
        assertEquals("kefblog.com", downloadDao.get(id).url)
    }

    @Test
    fun `update download size and status and delete success`() = runTest {
        assertEquals(true, downloadDao.getAll(0).isEmpty())
        val id = downloadDao.add(download)
        downloadDao.update(mutableStateOf(Status.Success), id)
        downloadDao.update(12000, id)
        assertEquals(1, downloadDao.getAll(0).size)
        assertEquals(Status.Success, downloadDao.get(id).status.value)
        assertEquals(12000, downloadDao.get(id).currentSize)

        downloadDao.delete(downloadDao.get(id))
        assertEquals(true, downloadDao.getAll(0).isEmpty())
    }

    @After
    fun cleanUp(){
        database.close()
    }
}