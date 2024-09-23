package blackorbs.dev.jetfiledownloader.services

import androidx.compose.runtime.mutableStateOf
import androidx.test.filters.SmallTest
import blackorbs.dev.jetfiledownloader.entities.Status
import blackorbs.dev.jetfiledownloader.fakes.FakeAppModule
import blackorbs.dev.jetfiledownloader.fakes.FakeDownloadDao
import blackorbs.dev.jetfiledownloader.repository.DownloadRepository
import blackorbs.dev.jetfiledownloader.util.TestUtil.testDownload
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@SmallTest
class DownloadRepositoryTest {
    private lateinit var downloadRepos: DownloadRepository
    private lateinit var fakeDownloadDao: FakeDownloadDao
    private val download = testDownload()

    @Before
    fun setup(){
        fakeDownloadDao = FakeDownloadDao()
        downloadRepos = DownloadRepository(
            FakeAppModule(
                downloadDao = fakeDownloadDao
            )
        )
    }

    @Test
    fun `add, update and get download item repository`() = runTest{
        assertTrue(
            fakeDownloadDao.getAll(0).isEmpty()
        )
        download.id = downloadRepos.add(download)
        assertTrue(
            fakeDownloadDao.getAll(0).isNotEmpty()
        )
        assertEquals(
            download, downloadRepos.get(download.id)
        )
        downloadRepos.update(12000, download.id)
        downloadRepos.update(mutableStateOf(Status.Success), download.id)
        assertEquals(Status.Success, downloadRepos.get(download.id).status.value)
        assertEquals(12000, downloadRepos.get(download.id).currentSize)
        downloadRepos.delete(download)
        assertTrue(
            fakeDownloadDao.getAll(0).isEmpty()
        )
    }
}