package blackorbs.dev.jetfiledownloader.util

import androidx.compose.runtime.mutableStateOf
import blackorbs.dev.jetfiledownloader.entities.Download
import blackorbs.dev.jetfiledownloader.entities.Status
import okhttp3.mockwebserver.Dispatcher
import okio.Buffer
import okio.buffer
import okio.source
import java.time.LocalDateTime

object TestUtil {
    private var id = 0L

    fun testDownload(status: Status = Status.Queued) = Download(
        "google.com", "test${id++}.pdf", 1200,
        id = id, status = mutableStateOf(status)
    ).apply {
        dateTime = LocalDateTime.now()
        filePath = "downloads/test${this.id}.pdf"
    }

    fun Dispatcher.getResourceData(fileName: String): Buffer{
        javaClass.classLoader?.let {
            return it.getResourceAsStream(fileName)
                .source().buffer().buffer
        }
        return Buffer()
    }
}