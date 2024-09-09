package blackorbs.dev.jetfiledownloader.entities

import android.content.Context
import android.text.format.Formatter
import androidx.annotation.StringRes
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import blackorbs.dev.jetfiledownloader.R
import java.time.LocalDateTime

@Entity(tableName = "downloads")
data class Download(
    var url: String, var fileName: String, val totalSize: Long,
    var status: MutableState<Status> = mutableStateOf(Status.Queued),
    @PrimaryKey(autoGenerate = true) var id: Long = 0L
){
    lateinit var filePath: String
    lateinit var dateTime: LocalDateTime

    var currentSize = mutableLongStateOf(0L)

    val sizePercent: Int get() = if(totalSize == 0L) 100 else
        Math.floorDiv(currentSize.longValue*100, totalSize).toInt()

    @Ignore
    fun totalMB(context: Context): String = Formatter.formatFileSize(
        context, totalSize
    )

    @Ignore
    var actionType = ActionType.None

    val statusColor: Color get() = when(status.value){
        Status.Success -> Color.hsl(125f,1f,0.3f)
        Status.Ongoing -> Color.Black
        Status.Error -> Color.Red
        else -> Color.DarkGray
    }
}

enum class ActionType{
    None, Pause, Resume
}

enum class Status(@StringRes val titleResID: Int, var text: String = ""){
    Queued(R.string.queued),
    Ongoing(R.string.ongoing),
    Paused(R.string.paused),
    Success(R.string.success),
    Error(R.string.error),
    Deleted(R.string.deleted)
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

    @TypeConverter
    fun sizeToLong(size: MutableLongState) = size.longValue
    @TypeConverter
    fun longToSize(long: Long): MutableLongState = mutableLongStateOf(long)
}