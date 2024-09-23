package blackorbs.dev.jetfiledownloader.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import blackorbs.dev.jetfiledownloader.BaseAppModule
import blackorbs.dev.jetfiledownloader.MainApp
import blackorbs.dev.jetfiledownloader.R
import blackorbs.dev.jetfiledownloader.entities.ActionType
import blackorbs.dev.jetfiledownloader.entities.Download
import blackorbs.dev.jetfiledownloader.entities.Status
import blackorbs.dev.jetfiledownloader.entities.formatAsFileSize
import blackorbs.dev.jetfiledownloader.services.Notifier.Companion.SUMMARY_NOTIF_ID
import blackorbs.dev.jetfiledownloader.ui.addIfAbsent
import blackorbs.dev.jetfiledownloader.ui.getById
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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
    private lateinit var appModule: BaseAppModule
    private val scope = CoroutineScope(
        Dispatchers.IO + SupervisorJob()
    )

    private lateinit var downloadClient: OkHttpClient
    lateinit var notifier: Notifier

    private val stoppedDownloads = mutableListOf<Download>()

    override fun onCreate() {
        super.onCreate()
        appModule = (application as MainApp).appModule

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

        if(intent != null && intent.action != null) {
            scope.launch { handleNotifAction(intent) }
        }
        return START_STICKY
    }

    fun executeTask(download: Download){
        if(download.actionType == ActionType.Pause){
            updateStatus(download.apply { status.value = Status.Paused })
            return
        }
        updateStatus(download.apply { status.value = Status.Queued })
        val requestBuilder = Request.Builder()
        val file = File(download.filePath)
        if(file.length() > 0){
            requestBuilder.addHeader(
                "Range", "bytes=${file.length()}-"
            )
        }
        else notifier.showToast(getString(R.string.download_started, download.fileName))
        downloadClient.newCall(
            requestBuilder.url(download.url).tag(download.fileName).build()
        ).enqueue(object : Callback{
            override fun onResponse(call: Call, response: Response) {
                if(response.isSuccessful && response.body != null){
                    updateStatus(download.apply { status.value = Status.Ongoing })
                    if(download.totalSize <= 0L){
                        download.totalSize =
                            download.currentSize+response.body!!.contentLength()
                    }
                    val notifBuilder: NotificationCompat.Builder = notifier.getNotificationBuilder(download)
                    var sink: BufferedSink? = null; var source: BufferedSource? = null
                    try {
                        source = response.body!!.source()
                        sink = (if(file.exists() && response.body!!.contentLength() < download.totalSize)
                                file.appendingSink() else file.sink()).buffer()
                        val bufferSize: Long = 8*1024
                        var bytesRead: Long
                        var updateTime = 0L

                        while(source.read(sink.buffer, bufferSize)
                                .also { bytesRead = it } != -1L
                        ) {
                            sink.emit(); download.currentSize += bytesRead
                            if(System.currentTimeMillis() - updateTime > 1000){
                                updateSize(download.apply { publishProgress() }, notifBuilder)
                                updateTime = System.currentTimeMillis()
                            }
                        }
                        updateStatus(download.apply { status.value = Status.Success })
                        Timber.i("${download.fileName} successfully downloaded and saved to storage.")
                    }
                    catch (e: IOException){
                        handleError(
                            download, e.localizedMessage?:e.message?:e.toString()
                        )
                    }
                    finally {
                        sink?.flush(); sink?.close(); source?.close()
                    }
                }
                else{
                    Timber.e("Body: ${response.body}\nCode: ${response.code}")
                    handleError(
                        download, getString(R.string.error_message)
                    )
                }
            }

            override fun onFailure(call: Call, e: IOException) =
                handleError(
                    download, e.localizedMessage?:e.message?:e.toString()
                )
        })
    }

    private fun updateSize(download: Download, notifBuilder: NotificationCompat.Builder){
        scope.launch {
            appModule.downloadDao.update(download.currentSize, download.id)
        }
        with(notifBuilder){
            clearActions()
            addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.pause),
                notifier.actionIntent(R.string.pause, download.id)
            )
            setContentText(getStatusText(download))
            setProgress(100, download.sizePercent.intValue, false)
            notifier.showNotification(download, this)
        }
    }

    private fun updateStatus(download: Download){
        val notifBuilder = notifier.getNotificationBuilder(download)
        if(download.isNotCompleted){
            if(download.isPending){ // Queued/Ongoing
                if(download.status.value == Status.Queued){
                    appModule.ongoingDownloads.addIfAbsent(download)
                    appModule.errorDownloads.remove(download)
                    stoppedDownloads.remove(download)
                }
                notifBuilder.addAction(
                    android.R.drawable.ic_media_pause,
                    getString(R.string.pause),
                    notifier.actionIntent(R.string.pause, download.id)
                )
            }
            else{ // Paused/Error
                if(download.status.value == Status.Paused)
                    cancel(download.fileName)
                else appModule.errorDownloads.addIfAbsent(download)
                stoppedDownloads.addIfAbsent(download)
                appModule.ongoingDownloads.remove(download)
                notifBuilder.addAction(
                    android.R.drawable.ic_media_play,
                    getString(R.string.resume),
                    notifier.actionIntent(R.string.resume, download.id)
                )
            }
            notifBuilder.setProgress(100, download.sizePercent.intValue, false)
        }
        else { // Success
            appModule.ongoingDownloads.remove(download)
            notifBuilder.apply {
                setContentIntent(notifier.getPendingFileIntent(download.filePath))
                setProgress(0, 0, false)
                setAutoCancel(true)
            }
        }
        scope.launch {
            appModule.downloadDao.update(download.status, download.id)
        }
        notifier.showUpdate(download.fileName, download.status.value)
        notifBuilder.setContentText(getStatusText(download))
        notifier.showNotification(download, notifBuilder)
    }

    private fun getStatusText(download: Download) =
        "${getString(download.status.value.titleResID)} (${
            if(download.status.value == Status.Success) getString(R.string.tap_to_open)
            else "${download.currentSize.formatAsFileSize(this)}/" +
                    download.totalSize.formatAsFileSize(this)
        })"

    private fun handleError(
        download: Download, errorMsg: String
    ) {
        if(download.status.value == Status.Paused || download.status.value == Status.Success
            || download.status.value == Status.Error
        ) return
        updateStatus(download.apply { status.value = Status.Error.apply { text = errorMsg }})
        Timber.e("${download.fileName} Download Error:: $errorMsg")
    }

    private fun cancel(fileName: String) {
        for(call in downloadClient.dispatcher.queuedCalls()) {
            if(fileName == call.request().tag()) call.cancel()
        }
        for(call in downloadClient.dispatcher.runningCalls()) {
            if(fileName == call.request().tag()) call.cancel()
        }
    }

    private fun handleNotifAction(intent: Intent) {
        when(intent.action){
            getString(R.string.stop_service) -> {
                if(appModule.ongoingDownloads.isEmpty()){
                    serviceDisconnector?.invoke()
                    stopSelf()
                }
                else{
                    notifier.showToast(getString(R.string.stop_ongoing_task))
                }
            }
            getString(R.string.stop_ongoing_task) -> appModule.ongoingDownloads.forEach {
                executeTask(it.apply { actionType = ActionType.Pause })
            }
            getString(R.string.pause) -> appModule.ongoingDownloads.getById(
                intent.getLongExtra(getString(R.string.pause), -1)
            )?.let {
                executeTask(it.apply { actionType = ActionType.Pause })
            }
            getString(R.string.resume) -> stoppedDownloads.getById(
                intent.getLongExtra(getString(R.string.resume), -1)
            )?.let {
                executeTask(it.apply { actionType = ActionType.Resume })
            }
            else -> {}
        }
    }

    var serviceDisconnector: (() -> Unit)? = null

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
        scope.cancel()
        notifier.handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}