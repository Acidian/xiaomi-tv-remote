package za.co.acidian.xiaomitvremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity(), AndroidTvRemoteClient.Listener {
    private lateinit var remote: AndroidTvRemoteClient
    private val uiStatus = mutableStateOf("Not connected")
    private val uiDevices = mutableStateOf<List<AndroidTvRemoteClient.TvDevice>>(emptyList())
    private val uiPairHost = mutableStateOf<String?>(null)
    private val uiConnectedHost = mutableStateOf<String?>(null)
    private val uiPower = mutableStateOf<Boolean?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        remote = AndroidTvRemoteClient(this, this)
        setContent {
            XiaomiRemoteTheme {
                XiaomiRemoteApp(
                    remote = remote,
                    status = uiStatus.value,
                    devices = uiDevices.value,
                    pairHost = uiPairHost.value,
                    connectedHost = uiConnectedHost.value,
                    power = uiPower.value,
                    onDismissPair = { uiPairHost.value = null },
                )
            }
        }
        remote.lastHost()?.let { remote.connect(it) }
    }

    override fun onDestroy() {
        remote.disconnect()
        super.onDestroy()
    }

    override fun onStatus(status: String) {
        uiStatus.value = status
    }

    override fun onDevices(devices: List<AndroidTvRemoteClient.TvDevice>) {
        uiDevices.value = devices
    }

    override fun onPairingCodeRequested(host: String) {
        uiPairHost.value = host
    }

    override fun onConnected(host: String) {
        uiConnectedHost.value = host
        uiPairHost.value = null
    }

    override fun onDisconnected(reason: String) {
        uiConnectedHost.value = null
    }

    override fun onPowerChanged(on: Boolean) {
        uiPower.value = on
    }
}

private data class TabSpec(val label: String, val icon: ImageVector)

private val Tabs = listOf(
    TabSpec("Remote", Icons.Rounded.Tv),
    TabSpec("Type", Icons.Rounded.Keyboard),
    TabSpec("Apps", Icons.Rounded.Apps),
    TabSpec("Devices", Icons.Rounded.Devices),
)

/**
 * App shell. Four destinations, each doing one job: the day-to-day remote, typing,
 * launching apps, and the once-off pairing management. The Remote tab is always
 * one tap away, and a new connection always lands back on it rather than leaving
 * the person on a setup screen.
 */
@Composable
fun XiaomiRemoteApp(
    remote: AndroidTvRemoteClient,
    status: String,
    devices: List<AndroidTvRemoteClient.TvDevice>,
    pairHost: String?,
    connectedHost: String?,
    power: Boolean?,
    onDismissPair: () -> Unit,
) {
    val palette = LocalRemotePalette.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var systemKeysOpen by rememberSaveable { mutableStateOf(false) }
    val connected = connectedHost != null
    val deviceLabel = devices.firstOrNull { it.host == connectedHost }?.name ?: connectedHost.orEmpty()

    LaunchedEffect(connectedHost) {
        if (connectedHost != null) tab = 0
    }

    Scaffold(
        containerColor = palette.background,
        bottomBar = {
            NavigationBar(containerColor = palette.surface, tonalElevation = 0.dp) {
                Tabs.forEachIndexed { index, spec ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(spec.icon, contentDescription = null) },
                        label = { Text(spec.label, style = MaterialTheme.typography.labelMedium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = palette.onAccent,
                            selectedTextColor = palette.textPrimary,
                            indicatorColor = palette.accent,
                            unselectedIconColor = palette.textFaint,
                            unselectedTextColor = palette.textFaint,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (tab) {
                0 -> if (connected) {
                    RemoteScreen(
                        remote = remote,
                        status = status,
                        deviceLabel = deviceLabel,
                        power = power,
                        onOpenSystemKeys = { systemKeysOpen = true },
                    )
                } else {
                    NoConnectionState(status = status, onOpenDevices = { tab = 3 })
                }

                1 -> TypeScreen(remote = remote, connected = connected)
                2 -> AppsScreen(remote = remote, connected = connected)
                else -> DevicesScreen(
                    remote = remote,
                    status = status,
                    devices = devices,
                    connectedHost = connectedHost,
                    onOpenRemote = { tab = 0 },
                )
            }
        }
    }

    if (systemKeysOpen) {
        SystemKeysSheet(
            onKey = { remote.sendKey(it) },
            onDismiss = { systemKeysOpen = false },
        )
    }

    pairHost?.let { host ->
        PairingSheet(
            host = host,
            onPair = { code -> remote.finishPairing(code) },
            onDismiss = onDismissPair,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SystemKeysSheet(
    onKey: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalRemotePalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = palette.outlineVariant) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RemoteSpacing.screen)
                .padding(bottom = RemoteSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(RemoteSpacing.sm),
        ) {
            Text("More keys", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
            Spacer(Modifier.height(RemoteSpacing.xs))

            Row(horizontalArrangement = Arrangement.spacedBy(RemoteSpacing.sm)) {
                SystemKey("Menu", Icons.Rounded.Menu, Modifier.weight(1f)) { onKey(AndroidTvRemoteClient.KEY_MENU) }
                SystemKey("Search", Icons.Rounded.Search, Modifier.weight(1f)) { onKey(AndroidTvRemoteClient.KEY_SEARCH) }
                SystemKey("Settings", Icons.Rounded.Settings, Modifier.weight(1f)) { onKey(AndroidTvRemoteClient.KEY_SETTINGS) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(RemoteSpacing.sm)) {
                SystemKey("Sleep", Icons.Rounded.Bedtime, Modifier.weight(1f)) { onKey(AndroidTvRemoteClient.KEY_SLEEP) }
                SystemKey("Wake", Icons.Rounded.WbSunny, Modifier.weight(1f)) { onKey(AndroidTvRemoteClient.KEY_WAKEUP) }
            }
        }
    }
}

@Composable
private fun SystemKey(
    label: String,
    icon: ImageVector,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    RemoteKey(
        onClick = onClick,
        icon = icon,
        label = label,
        modifier = modifier.height(58.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PairingSheet(
    host: String,
    onPair: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalRemotePalette.current
    var code by rememberSaveable(host) { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = palette.outlineVariant) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = RemoteSpacing.screen)
                .padding(bottom = RemoteSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(RemoteSpacing.md),
        ) {
            Text("Pair with your TV", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
            Text(
                text = "Your TV is showing a 6-character code. Enter it here to prove both devices are in the same room.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textMuted,
            )

            RemoteTextField(
                value = code,
                onValueChange = { input ->
                    code = input.uppercase().filter { it in "0123456789ABCDEF" }.take(6)
                },
                label = "Pairing code",
                placeholder = "A1B2C3",
                keyboardType = KeyboardType.Ascii,
            )

            Text(
                text = "$host · 0-9 and A-F only",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textFaint,
            )

            BroadButton(
                label = if (code.length == 6) "Pair" else "Pair (${code.length}/6)",
                onClick = { onPair(code) },
                enabled = code.length == 6,
            )

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", color = palette.textMuted)
            }
        }
    }
}
