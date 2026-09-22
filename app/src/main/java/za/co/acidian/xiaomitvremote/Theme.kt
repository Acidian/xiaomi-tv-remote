package za.co.acidian.xiaomitvremote

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Design tokens for the remote UI.
 *
 * Kept as one file so every screen pulls from the same palette, spacing scale,
 * and corner-radius set instead of re-declaring magic numbers per Composable.
 * Values are unchanged from the original inline declarations; this is a pure
 * extraction, no visual change yet.
 */
object RemoteColors {
    val Background = Color(0xFF111318)
    val Surface = Color(0xFF1B1E25)
    val Accent = Color(0xFF7DA7FF)
    val OnAccent = Color(0xFF0A0E14)
    val Connected = Color(0xFF8CE99A)
    val TextSecondary = Color.LightGray
    val TextMuted = Color.Gray
}

/** 4dp base grid. Use these instead of ad hoc `.dp` literals in layout code. */
object RemoteSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 14.dp
    val lg = 18.dp
    val xl = 24.dp
}

private val RemoteDarkColorScheme = darkColorScheme(
    primary = RemoteColors.Accent,
    onPrimary = RemoteColors.OnAccent,
    background = RemoteColors.Background,
    onBackground = Color.White,
    surface = RemoteColors.Surface,
    onSurface = Color.White,
)

private val RemoteShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun XiaomiRemoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RemoteDarkColorScheme,
        shapes = RemoteShapes,
        content = content,
    )
}
