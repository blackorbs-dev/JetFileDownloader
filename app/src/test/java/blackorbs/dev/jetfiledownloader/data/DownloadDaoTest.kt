package blackorbs.dev.jetfiledownloader.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.paging.PagingConfig
import androidx.paging.PagingSource.LoadResult.Page
import androidx.paging.testing.TestPager
import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import blackorbs.dev.jetfiledownloader.entities.Download
import blackorbs.dev.jetfiledownloader.repository.DownloadRepo
import blackorbs.dev.jetfiledownloader.ui.download.DownloadVm
import blackorbs.dev.jetfiledownloader.util.TestUtil.testDownload
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime

@SmallTest
@RunWith(RobolectricTestRunner::class)
class DownloadDaoTest {
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: Database
    private lateinit var downloadDao: DownloadDao

    private lateinit var repo: DownloadRepo
    private lateinit var downloadVm: DownloadVm

    private val downloads = listOf(
        testDownload(), testDownload(), testDownload(),
        testDownload(), testDownload(), testDownload()
    )

    @Before
    fun setup(){
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            Database::class.java
        ).allowMainThreadQueries().build()

        downloadDao = database.downloadDao()

        repo = DownloadRepo(downloadDao)
        downloadVm = DownloadVm(repo)
    }

    @Test
    fun `add item to paged list test`() = runTest{
        downloads.forEach { downloadDao.add(it) }

        val pager = TestPager(
            config = PagingConfig(
                pageSize = 2,
                initialLoadSize = 2,
                enablePlaceholders = false
            ),
            pagingSource = downloadDao.getAll()
        )

        assertEquals(
            downloads.subList(0,2).map { it.id },
            (pager.refresh() as Page).data.map { it.id }
        )

        assertEquals(
            downloads.subList(2,4).map { it.id },
            (pager.append() as Page).data.map { it.id }
        )
    }

    @Test
    fun `get paged items test`() = runBlocking{
        downloadDao.add(
            Download("google.com", "test.pdf", 1200)
                .apply {
                    dateTime = LocalDateTime.now()
                    filePath = "downloads/test.pdf"
                }
        )
        val pager = TestPager(
            config = PagingConfig(10),
            pagingSource = downloadDao.getAll()
        )
        assertEquals(
            1,
            (pager.refresh() as Page).data.size
        )
        assertEquals(
            1,
            downloadVm.downloads.asSnapshot().size
        )
    }

    @After
    fun cleanUp(){
        database.close()
    }
}