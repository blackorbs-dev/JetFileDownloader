package blackorbs.dev.jetfiledownloader.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.filters.SmallTest
import blackorbs.dev.jetfiledownloader.util.MainCoroutineRule
import org.junit.Rule

@SmallTest
class DownloadViewModelTest {
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

}