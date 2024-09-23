package blackorbs.dev.jetfiledownloader.app

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import blackorbs.dev.jetfiledownloader.BaseAppModule
import blackorbs.dev.jetfiledownloader.FakeApp
import blackorbs.dev.jetfiledownloader.R
import blackorbs.dev.jetfiledownloader.robots.Robot
import blackorbs.dev.jetfiledownloader.ui.MainScreen
import blackorbs.dev.jetfiledownloader.ui.theme.JetTheme
import blackorbs.dev.jetfiledownloader.util.TestUtil.testDownload
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class AppTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var robot: Robot
    private val mockWebServer = MockWebServer()
    private val downloads = (1..5).map { testDownload() }

    @Before
    fun setup(){
        mockWebServer.start()
    }

    @Test
    fun loadMainScreenAndNavigateThroughWithStateSave(){
        composeRule.setContent {
            robot = Robot(LocalContext.current, composeRule)
            JetTheme {
                MainScreen()
            }
        }
        composeRule.onRoot(useUnmergedTree = true)
            .printToLog("ComposeTester")
        with(robot) {
            assertTextDisplayed(R.string.enter_url)
            enterText(R.string.enter_url,
                text = "download pdf"
            )
            assertTextNotDisplayed(R.string.enter_url)
            assertTextDisplayed(text = "download pdf")
            clickIcon(R.string.clear_input)
            assertTextDisplayed(R.string.enter_url)
            enterText(R.string.enter_url,
                text = "download apk")
            assertTextDisplayed(text = "download apk")

            clickIcon(R.string.show_menu)
            assertIconReplaced(R.string.show_menu, R.string.hide_menu)
            clickTextWithIcon(R.string.download)
            assertTextDisplayed(
                R.string.empty_list_info,
                R.string.downloaded_files
            )
            assertIconReplaced(R.string.hide_menu, R.string.show_menu)

            clickIcon(R.string.show_menu)
            clickTextWithIcon(R.string.favorite)
            assertTextDisplayed(
                R.string.empty_list_info,
                R.string.favorite_websites
            )

            Espresso.pressBack()
            assertTextDisplayed(text = "download apk")
            assertIconNotDisplayed(R.string.clear_input)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun navigateToAndFroDownloadDetailsPage() = runTest{
        var appModule: BaseAppModule? = null
        composeRule.setContent {
            robot = Robot(
                LocalContext.current.applicationContext,
                composeRule)
            appModule = (LocalContext.current.applicationContext as FakeApp).appModule
            JetTheme {
                MainScreen()
            }
        }
        downloads.forEach {
            appModule?.downloadDao?.add(it)
        }
        with(robot){
            clickIcon(R.string.show_menu)
            clickTextWithIcon(R.string.download)
            assertTextNotDisplayed(
                R.string.empty_list_info,
                R.string.downloaded_files
            )
            assertTextDisplayed(R.string.today)
            longClickText(text = downloads[0].fileName)
            clickIcon(R.string.info)
            assertTextNotDisplayed(R.string.today)
            assertTextDisplayed(text = downloads[0].url)
            assertTextDisplayed(text = downloads[0].filePath)
            assertIconDisplayed(R.string.share)

            clickIcon(R.string.go_back)
            assertTextDisplayed(R.string.today)
            assertTextNotDisplayed(text = downloads[0].filePath)
            assertIconDisplayed(R.string.delete)
            clickText(text = downloads[1].fileName)
            assertIconNotDisplayed(R.string.delete)
            assertIconsCount(R.string.selected, num = 2)
            assertIconDisplayed(R.string.share_all)
            assertIconDisplayed(R.string.delete_all)

            clickText(text = downloads[0].fileName)
            clickIcon(R.string.info)
            assertTextNotDisplayed(R.string.today)
            assertTextDisplayed(text = downloads[1].fileName)
            assertTextDisplayed(text = downloads[1].url)

            clickIcon(R.string.go_back)
            clickIcon(R.string.delete)
            composeRule.waitUntilDoesNotExist(
                hasText(downloads[1].fileName), 4000
            )
            assertTextNotDisplayed(text = downloads[1].fileName)
            assertIconNotDisplayed(R.string.delete)
            assertIconNotDisplayed(R.string.info)
        }
    }

    @After
    fun cleanup(){
        mockWebServer.shutdown()
    }
}