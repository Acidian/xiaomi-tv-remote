package za.co.acidian.xiaomitvremote

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * The one button primitive the whole remote is built from.
 *
 * Pressing shrinks the key slightly and fires a haptic tick, so a tap is felt as
 * well as seen: on a phone held at arm's length, that's the difference between
 * "did it register?" and confidence. Keys that make sense to hold down (D-pad,
 * volume) repeat while pressed instead of forcing a burst of taps.
 */
@Composable
internal fun RemoteKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    label: String? = null,
    hint: String? = null,
    container: Color = LocalRemotePalette.current.surfaceHigh,
    contentColor: Color = LocalRemotePalette.current.textPrimary,
    shape: Shape = RoundedCornerShape(20.dp),
    size: Dp? = null,
    iconSize: Dp = 24.dp,
    fontSize: TextUnit = 14.sp,
    weight: FontWeight = FontWeight.SemiBold,
    repeatWhileHeld: Boolean = false,
    enabled: Boolean = true,
) {
    val palette = LocalRemotePalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing),
        label = "key-scale",
    )
    val haptics = LocalHapticFeedback.current

    if (repeatWhileHeld && enabled) {
        // Cancelled automatically the moment `pressed` flips back to false.
        LaunchedEffect(pressed) {
            if (!pressed) return@LaunchedEffect
            delay(420)
            while (true) {
                onClick()
                delay(110)
            }
        }
    }

    val tint = if (enabled) contentColor else palette.textFaint

    Box(
        modifier = modifier
            .then(
                if (size != null) {
                    Modifier.size(size)
                } else {
                    Modifier.defaultMinSize(minWidth = 56.dp, minHeight = 52.dp)
                },
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(if (enabled) container else palette.surfaceHigh)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        when {
            icon != null && label != null -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RemoteSpacing.sm),
                modifier = Modifier.padding(horizontal = RemoteSpacing.md),
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
                Text(label, color = tint, fontSize = fontSize, fontWeight = weight, maxLines = 1)
            }

            label != null -> Text(
                text = label,
                modifier = if (size == null) Modifier.padding(horizontal = RemoteSpacing.lg) else Modifier,
                color = tint,
                fontSize = fontSize,
                fontWeight = weight,
                maxLines = 1,
            )

            icon != null -> Icon(
                imageVector = icon,
                contentDescription = hint,
                tint = tint,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
internal fun StatusDot(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(9.dp).clip(CircleShape).background(color))
}

/** Title block used at the top of the non-remote tabs. */
@Composable
internal fun ScreenHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalRemotePalette.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(RemoteSpacing.xs)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = palette.textPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = palette.textMuted)
    }
}

/** Small uppercase grouping label; used sparingly, not above every row. */
@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = LocalRemotePalette.current.textFaint,
    )
}

/**
 * D-pad drawn as a physical dial: a tinted plate with the four directions riding
 * its edge and a solid accent OK in the middle. Holding a direction repeats it,
 * which is how you get through a long TV list without tapping forty times.
 */
@Composable
internal fun DPad(
    diameter: Dp,
    onKey: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalRemotePalette.current
    val arrow = diameter * 0.28f
    val ok = diameter * 0.38f
    val inset = diameter * 0.032f

    Box(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .background(
                if (palette.isDark) {
                    Brush.radialGradient(
                        0f to palette.surfaceHigh,
                        0.66f to palette.surface,
                        1f to palette.background,
                    )
                } else {
                    // In light mode the plate needs a wash of accent or it disappears
                    // into the page: a pale violet centre fading out to the surface.
                    Brush.radialGradient(
                        0f to palette.accentContainer,
                        0.72f to palette.surface,
                        1f to palette.background,
                    )
                },
            )
            .border(1.dp, palette.outlineVariant, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        // A soft accent bloom behind OK gives the dial depth without any glass effect.
        Box(
            modifier = Modifier
                .size(diameter * 0.66f)
                .background(
                    Brush.radialGradient(
                        listOf(
                            palette.accent.copy(alpha = if (palette.isDark) 0.20f else 0.12f),
                            Color.Transparent,
                        ),
                    ),
                    CircleShape,
                ),
        )
        RemoteKey(
            onClick = { onKey(AndroidTvRemoteClient.KEY_DPAD_UP) },
            icon = Icons.Rounded.KeyboardArrowUp,
            hint = "Up",
            modifier = Modifier.align(Alignment.TopCenter).padding(top = inset),
            size = arrow,
            container = Color.Transparent,
            contentColor = palette.textPrimary,
            shape = CircleShape,
            iconSize = arrow * 0.64f,
            repeatWhileHeld = true,
        )
        RemoteKey(
            onClick = { onKey(AndroidTvRemoteClient.KEY_DPAD_DOWN) },
            icon = Icons.Rounded.KeyboardArrowDown,
            hint = "Down",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = inset),
            size = arrow,
            container = Color.Transparent,
            contentColor = palette.textPrimary,
            shape = CircleShape,
            iconSize = arrow * 0.64f,
            repeatWhileHeld = true,
        )
        RemoteKey(
            onClick = { onKey(AndroidTvRemoteClient.KEY_DPAD_LEFT) },
            icon = Icons.Rounded.KeyboardArrowLeft,
            hint = "Left",
            modifier = Modifier.align(Alignment.CenterStart).padding(start = inset),
            size = arrow,
            container = Color.Transparent,
            contentColor = palette.textPrimary,
            shape = CircleShape,
            iconSize = arrow * 0.64f,
            repeatWhileHeld = true,
        )
        RemoteKey(
            onClick = { onKey(AndroidTvRemoteClient.KEY_DPAD_RIGHT) },
            icon = Icons.Rounded.KeyboardArrowRight,
            hint = "Right",
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = inset),
            size = arrow,
            container = Color.Transparent,
            contentColor = palette.textPrimary,
            shape = CircleShape,
            iconSize = arrow * 0.64f,
            repeatWhileHeld = true,
        )
        RemoteKey(
            onClick = { onKey(AndroidTvRemoteClient.KEY_DPAD_CENTER) },
            label = "OK",
            hint = "Select",
            size = ok,
            container = palette.accent,
            contentColor = palette.onAccent,
            shape = CircleShape,
            fontSize = 19.sp,
            weight = FontWeight.Black,
        )
    }
}
