package za.co.acidian.xiaomitvremote

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tv
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Device screen: the once-off setup. It leads with whatever is already paired and
 * only falls back to discovery and manual IP entry, so the common case ("reconnect
 * to the telly in the lounge") is a single tap rather than a search.
 */
@Composable
internal fun DevicesScreen(
    remote: AndroidTvRemoteClient,
    status: String,
    devices: List<AndroidTvRemoteClient.TvDevice>,
    connectedHost: String?,
    onOpenRemote: () -> Unit,
) {
    val palette = LocalRemotePalette.current
    var ip by rememberSaveable { mutableStateOf(remote.lastHost().orEmpty()) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = RemoteSpacing.screen,
            end = RemoteSpacing.screen,
            top = RemoteSpacing.lg,
            bottom = RemoteSpacing.xl,
        ),
        verticalArrangement = Arrangement.spacedBy(RemoteSpacing.md),
    ) {
        item {
            ScreenHeader(
                title = "Devices",
                subtitle = "Pair once. After that the remote reconnects on its own.",
            )
        }

        if (connectedHost != null) {
            item { PairedDeviceCard(host = connectedHost, onOpenRemote = onOpenRemote, onRepair = { remote.startPairing(connectedHost) }, onDisconnect = { remote.disconnect() }) }
        }

        item {
            Spacer(Modifier.height(RemoteSpacing.xs))
            BroadButton(
                label = if (devices.isEmpty()) "Find TVs on Wi-Fi" else "Search again",
                onClick = { remote.discover() },
                icon = Icons.Rounded.Wifi,
                container = if (connectedHost != null) palette.surfaceHigh else palette.accent,
                contentColor = if (connectedHost != null) palette.textPrimary else palette.onAccent,
            )
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RemoteSpacing.sm),
            ) {
                StatusDot(if (connectedHost != null) palette.live else palette.textFaint)
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (connectedHost != null) palette.textPrimary else palette.textMuted,
                )
            }
            if (devices.isEmpty() && connectedHost == null) {
                Spacer(Modifier.height(RemoteSpacing.sm))
                Text(
                    text = "Phone and TV need to be on the same Wi-Fi. If the TV is on Ethernet, its IP is in the TV's network settings.",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textFaint,
                )
            }
        }

        if (devices.isNotEmpty()) {
            item { SectionLabel("On your network") }
            items(devices, key = { it.host }) { device ->
                DeviceRow(
                    name = device.name,
                    host = device.host,
                    isConnected = device.host == connectedHost,
                    onConnect = { remote.connect(device.host) },
                    onPair = { remote.startPairing(device.host) },
                )
            }
        }

        item {
            Spacer(Modifier.height(RemoteSpacing.sm))
            SectionLabel("Not showing up?")
            Spacer(Modifier.height(RemoteSpacing.sm))
            RemoteTextField(
                value = ip,
                onValueChange = { ip = it },
                label = "TV IP address",
                placeholder = "192.168.1.42",
                keyboardType = KeyboardType.Uri,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RemoteSpacing.sm),
            ) {
                RemoteKey(
                    onClick = { remote.connect(ip.trim()) },
                    label = "Connect",
                    modifier = Modifier.weight(1f).height(52.dp),
                    container = palette.surfaceHigh,
                    contentColor = palette.textPrimary,
                    enabled = ip.isNotBlank(),
                )
                RemoteKey(
                    onClick = { remote.startPairing(ip.trim()) },
                    label = "Pair this IP",
                    modifier = Modifier.weight(1f).height(52.dp),
                    container = palette.accentContainer,
                    contentColor = palette.onAccentContainer,
                    enabled = ip.isNotBlank(),
                )
            }
        }
    }
}

@Composable
private fun PairedDeviceCard(
    host: String,
    onOpenRemote: () -> Unit,
    onRepair: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val palette = LocalRemotePalette.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(palette.surface)
            .padding(RemoteSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RemoteSpacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(palette.liveContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = palette.onLiveContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(RemoteSpacing.md))
            Column(Modifier.weight(1f)) {
                Text("Paired", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                Text(
                    text = host,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(RemoteSpacing.sm)) {
            RemoteKey(
                onClick = onRepair,
                icon = Icons.Rounded.Refresh,
                label = "Re-pair",
                modifier = Modifier.weight(1f).height(48.dp),
                container = palette.surfaceHigh,
                contentColor = palette.textPrimary,
                iconSize = 18.dp,
                fontSize = 13.sp,
            )
            RemoteKey(
                onClick = onDisconnect,
                label = "Disconnect",
                modifier = Modifier.weight(1f).height(48.dp),
                container = palette.surfaceHigh,
                contentColor = palette.textMuted,
                fontSize = 13.sp,
            )
        }

        Text(
            text = "Paired to this phone. Re-pair only if the TV forgets it or you reset the TV's remote settings.",
            style = MaterialTheme.typography.labelMedium,
            color = palette.textMuted,
        )

        BroadButton(label = "Open the remote", onClick = onOpenRemote, icon = Icons.Rounded.Tv)
    }
}

@Composable
private fun DeviceRow(
    name: String,
    host: String,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onPair: () -> Unit,
) {
    val palette = LocalRemotePalette.current
    val tone = if (isConnected) palette.live else palette.textFaint

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(palette.surface)
            .clickable(onClick = onConnect)
            .padding(horizontal = RemoteSpacing.lg, vertical = RemoteSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Tv,
            contentDescription = null,
            tint = tone,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(RemoteSpacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(host, style = MaterialTheme.typography.labelMedium, color = palette.textMuted)
        }
        RemoteKey(
            onClick = onPair,
            label = "Pair",
            container = palette.accentContainer,
            contentColor = palette.onAccentContainer,
            fontSize = 13.sp,
            weight = FontWeight.SemiBold,
            modifier = Modifier.height(44.dp),
        )
    }
}
