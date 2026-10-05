package com.innovii.starhash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovii.starhash.core.Status

object Sh {
    val Night = Color(0xFF13104A)
    val Violet = Color(0xFF4F3BDB)
    val VioletSoft = Color(0xFFECE9FF)
    val Teal = Color(0xFF00C2A8)
    val TealSoft = Color(0xFFDDF8F3)
    val Background = Color(0xFFF4F6FB)
    val Ink = Color(0xFF111827)
    val Muted = Color(0xFF6B7280)
    val Line = Color(0xFFE5E7EB)
    val Pass = Color(0xFF0F9D58)
    val Warn = Color(0xFFE8A100)
    val Fail = Color(0xFFD93025)
    val Blocked = Color(0xFF6B7280)
    val Mono = FontFamily.Monospace
}

private val colors = lightColorScheme(
    primary = Sh.Violet,
    onPrimary = Color.White,
    primaryContainer = Sh.VioletSoft,
    onPrimaryContainer = Sh.Night,
    secondary = Sh.Teal,
    onSecondary = Color.White,
    secondaryContainer = Sh.TealSoft,
    onSecondaryContainer = Sh.Night,
    background = Sh.Background,
    onBackground = Sh.Ink,
    surface = Color.White,
    onSurface = Sh.Ink,
    surfaceVariant = Color(0xFFEFF1F7),
    onSurfaceVariant = Sh.Muted,
    error = Sh.Fail,
    outline = Color(0xFFD1D5DB),
    outlineVariant = Sh.Line,
)

@Composable
fun StarHashTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}

fun statusColor(s: Status): Color = when (s) {
    Status.PASS -> Sh.Pass
    Status.WARN -> Sh.Warn
    Status.FAIL -> Sh.Fail
    Status.BLOCKED -> Sh.Blocked
}

fun statusIcon(s: Status): ImageVector = when (s) {
    Status.PASS -> Icons.Filled.CheckCircle
    Status.WARN -> Icons.Filled.Warning
    Status.FAIL -> Icons.Filled.Close
    Status.BLOCKED -> Icons.Filled.Lock
}

@Composable
fun StatusIcon(s: Status, size: Dp = 28.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(statusColor(s).copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(statusIcon(s), contentDescription = s.label, tint = statusColor(s), modifier = Modifier.size(size * 0.62f))
    }
}

@Composable
fun StatusPill(s: Status, text: String = s.label.uppercase()) {
    Text(
        text,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(statusColor(s)).padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** The StarHash mark: a teal star and a white hash on night blue. */
@Composable
fun Logo(size: Dp = 44.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.28f)).background(Sh.Night),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy((-1).dp), verticalAlignment = Alignment.CenterVertically) {
            Text("*", color = Sh.Teal, fontWeight = FontWeight.Black, fontSize = (size.value * 0.55f).sp)
            Text("#", color = Color.White, fontWeight = FontWeight.Black, fontSize = (size.value * 0.42f).sp)
        }
    }
}

@Composable
fun Brand(subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Logo()
        Column(Modifier.padding(start = 12.dp)) {
            Text("StarHash", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            Text(subtitle, color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
        }
    }
}
