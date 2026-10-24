package za.co.acidian.xiaomitvremote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Full-width primary action. One per screen, so it always reads as *the* next step. */
@Composable
internal fun BroadButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    container: Color = LocalRemotePalette.current.accent,
    contentColor: Color = LocalRemotePalette.current.onAccent,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = contentColor,
            disabledContainerColor = LocalRemotePalette.current.surfaceHigh,
            disabledContentColor = LocalRemotePalette.current.textFaint,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Text(label, style = MaterialTheme.typography.labelLarge, fontSize = 15.sp)
        }
    }
}

/**
 * Text field styled from the palette. M3's defaults would drift toward the stock
 * purple-grey scheme, and the outline is the strongest affordance a field has, so
 * it's driven by the accent when focused and the quiet outline when it isn't.
 */
@Composable
internal fun RemoteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    leading: @Composable (() -> Unit)? = null,
) {
    val palette = LocalRemotePalette.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it, color = palette.textFaint) } },
        singleLine = singleLine,
        shape = RoundedCornerShape(18.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.textPrimary),
        trailingIcon = trailing,
        leadingIcon = leading,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = palette.textPrimary,
            unfocusedTextColor = palette.textPrimary,
            focusedContainerColor = palette.surface,
            unfocusedContainerColor = palette.surface,
            focusedBorderColor = palette.accent,
            unfocusedBorderColor = palette.outlineVariant,
            focusedLabelColor = palette.accent,
            unfocusedLabelColor = palette.textMuted,
            cursorColor = palette.accent,
        ),
    )
}

/** Compact secondary action, e.g. one app out of a launcher grid. */
@Composable
internal fun ChipButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    container: Color = LocalRemotePalette.current.surfaceHigh,
    contentColor: Color = LocalRemotePalette.current.textPrimary,
) {
    RemoteKey(
        onClick = onClick,
        label = label,
        icon = icon,
        modifier = modifier,
        container = container,
        contentColor = contentColor,
        shape = RoundedCornerShape(16.dp),
        iconSize = 18.dp,
        fontSize = 13.sp,
        weight = FontWeight.SemiBold,
    )
}
