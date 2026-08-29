package za.co.acidian.xiaomitvremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

private val Bg = Color(0xFF111318)
private val Panel = Color(0xFF1B1E25)
private val Accent = Color(0xFF7DA7FF)

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
    var showTools by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Bg, surface = Panel)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Text("Xiaomi TV Remote", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(status, color = if (connectedHost != null) Color(0xFF8CE99A) else Color.LightGray)
                    if (power != null) Text(if (power) "TV awake" else "TV sleeping", color = Color.Gray, fontSize = 12.sp)
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { remote.discover() }, modifier = Modifier.weight(1f)) { Text("Find TV") }
                        if (connectedHost != null) {
                            OutlinedButton(onClick = { remote.startPairing(connectedHost) }) { Text("Re-pair") }
                        }
                    }
                }

                if (devices.isNotEmpty()) {
                    item { Text("Found on network", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold) }
                    items(devices) { device ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                ip = device.host
                                remote.connect(device.host)
                            }
                        ) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(device.name, fontWeight = FontWeight.SemiBold)
                                    Text(device.host, color = Color.Gray, fontSize = 12.sp)
                                }
                                TextButton(onClick = { remote.startPairing(device.host) }) { Text("Pair") }
                            }
                        }
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = ip,
                            onValueChange = { ip = it },
                            label = { Text("TV IP address") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        )
                        Button(onClick = { remote.connect(ip.trim()) }) { Text("Connect") }
                    }
                    TextButton(onClick = { remote.startPairing(ip.trim()) }, enabled = ip.isNotBlank()) { Text("Pair this IP") }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        RemoteButton("Power") { remote.sendKey(AndroidTvRemoteClient.KEY_POWER) }
                        RemoteButton("Home") { remote.sendKey(AndroidTvRemoteClient.KEY_HOME) }
                        RemoteButton("Back") { remote.sendKey(AndroidTvRemoteClient.KEY_BACK) }
                    }
                }

                item { DPad(remote) }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        RemoteButton("Vol −") { remote.sendKey(AndroidTvRemoteClient.KEY_VOLUME_DOWN) }
                        RemoteButton("Mute") { remote.sendKey(AndroidTvRemoteClient.KEY_MUTE) }
                        RemoteButton("Vol +") { remote.sendKey(AndroidTvRemoteClient.KEY_VOLUME_UP) }
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        RemoteButton("⏮") { remote.sendKey(AndroidTvRemoteClient.KEY_MEDIA_PREVIOUS) }
                        RemoteButton("⏪") { remote.sendKey(AndroidTvRemoteClient.KEY_MEDIA_REWIND) }
                        RemoteButton("▶ / ❚❚") { remote.sendKey(AndroidTvRemoteClient.KEY_MEDIA_PLAY_PAUSE) }
                        RemoteButton("⏩") { remote.sendKey(AndroidTvRemoteClient.KEY_MEDIA_FAST_FORWARD) }
                        RemoteButton("⏭") { remote.sendKey(AndroidTvRemoteClient.KEY_MEDIA_NEXT) }
                    }
                }

                item {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Type on TV") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            TextButton(onClick = { remote.sendText(text); text = "" }, enabled = text.isNotEmpty()) { Text("Send") }
                        },
                    )
                }

                item {
                    OutlinedButton(onClick = { showTools = !showTools }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (showTools) "Hide extra controls" else "More controls & app launcher")
                    }
                }

                if (showTools) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            RemoteButton("Menu") { remote.sendKey(AndroidTvRemoteClient.KEY_MENU) }
                            RemoteButton("Search") { remote.sendKey(AndroidTvRemoteClient.KEY_SEARCH) }
                            RemoteButton("Settings") { remote.sendKey(AndroidTvRemoteClient.KEY_SETTINGS) }
                            RemoteButton("Sleep") { remote.sendKey(AndroidTvRemoteClient.KEY_SLEEP) }
                            RemoteButton("Wake") { remote.sendKey(AndroidTvRemoteClient.KEY_WAKEUP) }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = appLink,
                            onValueChange = { appLink = it },
                            label = { Text("App/deep link (e.g. youtube://)") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                TextButton(onClick = { remote.launchAppLink(appLink) }, enabled = appLink.isNotBlank()) { Text("Launch") }
                            },
                        )
                    }
                }
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

@Composable
private fun RemoteButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Panel),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) { Text(label, fontSize = 13.sp) }
}

@Composable
private fun DPad(remote: AndroidTvRemoteClient) {
    Box(
        modifier = Modifier.size(230.dp).background(Panel, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Button(onClick = { remote.sendKey(AndroidTvRemoteClient.KEY_DPAD_UP) }, modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)) { Text("▲") }
        Button(onClick = { remote.sendKey(AndroidTvRemoteClient.KEY_DPAD_DOWN) }, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)) { Text("▼") }
        Button(onClick = { remote.sendKey(AndroidTvRemoteClient.KEY_DPAD_LEFT) }, modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp)) { Text("◀") }
        Button(onClick = { remote.sendKey(AndroidTvRemoteClient.KEY_DPAD_RIGHT) }, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)) { Text("▶") }
        Button(onClick = { remote.sendKey(AndroidTvRemoteClient.KEY_DPAD_CENTER) }, modifier = Modifier.size(78.dp), shape = CircleShape) {
            Text("OK", fontWeight = FontWeight.Bold)
        }
    }
}
