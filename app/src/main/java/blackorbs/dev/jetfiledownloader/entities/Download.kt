package blackorbs.dev.jetfiledownloader.entities

import android.content.Context
import android.text.format.Formatter
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import blackorbs.dev.jetfiledownloader.R
import java.time.LocalDateTime

@Entity(tableName = "downloads")
data class Download(
    var url: String, var fileName: String, var totalSize: Long,
    var status: MutableState<Status> = mutableStateOf(Status.Queued),
    @PrimaryKey(autoGenerate = true) var id: Long = 0L
){
    lateinit var filePath: String
    lateinit var dateTime: LocalDateTime

    var currentSize = 0L

    @Ignore var isSelected = mutableStateOf(false)
    @Ignore var isPendingDelete = mutableStateOf(false)

    @Ignore var height = 0.dp

    @Ignore var errorIndex = -1

    @Ignore var notifID = -1

    @Ignore var actionType = ActionType.None

    @Ignore val sizePercent = mutableIntStateOf(0)

    @Ignore fun publishProgress() {
        sizePercent.intValue = if(totalSize == 0L) 0 else
            Math.floorDiv(currentSize*100, totalSize).toInt()
    }

    val isNotCompleted get() = status.value != Status.Success
            && status.value != Status.Deleted

    val isPending get() = status.value == Status.Queued
            || status.value == Status.Ongoing

    val type by lazy {
        if(fileName.contains('.'))
            fileName.substring(fileName.lastIndexOf('.')+1)
                .apply {
                    if(length > 4 || isEmpty()) return@lazy "bin"
                }
        else "bin"
    }
}

@Composable
internal fun Long.formatAsFileSize(): String =
    formatAsFileSize(LocalContext.current)

internal fun Long.formatAsFileSize(context: Context): String =
    Formatter.formatFileSize(context, this)

enum class ActionType{ None, Pause, Resume, Select,
    Delete, UndoDelete, ShowInfo, Share
}

enum class Status(@StringRes val titleResID: Int, var text: String = ""){
    Queued(R.string.queued), Ongoing(R.string.ongoing), Paused(R.string.paused),
    Success(R.string.success), Error(R.string.error), Deleted(R.string.deleted)
}

enum class LayoutState{
    None, PendingDelete, SelectionMode
}

class Converter{
    private val separator = ",,"

    @TypeConverter
    fun timeToString(time: LocalDateTime) = time.toString()
    @TypeConverter
    fun stringToTime(string: String): LocalDateTime =
        LocalDateTime.parse(string)

    @TypeConverter
    fun statusToString(status: MutableState<Status>) =
        "${status.value.name}$separator${status.value.text}"
    @TypeConverter
    fun stringToStatus(string: String): MutableState<Status>{
        val arr = string.split(separator, limit = 2)
        return mutableStateOf(Status.valueOf(arr[0]).apply { text = arr[1] })
    }
}