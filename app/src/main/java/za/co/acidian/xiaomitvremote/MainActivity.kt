package za.co.acidian.xiaomitvremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { XiaomiRemoteApp() }
    }
}

private val Bg = Color(0xFF111318)
private val Panel = Color(0xFF1B1E25)
private val Accent = Color(0xFF7DA7FF)

@Composable
fun XiaomiRemoteApp() {
    var status by remember { mutableStateOf("Not connected") }
    MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Bg, surface = Panel)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Xiaomi TV Remote", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(status, color = Color.LightGray)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { status = "Discovery coming next" }, modifier = Modifier.fillMaxWidth()) {
                    Text("Find TV on Wi-Fi")
                }
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    RemoteButton("Power")
                    RemoteButton("Home")
                    RemoteButton("Back")
                }
                Spacer(Modifier.height(22.dp))
                DPad()
                Spacer(Modifier.height(22.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    RemoteButton("Vol −")
                    RemoteButton("Mute")
                    RemoteButton("Vol +")
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    RemoteButton("⏪")
                    RemoteButton("▶ / ❚❚")
                    RemoteButton("⏩")
                }
                Spacer(Modifier.height(22.dp))
                OutlinedButton(onClick = { status = "Keyboard input coming next" }, modifier = Modifier.fillMaxWidth()) {
                    Text("⌨  Send text to TV")
                }
            }
        }
    }
}

@Composable
private fun RemoteButton(label: String) {
    Button(
        onClick = { },
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Panel),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
    ) { Text(label) }
}

@Composable
private fun DPad() {
    Box(
        modifier = Modifier.size(230.dp).background(Panel, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = {}, modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)) { Text("▲") }
        Button(onClick = {}, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)) { Text("▼") }
        Button(onClick = {}, modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp)) { Text("◀") }
        Button(onClick = {}, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)) { Text("▶") }
        Button(onClick = {}, modifier = Modifier.size(78.dp), shape = CircleShape) { Text("OK", fontWeight = FontWeight.Bold) }
    }
}
