package blackorbs.dev.jetfiledownloader.services

import androidx.paging.PagingConfig
import androidx.paging.PagingSource.LoadResult
import androidx.paging.testing.TestPager
import androidx.test.filters.SmallTest
import blackorbs.dev.jetfiledownloader.fakes.FakeAppModule
import blackorbs.dev.jetfiledownloader.fakes.FakeDownloadDao
import blackorbs.dev.jetfiledownloader.repository.DownloadsPagingSource
import blackorbs.dev.jetfiledownloader.util.TestUtil.testDownload
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@SmallTest
class DownloadPagingSourceTest {
    private lateinit var fakeDownloadDao: FakeDownloadDao
    private lateinit var downloadPagingSource: DownloadsPagingSource
    private val pagingConfig = PagingConfig(
        20, enablePlaceholders = false
    )
    private val downloads = (1..50).map { testDownload() }

    @Before
    fun setup(){
        fakeDownloadDao = FakeDownloadDao()
        downloadPagingSource = DownloadsPagingSource(
            FakeAppModule(downloadDao = fakeDownloadDao)
        )
    }

    @Test
    fun `get paged download test`() = runTest{
        assertTrue(
            (TestPager(pagingConfig, downloadPagingSource).refresh()
                    as LoadResult.Page).data.isEmpty()
        )
        downloads.forEach{fakeDownloadDao.add(it)}
        val pager = TestPager(pagingConfig, downloadPagingSource)
        assertEquals(
            downloads.subList(0, 20),
            (pager.refresh() as LoadResult.Page).data
        )
        assertEquals(
            downloads.subList(20, 40),
            (pager.append() as LoadResult.Page).data
        )
        assertEquals(
            downloads.subList(40, 50),
            (pager.append() as LoadResult.Page).data
        )
    }
}