package blackorbs.dev.jetfiledownloader.services

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.webkit.URLUtil
import androidx.compose.runtime.MutableState
import blackorbs.dev.jetfiledownloader.MainApp
import blackorbs.dev.jetfiledownloader.R
import blackorbs.dev.jetfiledownloader.entities.ActionType
import blackorbs.dev.jetfiledownloader.entities.Download
import blackorbs.dev.jetfiledownloader.entities.Status
import blackorbs.dev.jetfiledownloader.ui.MainServiceHolder
import blackorbs.dev.jetfiledownloader.ui.download.DownloadVm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.time.LocalDateTime

class DownloadManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val downloadFolder: File,
    private val mainServiceHolder: MainServiceHolder,
    private val downloadVm: DownloadVm,
    private val showDialog: MutableState<Boolean>,
    private val onMessage: (Int) -> Unit
) {
    val permissionManager = PermissionManager(
        context = context, onNotifPermission = this::onNotifPermission,
        executePending = this::executePending, onMessage = onMessage
    )
    private val app = context.applicationContext as MainApp
    private val pendingDownloads = mutableListOf<Download>()

    private lateinit var download: Download
    private lateinit var tempFileName: String

    fun execute(
        url: String, @Suppress("UNUSED_PARAMETER") userAgent: String,
        contentDisposition: String, mimeType: String, contentLength: Long
    ){
        download = Download(
            url = url,
            fileName = URLUtil.guessFileName(url, contentDisposition, mimeType),
            totalSize = contentLength
        )
        executePending()
    }

    fun execute(downloadUpdate: Download){
        if(downloadUpdate.actionType == ActionType.None){
            if(downloadUpdate.status.value == Status.Success){
                context.startActivity(
                    Notifier.getFileIntent(context, downloadUpdate.filePath)
                )
            }
            return
        }
        download = downloadUpdate
        executePending()
    }

    private fun executePending(){
        if(permissionManager.storagePermissionRequired()){
            permissionManager.launchRequest(PermissionManager.Type.Storage)
        }
        else {
            if(downloadFolder.exists() || downloadFolder.mkdirs()){
                if(download.actionType == ActionType.Resume
                    || download.actionType == ActionType.Pause)
                {
                    continueDownload(download); return
                }
                download.filePath = "${downloadFolder}/${download.fileName}"
                scope.launch {
                    tempFileName = download.fileName
                    var num = 0
                    val type = download.fileName.substring(fileName.lastIndexOf('.'))
                    val name = download.fileName.replace(type, "")
                    while(File(download.filePath).exists()){
                        download.fileName = "$name(${++num})$type"
                        download.filePath = "${downloadFolder}/${download.fileName}"
                    }
                    if(num > 0){
                        if(showDialog.value) onMessage(R.string.file_still_exist)
                        else showDialog.value = true
                        return@launch
                    }
                    else showDialog.value = false
                    download.dateTime = LocalDateTime.now()
                    download = downloadVm.add(download)
                    app.addNum++
                    continueDownload(download)
                }
            }
            else{
                Timber.e("Couldn't create download folder")
            }
        }
    }

    private fun continueDownload(download: Download){
        if(permissionManager.permissionNum == 0)
            permissionManager.showNotifPermission()
        if(mainServiceHolder.downloadService == null){
            Timber.d("Download Service is disconnected")
            pendingDownloads.add(download)
            startDownloadService()
        }
        else{
            mainServiceHolder.downloadService!!.executeTask(download)
        }
    }

    fun handleFileRename(input: String){
        if(input.isEmpty()){ // resume failed download
            download = app.errorDownloads[
                app.errorDownloads.indexOfFirst {
                    it.fileName == tempFileName
                }
            ].apply { url = download.url; actionType = ActionType.Resume }
            showDialog.value = false
        }
        else{
            val type = download.fileName.substring(download.fileName.lastIndexOf('.'))
            download.fileName = input
            if(!input.endsWith(type)){
                download.fileName += type
            }
        }
        executePending()
    }

    val fileName get() = download.fileName

    val isFailedDownload get() = app.errorDownloads.any { it.fileName == tempFileName }

    init {
        mainServiceHolder.connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                Timber.i("Service connection started...")
                mainServiceHolder.downloadService = (service as DownloadService.ServiceBinder).service
                mainServiceHolder.downloadService!!.sizeUpdater = downloadVm::update
                mainServiceHolder.downloadService!!.statusUpdater = { downloadItem ->
                    scope.launch {
                        if(downloadItem.status.value == Status.Error){
                            app.errorDownloads.add(downloadItem)
                        }
                        else{
                            app.errorDownloads.removeIf { it.id == downloadItem.id }
                        }
                    }
                    downloadVm.update(downloadItem.status, downloadItem.id)
                }
                app.ongoingDownloads.addAll(mainServiceHolder.downloadService!!.ongoingDownloads)
                pendingDownloads.forEach {
                    mainServiceHolder.downloadService!!.executeTask(it)
                }
                pendingDownloads.clear()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                mainServiceHolder.downloadService = null
            }
        }
        if(DownloadService.isRunning) startDownloadService()
    }

    private fun startDownloadService(){
        Intent(context, DownloadService::class.java).also { intent ->
            context.startForegroundService(intent)
            context.bindService(intent, mainServiceHolder.connection!!, Context.BIND_AUTO_CREATE)
        }
    }

    private fun onNotifPermission(granted: Boolean){
        if(granted){
            mainServiceHolder.downloadService?.notifier
                ?.showAllPendingNotifications()
        }
        else{
            downloadVm.showNotifBox.value =
                permissionManager.permissionNum == 1
        }
    }
}