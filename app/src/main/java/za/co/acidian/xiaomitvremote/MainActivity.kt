package za.co.acidian.xiaomitvremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), AndroidTvRemoteClient.Listener {
    private lateinit var remote: AndroidTvRemoteClient
    private val uiStatus = mutableStateOf("Not connected")
    private val uiDevices = mutableStateOf<List<AndroidTvRemoteClient.TvDevice>>(emptyList())
    private val uiPairHost = mutableStateOf<String?>(null)
    private val uiConnectedHost = mutableStateOf<String?>(null)
    private val uiPower = mutableStateOf<Boolean?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        remote = AndroidTvRemoteClient(this, this)
        setContent {
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
        remote.lastHost()?.let { remote.connect(it) }
    }

    override fun onDestroy() {
        remote.disconnect()
        super.onDestroy()
    }

    override fun onStatus(status: String) { uiStatus.value = status }
    override fun onDevices(devices: List<AndroidTvRemoteClient.TvDevice>) { uiDevices.value = devices }
    override fun onPairingCodeRequested(host: String) { uiPairHost.value = host }
    override fun onConnected(host: String) {
        uiConnectedHost.value = host
        uiPairHost.value = null
    }
    override fun onDisconnected(reason: String) { uiConnectedHost.value = null }
    override fun onPowerChanged(on: Boolean) { uiPower.value = on }
}

