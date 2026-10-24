package za.co.acidian.xiaomitvremote

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardReturn
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Typing screen. On a phone keyboard the thing people get wrong is expecting the
 * text to stream character by character; this sends the whole buffer on Send (the
 * protocol has a batch edit message), and keeps Enter and Backspace as separate
 * keys for the cases where the TV field needs them.
 */
@Composable
internal fun TypeScreen(
    remote: AndroidTvRemoteClient,
    connected: Boolean,
) {
    val palette = LocalRemotePalette.current
    var text by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = RemoteSpacing.screen, vertical = RemoteSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RemoteSpacing.md),
    ) {
        ScreenHeader(
            title = "Type on TV",
            subtitle = "Sends into whatever field is open on the TV: search boxes, logins, browsers.",
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

        RemoteTextField(
            value = text,
            onValueChange = { text = it },
            label = "Message",
            modifier = Modifier.weight(1f),
            singleLine = false,
            placeholder = "Type here, then send",
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RemoteSpacing.sm),
        ) {
            RemoteKey(
                onClick = { text = "" },
                icon = Icons.Rounded.Close,
                label = "Clear",
                hint = "Clear the message",
                modifier = Modifier.weight(1f).height(52.dp),
                container = palette.surfaceHigh,
                contentColor = palette.textMuted,
                iconSize = 17.dp,
                fontSize = 13.sp,
                enabled = text.isNotEmpty(),
            )
            RemoteKey(
                onClick = { remote.sendKey(AndroidTvRemoteClient.KEY_DEL) },
                icon = Icons.Rounded.Backspace,
                label = "Delete",
                hint = "Backspace on the TV",
                modifier = Modifier.weight(1f).height(52.dp),
                container = palette.surfaceHigh,
                contentColor = palette.textPrimary,
                iconSize = 17.dp,
                fontSize = 13.sp,
                enabled = connected,
            )
            RemoteKey(
                onClick = { remote.sendKey(AndroidTvRemoteClient.KEY_ENTER) },
                icon = Icons.Rounded.KeyboardReturn,
                label = "Enter",
                hint = "Enter on the TV",
                modifier = Modifier.weight(1f).height(52.dp),
                container = palette.surfaceHigh,
                contentColor = palette.textPrimary,
                iconSize = 17.dp,
                fontSize = 13.sp,
                enabled = connected,
            )
        }

        BroadButton(
            label = "Send to TV",
            onClick = {
                remote.sendText(text)
                text = ""
            },
            icon = Icons.Rounded.Send,
            enabled = connected && text.isNotEmpty(),
        )

        Spacer(Modifier.height(RemoteSpacing.xxs))
        Text(
            text = "Tip: hold Backspace or the D-pad arrows to repeat.",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textFaint,
        )
    }
}
