package blackorbs.dev.jetfiledownloader.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import blackorbs.dev.jetfiledownloader.entities.Status

val DeepPurple = Color(0xFF1E0138)
val DeepPurpleDim = Color(0x661E0138)
val Purple80 = Color(0xFFBFA5FC)
val Purple10 = Color(0xFFF5E6FF)
val PurpleGrey80 = Color(0xFFB4AAC4)
val Pink = Color(0xFFFC03DB)
val Pink80 = Color(0xFFEFB8C8)
val DeepBlue = Color(0xFF000141)
val DeepBlueDim = Color(0x66000141)
val White80 = Color(0xFFC5C5C5)
val Green40 = Color(0xFF1F832C)
val Green60 = Color(0xFF12AF26)
val Red40 = Color(0xFFD34403)
val Orange40 = Color(0xFFD67208)

val Purple60 = Color(0xFF4817D3)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF4B445C)
val Pink40 = Color(0xFF7D5260)
val Blue60 = Color(0xFF030699)

@Composable @ReadOnlyComposable
fun statusColor(status: Status) = when(status){
    Status.Success -> LocalCustomColors.current.statusSuccess
    Status.Ongoing -> LocalCustomColors.current.statusOngoing
    Status.Error -> MaterialTheme.colorScheme.error
    else -> LocalCustomColors.current.statusDefault
}
@Composable @ReadOnlyComposable
fun typeColor(type: String) = when(type.lowercase()){
    "pdf" -> LocalCustomColors.current.fileRed40
    "doc", "docx" -> LocalCustomColors.current.fileBlue60
    "zip", "dll" -> LocalCustomColors.current.fileDeepBlue
    "jpg", "rar", "mov" -> LocalCustomColors.current.fileGreen40
    "xls", "csv", "apk" -> LocalCustomColors.current.fileGreen60
    "png", "iso", "txt" -> LocalCustomColors.current.filePurple40
    "ppt", "svg", "mp3", "mpeg" -> LocalCustomColors.current.fileOrange40
    "3gp", "mp4", "gif", "flv", "mkv"  -> LocalCustomColors.current.filePurple60
    else -> LocalCustomColors.current.default
}

val DarkCustomColors = CustomColors(
    fileGreen40 = Purple80,
    fileRed40 = Purple80,
    fileBlue60 = Purple80,
    fileDeepBlue = Purple80,
    fileGreen60 = Purple80,
    filePurple40 = Purple80,
    fileOrange40 = Purple80,
    filePurple60 = Purple80,
    statusSuccess = Purple80,
    statusOngoing = Purple80,
    statusDefault = Purple80,
    default = Purple80
)
val LocalCustomColors = staticCompositionLocalOf { CustomColors() }

@Immutable
data class CustomColors(
    val fileGreen40: Color = Green40,
    val fileRed40: Color = Red40,
    val fileBlue60: Color = Blue60,
    val fileDeepBlue: Color = DeepBlue,
    val fileGreen60: Color = Green60,
    val filePurple40: Color = Purple40,
    val fileOrange40: Color = Orange40,
    val filePurple60: Color = Purple60,
    val statusSuccess: Color = Green40,
    val statusOngoing: Color = Pink40,
    val statusDefault: Color = PurpleGrey40,
    val default: Color = Pink
)