/**
 * Top-level router. Daily use means: if we're already connected to a TV, open straight
 * into the remote, no discovery/connect UI in the way. [manualSetupOpen] lets the person
 * deliberately step back to the connect screen (e.g. to switch TVs or re-pair) without
 * tearing down the current connection; it's reset automatically the moment [connectedHost]
 * changes (a new connection succeeds, or the current one drops), so the person always
 * lands back on the remote as soon as there's something to control.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    var ip by remember { mutableStateOf(remote.lastHost().orEmpty()) }
    var text by remember { mutableStateOf("") }
    var appLink by remember { mutableStateOf("") }
    var showMoreSheet by rememberSaveable { mutableStateOf(false) }
    var manualSetupOpen by remember { mutableStateOf(false) }

    LaunchedEffect(connectedHost) {
        if (connectedHost != null) manualSetupOpen = false
    }

    XiaomiRemoteTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (connectedHost != null && !manualSetupOpen) {
                RemoteControlScreen(
                    remote = remote,
                    status = status,
                    connectedHost = connectedHost,
                    power = power,
                    text = text,
                    onTextChange = { text = it },
                    appLink = appLink,
                    onAppLinkChange = { appLink = it },
                    showMoreSheet = showMoreSheet,
                    onOpenMoreSheet = { showMoreSheet = true },
                    onCloseMoreSheet = { showMoreSheet = false },
                    onSwitchDevice = { manualSetupOpen = true },
                )
            } else {
                ConnectScreen(
                    remote = remote,
                    status = status,
                    devices = devices,
                    ip = ip,
                    onIpChange = { ip = it },
                    onDeviceChosen = { host -> ip = host },
                    canReturnToRemote = connectedHost != null,
                    onReturnToRemote = { manualSetupOpen = false },
                )
            }
        }

        pairHost?.let { host ->
            PairingDialog(
                host = host,
                onPair = { code -> remote.finishPairing(code) },
                onDismiss = onDismissPair,
            )
        }
    }
}

@Composable
private fun ConnectScreen(
    remote: AndroidTvRemoteClient,
    status: String,
    devices: List<AndroidTvRemoteClient.TvDevice>,
    ip: String,
    onIpChange: (String) -> Unit,
    onDeviceChosen: (String) -> Unit,
    canReturnToRemote: Boolean,
    onReturnToRemote: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(RemoteSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RemoteSpacing.md),
    ) {
        item {
            if (canReturnToRemote) {
                TextButton(onClick = onReturnToRemote, modifier = Modifier.fillMaxWidth()) {
                    Text("← Back to remote control")
                }
            }
            Text("Xiaomi TV Remote", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(RemoteSpacing.xs))
            Text(status, color = RemoteColors.TextSecondary)
        }

        item {
            Button(onClick = { remote.discover() }, modifier = Modifier.fillMaxWidth()) { Text("Find TV") }
        }

        if (devices.isNotEmpty()) {
            item { Text("Found on network", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold) }
            items(devices) { device ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        onDeviceChosen(device.host)
                        remote.connect(device.host)
                    }
                ) {
                    Row(Modifier.fillMaxWidth().padding(RemoteSpacing.md), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(device.name, fontWeight = FontWeight.SemiBold)
                            Text(device.host, color = RemoteColors.TextMuted, fontSize = 12.sp)
                        }
                        TextButton(onClick = { remote.startPairing(device.host) }) { Text("Pair") }
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RemoteSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = ip,
                    onValueChange = onIpChange,
                    label = { Text("TV IP address") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                Button(onClick = { remote.connect(ip.trim()) }) { Text("Connect") }
            }
            TextButton(onClick = { remote.startPairing(ip.trim()) }, enabled = ip.isNotBlank()) { Text("Pair this IP") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoteControlScreen(
    remote: AndroidTvRemoteClient,
    status: String,
    connectedHost: String,
    power: Boolean?,
    text: String,
    onTextChange: (String) -> Unit,
    appLink: String,
    onAppLinkChange: (String) -> Unit,
    showMoreSheet: Boolean,
    onOpenMoreSheet: () -> Unit,
    onCloseMoreSheet: () -> Unit,
    onSwitchDevice: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(RemoteSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RemoteSpacing.md),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(connectedHost, fontWeight = FontWeight.SemiBold)
                    Text(status, color = RemoteColors.Connected, fontSize = 12.sp)
                    if (power != null) Text(if (power) "TV awake" else "TV sleeping", color = RemoteColors.TextMuted, fontSize = 12.sp)
                }
                Row {
                    TextButton(onClick = { remote.startPairing(connectedHost) }) { Text("Re-pair") }
                    TextButton(onClick = onSwitchDevice) { Text("Switch") }
                }
            }
        }

        // Primary cluster, arranged like a physical remote: power isolated and
        // accent-colored at top (it's the one button with a materially different
        // consequence), back/home as a pair below it, D-pad beneath that.
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(RemoteSpacing.sm),
            ) {
                RemoteButton(
                    label = "⏻",
                    accessibilityLabel = "Power",
                    onClick = { remote.sendKey(AndroidTvRemoteClient.KEY_POWER) },
                    shape = CircleShape,
                    fixedSize = 64.dp,
                    containerColor = MaterialTheme.colorScheme.primary,
                    fontSize = 22.sp,
                )
                Row(Modifier.fillMaxWidth(0.7f), horizontalArrangement = Arrangement.spacedBy(RemoteSpacing.sm)) {
                    RemoteButton("Back", { remote.sendKey(AndroidTvRemoteClient.KEY_BACK) }, modifier = Modifier.weight(1f))
                    RemoteButton("Home", { remote.sendKey(AndroidTvRemoteClient.KEY_HOME) }, modifier = Modifier.weight(1f))
                }
            }
        }

        item { DPad(remote) }

        // Secondary cluster: volume + media transport, grouped under one label so
        // the scroll reads as sections rather than an undifferentiated button soup.
        item {
            Text(
                "Playback",
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.SemiBold,
                color = RemoteColors.TextMuted,
                fontSize = 12.sp,
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                RemoteButton("Vol −", { remote.sendKey(AndroidTvRemoteClient.KEY_VOLUME_DOWN) }, accessibilityLabel = "Volume down")
                RemoteButton("Mute", { remote.sendKey(AndroidTvRemoteClient.KEY_MUTE) })
                RemoteButton("Vol +", { remote.sendKey(AndroidTvRemoteClient.KEY_VOLUME_UP) }, accessibilityLabel = "Volume up")
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                RemoteButton("⏮", { remote.sendKey(AndroidTvRemoteClient.KEY_MEDIA_PREVIOUS) }, accessibilityLabel = "Previous")
                RemoteButton("⏪", { remote.sendKey(AndroidTvRemoteClient.KEY_MEDIA_REWIND) }, accessibilityLabel = "Rewind")
                RemoteButton("▶ / ❚❚", { remote.sendKey(AndroidTvRemoteClient.KEY_MEDIA_PLAY_PAUSE) }, accessibilityLabel = "Play or pause")
                RemoteButton("⏩", { remote.sendKey(AndroidTvRemoteClient.KEY_MEDIA_FAST_FORWARD) }, accessibilityLabel = "Fast forward")
                RemoteButton("⏭", { remote.sendKey(AndroidTvRemoteClient.KEY_MEDIA_NEXT) }, accessibilityLabel = "Next")
            }
        }

        item {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                label = { Text("Type on TV") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(onClick = { remote.sendText(text); onTextChange("") }, enabled = text.isNotEmpty()) { Text("Send") }
                },
            )
        }

        item {
            OutlinedButton(onClick = onOpenMoreSheet, modifier = Modifier.fillMaxWidth()) {
                Text("More controls & app launcher")
            }
        }
    }

    if (showMoreSheet) {
        val sheetState = rememberModalBottomSheetState()
        val scope = rememberCoroutineScope()
        val dismiss = {
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) onCloseMoreSheet()
            }
        }
        ModalBottomSheet(onDismissRequest = onCloseMoreSheet, sheetState = sheetState) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = RemoteSpacing.lg, vertical = RemoteSpacing.sm)
                    .padding(bottom = RemoteSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(RemoteSpacing.md),
            ) {
                Text("More controls", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    RemoteButton("Menu") { remote.sendKey(AndroidTvRemoteClient.KEY_MENU) }
                    RemoteButton("Search") { remote.sendKey(AndroidTvRemoteClient.KEY_SEARCH) }
                    RemoteButton("Settings") { remote.sendKey(AndroidTvRemoteClient.KEY_SETTINGS) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    RemoteButton("Sleep") { remote.sendKey(AndroidTvRemoteClient.KEY_SLEEP) }
                    RemoteButton("Wake") { remote.sendKey(AndroidTvRemoteClient.KEY_WAKEUP) }
                }
                OutlinedTextField(
                    value = appLink,
                    onValueChange = onAppLinkChange,
                    label = { Text("App/deep link (e.g. youtube://)") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(
                            onClick = {
                                remote.launchAppLink(appLink)
                                dismiss()
                            },
                            enabled = appLink.isNotBlank(),
                        ) { Text("Launch") }
                    },
                )
            }
        }
    }
}

@Composable
private fun PairingDialog(host: String, onPair: (String) -> Unit, onDismiss: () -> Unit) {
    var code by remember(host) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pair with Android TV") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Enter the 6-character hexadecimal code shown on the TV at $host.")
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().filter { c -> c in "0123456789ABCDEF" }.take(6) },
                    label = { Text("Pairing code") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onPair(code) }, enabled = code.length == 6) { Text("Pair") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * Shared button for every remote command: press-scale animation, a haptic tick on tap,
 * and an optional [accessibilityLabel] that fully replaces what TalkBack announces
 * (via `clearAndSetSemantics`, not a merge) for buttons whose visible glyph isn't
 * itself a meaningful spoken label (▲, ⏪, ⏻, ...).
 *
 * Note on touch targets: Material3's Button already reserves a 48dp x 48dp minimum
 * touch target automatically (`LocalMinimumInteractiveComponentSize`), even when the
 * visual size set here is smaller. No manual padding is needed for that; [fixedSize]
 * below is purely a visual choice for the D-pad/power circles.
 */
