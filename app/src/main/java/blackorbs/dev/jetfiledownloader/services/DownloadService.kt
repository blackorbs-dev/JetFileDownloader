package blackorbs.dev.jetfiledownloader.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import blackorbs.dev.jetfiledownloader.R
import blackorbs.dev.jetfiledownloader.entities.ActionType
import blackorbs.dev.jetfiledownloader.entities.Download
import blackorbs.dev.jetfiledownloader.entities.Status
import blackorbs.dev.jetfiledownloader.services.Notifier.Companion.SUMMARY_NOTIF_ID
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.BufferedSink
import okio.BufferedSource
import okio.appendingSink
import okio.buffer
import okio.sink
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class DownloadService: Service() {
    private val binder = ServiceBinder()

    private lateinit var downloadClient: OkHttpClient
    lateinit var notifier: Notifier
    private val _ongoingDownloads = mutableListOf<Download>()
    val ongoingDownloads: List<Download> = _ongoingDownloads

    override fun onCreate() {
        super.onCreate()

        val dispatcher = Dispatcher().apply {
            maxRequests = 30
            maxRequestsPerHost = 3
        }
        downloadClient = OkHttpClient.Builder().dispatcher(dispatcher)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        notifier = Notifier(this)
        holder = ServiceHolder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForeground(this,
            SUMMARY_NOTIF_ID, notifier.summaryNotif, notifier.notifType
        )
        notifier.showNotification(SUMMARY_NOTIF_ID, notifier.summaryNotif)

        if(intent != null && intent.action != null &&
            intent.action == getString(R.string.stop_service)) {
            stopSelf()
        }
        return START_STICKY
    }

    fun executeTask(download: Download){
        if(download.actionType == ActionType.Pause){
            download.status.value = Status.Paused
            statusUpdater?.invoke(download)
            cancel(download.fileName)
            _ongoingDownloads.removeIf{item -> item.id == download.id}
            return
        }
        val notifBuilder: NotificationCompat.Builder = notifier.getNotificationBuilder(download)
        notifier.showNotification(download.id.toInt(), notifBuilder.build())
        val requestBuilder = Request.Builder()
        if(download.currentSize.longValue > 0){
            requestBuilder.addHeader(
                "Range", "bytes=${download.currentSize.longValue}-"
            )
            download.status.value = Status.Queued
            statusUpdater?.invoke(download)
        }
        _ongoingDownloads.add(download)
        downloadClient.newCall(
            requestBuilder.url(download.url).tag(download.fileName).build()
        ).enqueue(object : Callback{

            override fun onResponse(call: Call, response: Response) {
                if(response.isSuccessful && response.body != null){
                    download.status.value = Status.Ongoing
                    statusUpdater?.invoke(download)
                    var sink: BufferedSink? = null; var source: BufferedSource? = null
                    try {
                        source = response.body!!.source()
                        val file = File(download.filePath)
                        sink = (if(file.exists()) file.appendingSink() else file.sink())
                                .buffer()
                        val bufferSize: Long = 8*1024
                        var bytesRead: Long
                        var updateTime = 0L

                        while(source.read(sink.buffer, bufferSize)
                                .also { bytesRead = it } != -1L
                        ) {
                            sink.emit(); download.currentSize.longValue += bytesRead
                            if(System.currentTimeMillis() - updateTime > 1000){
                                notifBuilder.setProgress(100, download.sizePercent, false)
                                notifier.showNotification(download.id.toInt(), notifBuilder.build())
                                updateTime = System.currentTimeMillis()
                            }
                        }
                        download.status.value = Status.Success
                        statusUpdater?.invoke(download)
                        notifier.showUpdate(download.fileName, Status.Success)
                        _ongoingDownloads.removeIf{item -> item.id == download.id}
                        notifBuilder.setProgress(100, 100, false)
                        notifBuilder.setContentIntent(notifier.getPendingFileIntent(download.filePath))
                        notifier.showNotification(download.id.toInt(), notifBuilder.build())

                        Timber.i("${download.fileName} successfully downloaded and saved to storage.")
                    }
                    catch (e: IOException){
                        handleError(download, e.localizedMessage?:e.message?:e.toString())
                    }
                    finally {
                        sink?.flush(); sink?.close(); source?.close()
                        sizeUpdater?.invoke(download.currentSize.longValue, download.id)
                    }
                }
                else{
                    Timber.e("Body: ${response.body} ---- Code: ${response.code}")
                    handleError(download, getString(R.string.error_message))
                }
            }

            override fun onFailure(call: Call, e: IOException) =
                handleError(download, e.localizedMessage?:e.message?:e.toString())
        })
    }

    private fun handleError(download: Download, errorMsg: String) {
        if(download.status.value == Status.Paused || download.status.value == Status.Success
            || download.status.value == Status.Error
        ) return
        download.status.value = Status.Error.apply { text = errorMsg }
        statusUpdater?.invoke(download)
        notifier.showUpdate(download.fileName, download.status.value)
        _ongoingDownloads.removeIf{item -> item.id == download.id}
        Timber.e("${download.fileName} Download Error:: $errorMsg")
    }

    var statusUpdater: ((download: Download) -> Unit)? = null

    var sizeUpdater: ((size: Long, id: Long) -> Unit)? = null

    private fun cancel(fileName: String) {
        for(call in downloadClient.dispatcher.queuedCalls()) {
            if(fileName == call.request().tag()) call.cancel()
        }
        for(call in downloadClient.dispatcher.runningCalls()) {
            if(fileName == call.request().tag()) call.cancel()
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    inner class ServiceBinder: Binder(){
        val service get() = this@DownloadService
    }

    companion object{
        private var holder: ServiceHolder? = null
        val isRunning: Boolean get() = try {
            holder != null && holder!!.ping
        }
        catch (e: NullPointerException){ false }
    }

    inner class ServiceHolder{
        // to check if service is running on app
        // app start and connect
        val ping get() = true
    }

    override fun onDestroy() {
        holder = null
        notifier.handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}