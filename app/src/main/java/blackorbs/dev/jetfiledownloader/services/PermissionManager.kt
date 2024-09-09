package blackorbs.dev.jetfiledownloader.services

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import blackorbs.dev.jetfiledownloader.R

class PermissionManager(
    private val context: Context,
    private val executePending: () -> Unit,
    private val onMessage: (Int) -> Unit,
    private val onNotifPermission: (Boolean) -> Unit,
    private var permissionRequestLauncher: ActivityResultLauncher<String>? = null
) {
    private var prefs: SharedPreferences =
        context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
    var permissionNum = 0

    init {
        permissionNum = prefs.getInt(PERM_KEY, 0)
    }

    @SuppressLint("InlinedApi")
    fun launchRequest(type: Type){
        val permission = when(type){
            Type.Storage -> Manifest.permission.WRITE_EXTERNAL_STORAGE
            Type.Notification -> Manifest.permission.POST_NOTIFICATIONS
        }
        if(permissionRequestLauncher == null){
            permissionRequestLauncher = (context as ComponentActivity).registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if(type == Type.Storage){
                    if(granted){ executePending }
                    else{ onMessage(R.string.permission_denied) }
                }
                else {
                    updateNotifPermission(granted)
                }
            }
        }
        permissionRequestLauncher?.launch(permission)
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
            permissionNum++
            prefs.edit{ putInt(PERM_KEY, permissionNum) }
        }
        onNotifPermission(granted)
    }

    fun showNotifPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && permissionNotGranted(Manifest.permission.POST_NOTIFICATIONS)
            && permissionNum < 2
            ) {
            launchRequest(Type.Notification)
        }
    }

    enum class Type{ Storage, Notification }

    companion object{
        const val PERM_KEY = "PermKey"
    }
}