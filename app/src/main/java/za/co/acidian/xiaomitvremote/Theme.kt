package za.co.acidian.xiaomitvremote

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Palette for one theme. Every accent has three companions so a colour is never
 * borrowed for a job it can't do: a solid fill, the text that sits on that fill,
 * and a tonal container with its own text colour. Values are authored in OKLCH
 * and converted to sRGB, which keeps the neutrals tinted toward the brand violet
 * instead of drifting to grey, and keeps chroma in check at the extremes.
 *
 * Contrast (WCAG 2.1, measured against the background):
 * dark  text 17.4:1, muted 9.5:1, faint 6.1:1, accent 5.7:1, live 10.8:1, warm 11.5:1, danger 6.0:1
 * light text 15.8:1, muted 7.3:1, faint 4.5:1, accent 5.8:1, danger 4.8:1
 */
data class RemotePalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val outline: Color,
    val outlineVariant: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val textFaint: Color,
    val accent: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val live: Color,
    val onLive: Color,
    val liveContainer: Color,
    val onLiveContainer: Color,
    val warm: Color,
    val onWarm: Color,
    val warmContainer: Color,
    val onWarmContainer: Color,
    val danger: Color,
    val onDanger: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
)

internal val DarkRemotePalette = RemotePalette(
    isDark = true,
    background = Color(0xFF0F0F1D),
    surface = Color(0xFF1C1C2E),
    surfaceHigh = Color(0xFF2B2B41),
    outline = Color(0xFF4D4D63),
    outlineVariant = Color(0xFF313142),
    textPrimary = Color(0xFFF4F4FD),
    textMuted = Color(0xFFB5B6C7),
    textFaint = Color(0xFF9090A3),
    accent = Color(0xFFA071FC),
    onAccent = Color(0xFF0F0F1D),
    accentContainer = Color(0xFF432F6B),
    onAccentContainer = Color(0xFFE1D7FF),
    live = Color(0xFF35DCAB),
    onLive = Color(0xFF0F0F1D),
    liveContainer = Color(0xFF00402E),
    onLiveContainer = Color(0xFFB8EBD6),
    warm = Color(0xFFF5C252),
    onWarm = Color(0xFF0F0F1D),
    warmContainer = Color(0xFF4C3703),
    onWarmContainer = Color(0xFFFBE5BB),
    danger = Color(0xFFF75D57),
    onDanger = Color(0xFF0F0F1D),
    dangerContainer = Color(0xFF682320),
    onDangerContainer = Color(0xFFFFD0C9),
)

internal val LightRemotePalette = RemotePalette(
    isDark = false,
    background = Color(0xFFF6F6FC),
    surface = Color(0xFFFDFDFF),
    surfaceHigh = Color(0xFFEAEAF3),
    outline = Color(0xFFB6B6C4),
    outlineVariant = Color(0xFFDDDDE6),
    textPrimary = Color(0xFF1B1B2B),
    textMuted = Color(0xFF505063),
    textFaint = Color(0xFF6F7082),
    accent = Color(0xFF7A37DA),
    onAccent = Color(0xFFFDFDFF),
    accentContainer = Color(0xFFEAE3FF),
    onAccentContainer = Color(0xFF4B2B83),
    live = Color(0xFF009871),
    onLive = Color(0xFF1B1B2B),
    liveContainer = Color(0xFFCFF1E3),
    onLiveContainer = Color(0xFF00503A),
    warm = Color(0xFFC98A0E),
    onWarm = Color(0xFF1B1B2B),
    warmContainer = Color(0xFFFAEDD5),
    onWarmContainer = Color(0xFF714900),
    danger = Color(0xFFD42325),
    onDanger = Color(0xFFFDFDFF),
    dangerContainer = Color(0xFFFFE2DD),
    onDangerContainer = Color(0xFF901114),
)

val LocalRemotePalette = staticCompositionLocalOf { DarkRemotePalette }

/** 4dp grid. Layout code uses these instead of ad hoc `.dp` literals. */
object RemoteSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** Side gutter for every screen. */
    val screen = 20.dp
}

private val RemoteShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

private val Sans = FontFamily.SansSerif

private val RemoteTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = Sans,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.6).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Sans,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.4).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Sans,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Sans,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = TextStyle(
        fontFamily = Sans,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontFamily = Sans,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
    ),
    labelLarge = TextStyle(
        fontFamily = Sans,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Sans,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Sans,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
    ),
)

/** Material roles are mapped from the same tokens so stock M3 components stay on-brand. */
private fun RemotePalette.toColorScheme(dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentContainer,
        onPrimaryContainer = onAccentContainer,
        secondary = live,
        onSecondary = onLive,
        secondaryContainer = liveContainer,
        onSecondaryContainer = onLiveContainer,
        tertiary = warm,
        onTertiary = onWarm,
        tertiaryContainer = warmContainer,
        onTertiaryContainer = onWarmContainer,
        error = danger,
        onError = onDanger,
        errorContainer = dangerContainer,
        onErrorContainer = onDangerContainer,
        background = background,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceHigh,
        onSurfaceVariant = textMuted,
        outline = outline,
        outlineVariant = outlineVariant,
        inverseSurface = textPrimary,
        inverseOnSurface = background,
        inversePrimary = accent,
        scrim = Color(0xFF07070F),
    )
} else {
    lightColorScheme(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentContainer,
        onPrimaryContainer = onAccentContainer,
        secondary = live,
        onSecondary = onLive,
        secondaryContainer = liveContainer,
        onSecondaryContainer = onLiveContainer,
        tertiary = warm,
        onTertiary = onWarm,
        tertiaryContainer = warmContainer,
        onTertiaryContainer = onWarmContainer,
        error = danger,
        onError = onDanger,
        errorContainer = dangerContainer,
        onErrorContainer = onDangerContainer,
        background = background,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceHigh,
        onSurfaceVariant = textMuted,
        outline = outline,
        outlineVariant = outlineVariant,
        inverseSurface = textPrimary,
        inverseOnSurface = surface,
        inversePrimary = accent,
        scrim = Color(0xFF1B1B2B),
    )
}

/**
 * Dark by default (this is a remote used with the lights down), but a real light
 * palette follows the system setting so daytime use never becomes a dark slab.
 */
@Composable
fun XiaomiRemoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkRemotePalette else LightRemotePalette
    CompositionLocalProvider(LocalRemotePalette provides palette) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(darkTheme),
            typography = RemoteTypography,
            shapes = RemoteShapes,
            content = content,
        )
    }
}
