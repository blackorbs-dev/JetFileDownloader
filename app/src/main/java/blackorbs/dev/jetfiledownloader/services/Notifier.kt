package blackorbs.dev.jetfiledownloader.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service.NOTIFICATION_SERVICE
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider.getUriForFile
import blackorbs.dev.jetfiledownloader.R
import blackorbs.dev.jetfiledownloader.entities.Download
import blackorbs.dev.jetfiledownloader.entities.Status
import blackorbs.dev.jetfiledownloader.ui.MainActivity
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class Notifier(private val context: Context) {
    private val notifMan: NotificationManager =
        context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    private val pendingNotifications: MutableMap<Int, Notification>
    val summaryNotif: Notification
    val notifType: Int
    private var notifRandomID = 2 // ID for Downloads notification, Starts at 2: Summary notif can be 0 or 1

    val handler: Handler

    init {
        val notifChannel = NotificationChannel(
            context.packageName, context.getString(R.string.notif_channel_title),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notifChannel.description = context.getString(R.string.notif_channel_desc)
        notifMan.createNotificationChannel(notifChannel)
        summaryNotif = getSummaryNotification()
        notifType = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        pendingNotifications = ConcurrentHashMap()

        handler = Handler(Looper.getMainLooper())
    }

    fun showAllPendingNotifications(){
        pendingNotifications.forEach(this::showNotification)
    }

    fun showUpdate(fileName: String, status: Status){
        if(status == Status.Success)
            showToast(context.getString(R.string.download_success, fileName))
        else if(status == Status.Error)
            showToast(context.getString(R.string.download_error, fileName, status.text))
    }

    fun showToast(message: String) {
        handler.post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    fun showNotification(download: Download, notifBuilder: NotificationCompat.Builder){
        showNotification(
            download.apply {
                if(notifID == -1) notifID = notifRandomID++
            }.notifID,
            notifBuilder.build()
        )
    }

    fun showNotification(id: Int, notification: Notification) {
        if (permissionNotGranted()) {
            if(pendingNotifications.containsKey(id)) return
            pendingNotifications[id] = notification
            return
        }
        notifMan.notify(id, notification)
        pendingNotifications.remove(id)
        notifMan.notify(SUMMARY_NOTIF_ID, summaryNotif)
    }

    fun getNotificationBuilder(download: Download) =
        NotificationCompat.Builder(context, context.packageName)
            .setSmallIcon(R.drawable.ic_downloading_24)
            .setContentTitle(download.fileName)
            .setContentIntent(getMainIntent())
            .setGroup(TAG).setSilent(true)

    private fun getSummaryNotification() = NotificationCompat.Builder(context, context.packageName)
        .apply {
            setSmallIcon(R.mipmap.ic_launcher)
            setContentTitle(context.getString(R.string.service_started))
            setContentIntent(getMainIntent())
            setGroupSummary(true)
            setGroup(TAG)
            setSilent(true)
            addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.stop_service),
                actionIntent(R.string.stop_service)
            )
            addAction(
                android.R.drawable.ic_notification_clear_all,
                context.getString(R.string.stop_all_tasks),
                actionIntent(R.string.stop_all_tasks)
            )
        }.build()

    private fun getMainIntent() = PendingIntent.getActivity(context, 0,
        Intent(context, MainActivity::class.java).putExtra(MAIN_KEY, 419),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun actionIntent(@StringRes actionResID: Int, id: Long = -1): PendingIntent =
        PendingIntent.getForegroundService(context, 0,
            Intent(context, DownloadService::class.java
            ).setAction(context.getString(actionResID))
                .putExtra(context.getString(actionResID), id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun getPendingFileIntent(filepath: String): PendingIntent {
        return PendingIntent.getActivity(context, 0, getFileIntent(context, filepath),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @SuppressLint("InlinedApi")
    private fun permissionNotGranted() = ActivityCompat.checkSelfPermission(context,
        Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    companion object {
        const val SUMMARY_NOTIF_ID: Int = 423
        const val TAG = "JetDownloadService"
        const val MAIN_KEY = "MainKey"

        fun getFileIntent(context: Context, filepath: String): Intent =
            Intent(Intent.ACTION_VIEW).apply {
                if (filepath.endsWith(".apk"))
                    setDataAndType(getUriForFile(
                        context, "${context.packageName}.provider", File(filepath)
                    ), "application/vnd.android.package-archive")
                else setData(getUriForFile(
                    context, "${context.packageName}.provider", File(filepath)
                ))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

        fun getFileUri(context: Context, filepath: String): Uri? {
            with(File(filepath)){
                if(exists()){
                    return getUriForFile(context,
                        "${context.packageName}.provider",
                        this
                    )
                }
            }
            return null
        }
    }
}