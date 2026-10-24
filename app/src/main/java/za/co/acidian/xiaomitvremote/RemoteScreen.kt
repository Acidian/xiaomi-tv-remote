package za.co.acidian.xiaomitvremote

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The remote itself, designed to fit one thumb-reachable screen with no scrolling:
 * status and power at the top, the D-pad as the centrepiece, volume under the thumb,
 * transport keys along the bottom edge. Everything here is a primary action, so
 * nothing competes with the D-pad for size.
 *
 * The dial is sized from the space actually left over rather than a fixed dp, so a
 * tall phone gets a bigger dial instead of two bands of dead space, and a short one
 * still fits without scrolling.
 */
@Composable
internal fun RemoteScreen(
    remote: AndroidTvRemoteClient,
    status: String,
    deviceLabel: String,
    power: Boolean?,
    onOpenSystemKeys: () -> Unit,
) {
    val palette = LocalRemotePalette.current
    var playing by rememberSaveable { mutableStateOf(true) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val chrome = 322.dp
        val slack = (maxHeight - chrome).coerceAtLeast(0.dp)
        val dial = minOf(
            (maxWidth - RemoteSpacing.screen * 2) * 0.94f,
            slack * 0.78f,
            348.dp,
        )

        Column(Modifier.fillMaxSize().padding(horizontal = RemoteSpacing.screen)) {
            Spacer(Modifier.height(RemoteSpacing.sm))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(RemoteSpacing.xxs),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(RemoteSpacing.sm),
                    ) {
                        StatusDot(if (power == false) palette.warm else palette.live)
                        Text(
                            text = deviceLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = if (power == false) "TV asleep · $status" else status,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                RemoteKey(
                    onClick = onOpenSystemKeys,
                    icon = Icons.Rounded.MoreHoriz,
                    hint = "More keys",
                    size = 48.dp,
                    shape = CircleShape,
                    container = palette.surfaceHigh,
                    contentColor = palette.textMuted,
                )
                Spacer(Modifier.width(RemoteSpacing.sm))
                RemoteKey(
                    onClick = { remote.sendKey(AndroidTvRemoteClient.KEY_POWER) },
                    icon = Icons.Rounded.PowerSettingsNew,
                    hint = "Power",
                    size = 56.dp,
                    shape = CircleShape,
                    container = palette.danger,
                    contentColor = palette.onDanger,
                    iconSize = 27.dp,
                )
            }

            Spacer(Modifier.height(RemoteSpacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RemoteSpacing.sm),
            ) {
                RemoteKey(
                    onClick = { remote.sendKey(AndroidTvRemoteClient.KEY_BACK) },
                    icon = Icons.Rounded.ArrowBack,
                    label = "Back",
                    hint = "Back",
                    modifier = Modifier.weight(1f).height(58.dp),
                    iconSize = 19.dp,
                )
                RemoteKey(
                    onClick = { remote.sendKey(AndroidTvRemoteClient.KEY_HOME) },
                    icon = Icons.Rounded.Home,
                    label = "Home",
                    hint = "Home",
                    modifier = Modifier.weight(1f).height(58.dp),
                    iconSize = 19.dp,
                )
                RemoteKey(
                    onClick = { remote.sendKey(AndroidTvRemoteClient.KEY_MENU) },
                    icon = Icons.Rounded.Menu,
                    label = "Menu",
                    hint = "Menu",
                    modifier = Modifier.weight(1f).height(58.dp),
                    iconSize = 19.dp,
                )
            }

            Spacer(Modifier.weight(1f))

            DPad(diameter = dial, onKey = { remote.sendKey(it) })

            Spacer(Modifier.weight(1f))

            VolumeBar(remote)

            Spacer(Modifier.height(RemoteSpacing.md))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(palette.surface)
                    .padding(horizontal = RemoteSpacing.xs, vertical = RemoteSpacing.sm),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RemoteKey(
                        onClick = { remote.sendKey(AndroidTvRemoteClient.KEY_MEDIA_PREVIOUS) },
                        icon = Icons.Rounded.SkipPrevious,
                        hint = "Previous track",
                        size = 50.dp,
                        shape = CircleShape,
                        container = Color.Transparent,
                        contentColor = palette.textMuted,
                        iconSize = 26.dp,
                    )
                    RemoteKey(
                        onClick = { remote.sendKey(AndroidTvRemoteClient.KEY_MEDIA_REWIND) },
                        icon = Icons.Rounded.FastRewind,
                        hint = "Rewind",
                        size = 50.dp,
                        shape = CircleShape,
                        container = Color.Transparent,
                        contentColor = palette.textMuted,
                        iconSize = 26.dp,
                    )
                    RemoteKey(
                        onClick = {
                            playing = !playing
                            remote.sendKey(AndroidTvRemoteClient.KEY_MEDIA_PLAY_PAUSE)
                        },
                        icon = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        hint = if (playing) "Pause" else "Play",
                        size = 72.dp,
                        shape = CircleShape,
                        container = palette.warm,
                        contentColor = palette.onWarm,
                        iconSize = 34.dp,
                    )
                    RemoteKey(
                        onClick = { remote.sendKey(AndroidTvRemoteClient.KEY_MEDIA_FAST_FORWARD) },
                        icon = Icons.Rounded.FastForward,
                        hint = "Fast forward",
                        size = 50.dp,
                        shape = CircleShape,
                        container = Color.Transparent,
                        contentColor = palette.textMuted,
                        iconSize = 26.dp,
                    )
                    RemoteKey(
                        onClick = { remote.sendKey(AndroidTvRemoteClient.KEY_MEDIA_NEXT) },
                        icon = Icons.Rounded.SkipNext,
                        hint = "Next track",
                        size = 50.dp,
                        shape = CircleShape,
                        container = Color.Transparent,
                        contentColor = palette.textMuted,
                        iconSize = 26.dp,
                    )
                }
            }

            Spacer(Modifier.height(RemoteSpacing.sm))
        }
    }
}

