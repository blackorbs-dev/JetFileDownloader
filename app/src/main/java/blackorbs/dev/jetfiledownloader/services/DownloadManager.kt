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
import blackorbs.dev.jetfiledownloader.ui.download.DownloadVM
import blackorbs.dev.jetfiledownloader.ui.startActivityWithChooser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.net.URI
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentLinkedQueue

class DownloadManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val downloadFolder: File,
    private val downloadVm: DownloadVM,
    private val showDialog: MutableState<Boolean>,
    private val showError: MutableState<Boolean>
) {
    lateinit var permissionManager: PermissionManager
    private var downloadService: DownloadService? = null
    private val serviceConnection: ServiceConnection
    private val app = (context.applicationContext as MainApp)
        .apply { newDownloadsCount.intValue = 0 }
    private val pendingDownloads = ConcurrentLinkedQueue<Download>()
    private var isRunning = false

    fun execute(
        url: String, @Suppress("Unused_Parameter") userAgent: String,
        contentDisposition: String, mimeType: String, contentLength: Long
    ){
        pendingDownloads.add( Download(
            url = url, totalSize = contentLength,
            fileName = getFileName(url, contentDisposition, mimeType)
        ) )
        executePending()
    }

    fun execute(download: Download){
        if(download.isSelected.value) return
        if(download.actionType == ActionType.None){
            if(download.status.value == Status.Success){
                context.startActivityWithChooser(
                    Notifier.getFileIntent(context, download.filePath),
                    R.string.open_file_using
                )
            }
            return
        }
        pendingDownloads.add(download)
        executePending()
    }

    fun executePending(){
        if(permissionManager.storagePermissionRequired()){
            permissionManager.launchRequest(PermissionManager.Type.Storage)
        }
        else if(downloadService == null){ startDownloadService() }
        else {
            if(downloadFolder.exists() || downloadFolder.mkdirs()){
                if(isRunning) return; isRunning = true
                scope.launch {
                    for(download: Download in pendingDownloads){
                        if(download.actionType == ActionType.Resume
                            || download.actionType == ActionType.Pause)
                        {
                            continueDownload(download); continue
                        }
                        download.filePath = "${downloadFolder}/${download.fileName}"
                        download.errorIndex =
                            app.errorDownloads.indexOfFirst { it.fileName == download.fileName }
                        var num = 0
                        val type = ".${download.type}"
                        val name = download.fileName.replace(type, "")
                        while(File(download.filePath).exists()){
                            download.fileName = "$name(${++num})$type"
                            download.filePath = "${downloadFolder}/${download.fileName}"
                        }
                        if(num > 0){
                            if(showDialog.value) showError.value = true
                            else showDialog.value = true; break
                        }
                        else showDialog.value = false
                        var newDownload = download
                        newDownload.dateTime = LocalDateTime.now()
                        newDownload = downloadVm.add(newDownload)
                        app.newDownloadsCount.intValue++
                        continueDownload(newDownload)
                    }
                    isRunning = false
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
        downloadService?.executeTask(download)
        pendingDownloads.poll()
    }

    fun onSelectedAction(actionType: ActionType) = when(actionType){
        ActionType.Delete -> downloadVm.deleteSelectedItems()
        ActionType.Share -> downloadVm.shareSelection(context)
        else -> {}
    }

    val selectedItemCount get() = downloadVm.selectedItemCount.value
    val isPendingDelete get() = downloadVm.isPendingDelete

    fun handleFileRename(input: String){
        var download = pendingDownloads.peek()
        if(download != null){
            if(input.isEmpty()){
                if(download.errorIndex != -1){ // resume failed download
                    download = app.errorDownloads[download.errorIndex]
                        .apply {
                            url = download!!.url
                            actionType = ActionType.Resume
                        }
                }
                else{
                    pendingDownloads.poll()
                }
                showDialog.value = false
            }
            else{
                val type = ".${download.type}"
                download.fileName = input
                if(!input.endsWith(type)){
                    download.fileName += type
                }
            }
            executePending()
        }
    }

    val fileName get() = pendingDownloads.peek()?.fileName?:""

    val isFailedDownload get() = pendingDownloads.peek()?.errorIndex != -1

    private fun getFileName(url: String, contentDisposition: String, mimeType: String): String {
        var fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        if(fileName.endsWith(".bin")){
            URI(url).path?.let {File(it).name}?.run {
                if(isNotEmpty()) fileName = this
            }
        }
        if(fileName.endsWith(".bin") && contentDisposition.contains("filename"))
            fileName = contentDisposition.replaceFirst(
                "(?i)^.*filename=\"?([^\"]+)\"?.*$".toRegex(), "$1"
            )
        return fileName
    }

    init {
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                Timber.i("Service connection started...")
                downloadService = (service as DownloadService.ServiceBinder).service
                downloadService?.serviceDisconnector =
                    this@DownloadManager::disconnectService
                executePending()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                downloadService = null
            }
        }
        if(DownloadService.isRunning) startDownloadService()
    }

    private fun startDownloadService(){
        Intent(context, DownloadService::class.java).also { intent ->
            context.startForegroundService(intent)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun disconnectService(){
        downloadService?.let {
            Timber.d("Disconnecting from Service")
            context.unbindService(serviceConnection)
        }
    }

    fun onNotifPermission(granted: Boolean){
        if(granted){
            downloadService?.notifier
                ?.showAllPendingNotifications()
            downloadVm.showNotifBox.value = false
        }
        else{
            downloadVm.showNotifBox.value =
                permissionManager.permissionNum == 1
        }
    }
}