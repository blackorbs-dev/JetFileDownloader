package blackorbs.dev.jetfiledownloader.util

import androidx.compose.runtime.mutableStateOf
import blackorbs.dev.jetfiledownloader.entities.Download
import blackorbs.dev.jetfiledownloader.entities.Status
import java.time.LocalDateTime

object TestUtil {
    private var id = 1L

    fun testDownload(status: Status = Status.Queued) = Download(
        "google.com", "test.pdf", 1200,
        id = id++, status = mutableStateOf(status)
    )
        .apply {
            dateTime = LocalDateTime.now()
            filePath = "downloads/test.pdf"
        }
}