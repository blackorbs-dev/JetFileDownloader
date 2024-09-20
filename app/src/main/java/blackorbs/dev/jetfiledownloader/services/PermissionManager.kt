package blackorbs.dev.jetfiledownloader.services

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import blackorbs.dev.jetfiledownloader.R
import blackorbs.dev.jetfiledownloader.services.PermissionManager.Type

@Composable
fun rememberPermissionManager(
    context: Context,
    executePendingDownloads: () -> Unit,
    onMessage: (String) -> Unit,
    onNotifPermission: (Boolean) -> Unit
)
= remember { PermissionManager(context, onNotifPermission) }.apply {
        permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if(type == Type.Storage){
                if(granted){ executePendingDownloads() }
                else{ onMessage(context.getString(R.string.permission_denied)) }
            }
            else {
                updateNotifPermission(granted)
            }
        }
    }

class PermissionManager(
    private val context: Context,
    private val onNotifPermission: (Boolean) -> Unit
) {
    lateinit var permissionLauncher: ManagedActivityResultLauncher<String, Boolean>
    private var prefs: SharedPreferences =
        context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
    var permissionNum = 0
    internal var type = Type.Storage

    init {
        permissionNum = prefs.getInt(PERM_KEY, 0)
    }

    @SuppressLint("InlinedApi")
    fun launchRequest(type: Type){
        this.type = type
        val permission = when(type){
            Type.Storage -> Manifest.permission.WRITE_EXTERNAL_STORAGE
            Type.Notification -> Manifest.permission.POST_NOTIFICATIONS
        }
        permissionLauncher.launch(permission)
    }

    fun storagePermissionRequired() =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                permissionNotGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private fun permissionNotGranted(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, permission
        ) != PackageManager.PERMISSION_GRANTED
    }

    fun updateNotifPermission(granted: Boolean){
        if(!granted){
            prefs.edit{ putInt(PERM_KEY, ++permissionNum) }
        }
        onNotifPermission(granted)
    }

    fun showNotifPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && permissionNotGranted(Manifest.permission.POST_NOTIFICATIONS)
            && permissionNum < 2
            ) { launchRequest(Type.Notification) }
    }

    enum class Type{ Storage, Notification }

    companion object{
        const val PERM_KEY = "PermKey"
    }
}