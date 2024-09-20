package blackorbs.dev.jetfiledownloader.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = DeepPurple,
    tertiary = PurpleGrey80,
    onPrimary = DeepPurple,
    onSecondary = PurpleGrey40,
    onSecondaryContainer = White80,
    onTertiary = PurpleGrey80,
    primaryContainer = Purple80,
    onPrimaryContainer = PurpleGrey80,
    outline = Purple80,
    outlineVariant = PurpleGrey40,
    surface = DeepPurple,
    onSurface = PurpleGrey80,
    background = PurpleGrey40,
    onBackground = PurpleGrey80,
    surfaceDim = DeepPurpleDim,
    error = Orange40
)

private val LightColorScheme = lightColorScheme(
    primary = DeepBlue,
    secondary = DeepBlue,
    tertiary = Purple40,
    onPrimary = Purple10,
    onSecondary = Purple10,
    onTertiary = White80,
    primaryContainer = Pink40,
    onPrimaryContainer = PurpleGrey40,
    outline = DeepBlue,
    outlineVariant = Purple40,
    surface = Pink80,
    onSurface = DeepBlue,
    background = Purple10,
    onBackground = DeepBlue,
    surfaceDim = DeepBlueDim,
    error = Pink
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    val customColors = if(darkTheme) DarkCustomColors else CustomColors()

    CompositionLocalProvider(LocalCustomColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}