@Composable
private fun RemoteButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accessibilityLabel: String? = null,
    shape: Shape = RoundedCornerShape(18.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    fixedSize: Dp? = null,
    fontSize: TextUnit = 13.sp,
    fontWeight: FontWeight = FontWeight.Normal,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1f, label = "remote-button-scale")
    val haptics = LocalHapticFeedback.current

    var buttonModifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }
    if (fixedSize != null) buttonModifier = buttonModifier.size(fixedSize)
    if (accessibilityLabel != null) {
        buttonModifier = buttonModifier.clearAndSetSemantics { contentDescription = accessibilityLabel }
    }

    Button(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onClick()
        },
        modifier = buttonModifier,
        interactionSource = interactionSource,
        shape = shape,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        contentPadding = if (fixedSize != null) PaddingValues(0.dp) else PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) { Text(label, fontSize = fontSize, fontWeight = fontWeight) }
}

@Composable
private fun DPad(remote: AndroidTvRemoteClient) {
    Box(
        modifier = Modifier.size(230.dp).background(MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        RemoteButton(
            label = "▲",
            accessibilityLabel = "Up",
            onClick = { remote.sendKey(AndroidTvRemoteClient.KEY_DPAD_UP) },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
            shape = CircleShape,
            fixedSize = 56.dp,
        )
        RemoteButton(
            label = "▼",
            accessibilityLabel = "Down",
            onClick = { remote.sendKey(AndroidTvRemoteClient.KEY_DPAD_DOWN) },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            shape = CircleShape,
            fixedSize = 56.dp,
        )
        RemoteButton(
            label = "◀",
            accessibilityLabel = "Left",
            onClick = { remote.sendKey(AndroidTvRemoteClient.KEY_DPAD_LEFT) },
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp),
            shape = CircleShape,
            fixedSize = 56.dp,
        )
        RemoteButton(
            label = "▶",
            accessibilityLabel = "Right",
            onClick = { remote.sendKey(AndroidTvRemoteClient.KEY_DPAD_RIGHT) },
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
            shape = CircleShape,
            fixedSize = 56.dp,
        )
        RemoteButton(
            label = "OK",
            accessibilityLabel = "Select",
            onClick = { remote.sendKey(AndroidTvRemoteClient.KEY_DPAD_CENTER) },
            shape = CircleShape,
            fixedSize = 78.dp,
            fontWeight = FontWeight.Bold,
        )
    }
}