/** Volume as one segmented rocker rather than three loose buttons. */
@Composable
private fun VolumeBar(remote: AndroidTvRemoteClient) {
    val palette = LocalRemotePalette.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(palette.surfaceHigh),
    ) {
        Row(Modifier.fillMaxSize()) {
            RemoteKey(
                onClick = { remote.sendKey(AndroidTvRemoteClient.KEY_VOLUME_DOWN) },
                icon = Icons.Rounded.VolumeDown,
                hint = "Volume down",
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RectangleShape,
                container = Color.Transparent,
                contentColor = palette.onLiveContainer,
                repeatWhileHeld = true,
                iconSize = 28.dp,
            )
            Box(Modifier.fillMaxHeight().width(1.dp).background(palette.outlineVariant))
            RemoteKey(
                onClick = { remote.sendKey(AndroidTvRemoteClient.KEY_MUTE) },
                icon = Icons.Rounded.VolumeOff,
                hint = "Mute",
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RectangleShape,
                container = Color.Transparent,
                contentColor = palette.onLiveContainer,
                iconSize = 26.dp,
            )
            Box(Modifier.fillMaxHeight().width(1.dp).background(palette.outlineVariant))
            RemoteKey(
                onClick = { remote.sendKey(AndroidTvRemoteClient.KEY_VOLUME_UP) },
                icon = Icons.Rounded.VolumeUp,
                hint = "Volume up",
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RectangleShape,
                container = Color.Transparent,
                contentColor = palette.onLiveContainer,
                repeatWhileHeld = true,
                iconSize = 28.dp,
            )
        }
    }
}

/** Shown on the Remote tab before a TV is paired, so the tab is never a dead end. */
@Composable
internal fun NoConnectionState(
    status: String,
    onOpenDevices: () -> Unit,
) {
    val palette = LocalRemotePalette.current
    Column(
        modifier = Modifier.fillMaxSize().padding(RemoteSpacing.screen),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(112.dp).clip(CircleShape).background(palette.accentContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Tv,
                contentDescription = null,
                tint = palette.onAccentContainer,
                modifier = Modifier.size(50.dp),
            )
        }
        Spacer(Modifier.height(RemoteSpacing.xl))
        Text(
            text = "No TV connected",
            style = MaterialTheme.typography.headlineSmall,
            color = palette.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(RemoteSpacing.sm))
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(RemoteSpacing.xl))
        BroadButton(
            label = "Find your TV",
            onClick = onOpenDevices,
            icon = Icons.Rounded.Wifi,
            modifier = Modifier.fillMaxWidth(0.8f),
        )
    }
}
