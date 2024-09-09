package blackorbs.dev.jetfiledownloader.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Blue60,
    secondary = Pink80,
    tertiary = Purple40,
    onPrimary = White80,
    onSecondary = PurpleGrey80,
    onTertiary = PurpleGrey80
)

private val LightColorScheme = lightColorScheme(
    primary = Blue90,
    secondary = Pink,
    tertiary = Blue60,
    onPrimary = White,
    onSecondary = Purple80,
    onTertiary = White80,
    primaryContainer = Purple40,
    onPrimaryContainer = PurpleGrey40,
    outline = Blue90,
    outlineVariant = Blue60,
    surface = Purple10,
    onSurface = PurpleGrey40,
    background = White,
    onBackground = Blue90

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun JetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}