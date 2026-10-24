package za.co.acidian.xiaomitvremote

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Launch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private data class LaunchableApp(val label: String, val link: String)

private val QuickApps = listOf(
    LaunchableApp("YouTube", "https://www.youtube.com"),
    LaunchableApp("Netflix", "netflix://"),
    LaunchableApp("Prime Video", "https://app.primevideo.com"),
    LaunchableApp("Disney+", "https://www.disneyplus.com"),
    LaunchableApp("Spotify", "spotify://"),
    LaunchableApp("Plex", "plex://"),
)

/**
 * App launcher. The protocol takes a URI and lets the TV resolve it, so the quick
 * list is really a set of well-known deep links rather than anything the app does
 * itself; the free-text field is there for everything not on the list.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppsScreen(
    remote: AndroidTvRemoteClient,
    connected: Boolean,
) {
    val palette = LocalRemotePalette.current
    var link by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RemoteSpacing.screen, vertical = RemoteSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RemoteSpacing.md),
    ) {
        ScreenHeader(
            title = "Apps",
            subtitle = "Skip the TV's home screen and open something directly.",
        )

        if (!connected) {
            Text(
                text = "Not connected. Open Devices to pick a TV first.",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.warmContainer)
                    .padding(horizontal = RemoteSpacing.md, vertical = RemoteSpacing.sm),
                style = MaterialTheme.typography.labelMedium,
                color = palette.onWarmContainer,
            )
        }

        Spacer(Modifier.height(RemoteSpacing.xs))
        SectionLabel("One tap")

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RemoteSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(RemoteSpacing.sm),
        ) {
            val tones = listOf(
                palette.accentContainer to palette.onAccentContainer,
                palette.liveContainer to palette.onLiveContainer,
                palette.warmContainer to palette.onWarmContainer,
                palette.dangerContainer to palette.onDangerContainer,
            )
            QuickApps.forEachIndexed { index, app ->
                val (container, content) = tones[index % tones.size]
                ChipButton(
                    label = app.label,
                    onClick = {
                        link = app.link
                        remote.launchAppLink(app.link)
                    },
                    container = container,
                    contentColor = content,
                )
            }
        }

        Spacer(Modifier.height(RemoteSpacing.md))
        SectionLabel("Anything else")

        RemoteTextField(
            value = link,
            onValueChange = { link = it },
            label = "App link or deep link",
            placeholder = "youtube:// or https://…",
            keyboardType = KeyboardType.Uri,
            singleLine = true,
        )

        BroadButton(
            label = "Launch on TV",
            onClick = { remote.launchAppLink(link.trim()) },
            icon = Icons.Rounded.Launch,
            enabled = connected && link.isNotBlank(),
        )

        Spacer(Modifier.height(RemoteSpacing.sm))
        Text(
            text = "Works when the app is installed on the TV and accepts links. If nothing happens, the app may only launch from the TV's own home screen.",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textFaint,
        )
        Spacer(Modifier.height(RemoteSpacing.md))
    }
}
