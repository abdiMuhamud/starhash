package com.innovii.starhash.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovii.starhash.R
import com.innovii.starhash.core.Status

/** The colours of one appearance. Brand colours that never change are on [Sh] directly. */
data class Palette(
    val dark: Boolean,
    val background: Color,
    val card: Color,
    val inset: Color,
    val bar: Color,
    val hero: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val violetSoft: Color,
    val tealSoft: Color,
    val tealInk: Color,
    val amberSoft: Color,
    val amberInk: Color,
    val bubble: Color,
    val bubbleInk: Color,
    val pass: Color,
    val warn: Color,
    val fail: Color,
    val blocked: Color,
)

/** Deep navy, soft text: easy on the eyes in a long QA session. */
val DarkPalette = Palette(
    dark = true,
    background = Color(0xFF0E0C26),
    card = Color(0xFF19173A),
    inset = Color(0xFF221F4A),
    bar = Color(0xFF141233),
    hero = Color(0xFF231D66),
    ink = Color(0xFFE4E5F1),
    muted = Color(0xFF9497B4),
    line = Color(0xFF2C2A55),
    accent = Color(0xFFA99BFF),
    violetSoft = Color(0xFF2C2770),
    tealSoft = Color(0xFF0F3437),
    tealInk = Color(0xFF5BE3CD),
    amberSoft = Color(0xFF3A2C12),
    amberInk = Color(0xFFF2B54B),
    bubble = Color(0xFF2B2A33),
    bubbleInk = Color(0xFFE9E9EE),
    pass = Color(0xFF3CCB82),
    warn = Color(0xFFF2B54B),
    fail = Color(0xFFF26B5E),
    blocked = Color(0xFF9AA0B6),
)

/** Light, but grey-blue rather than white. */
val LightPalette = Palette(
    dark = false,
    background = Color(0xFFE6E9F1),
    card = Color(0xFFF4F5FA),
    inset = Color(0xFFE6E9F1),
    bar = Color(0xFFF0F2F7),
    hero = Color(0xFF13104A),
    ink = Color(0xFF1B1F2E),
    muted = Color(0xFF5E6378),
    line = Color(0xFFD5D9E4),
    accent = Color(0xFF4F3BDB),
    violetSoft = Color(0xFFE1DCFA),
    tealSoft = Color(0xFFD2EFEA),
    tealInk = Color(0xFF047857),
    amberSoft = Color(0xFFF8E7CC),
    amberInk = Color(0xFFB45309),
    bubble = Color(0xFF2A2A2E),
    bubbleInk = Color(0xFFEDEDED),
    pass = Color(0xFF0F9D58),
    warn = Color(0xFFD99400),
    fail = Color(0xFFD93025),
    blocked = Color(0xFF6B7280),
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }

object Sh {
    val Night = Color(0xFF13104A)
    val Violet = Color(0xFF4F3BDB)
    val Teal = Color(0xFF00C2A8)
    val Mono = FontFamily.Monospace

    private val p: Palette @Composable @ReadOnlyComposable get() = LocalPalette.current
    val Background: Color @Composable @ReadOnlyComposable get() = p.background
    val Card: Color @Composable @ReadOnlyComposable get() = p.card
    val Inset: Color @Composable @ReadOnlyComposable get() = p.inset
    val Bar: Color @Composable @ReadOnlyComposable get() = p.bar
    val Hero: Color @Composable @ReadOnlyComposable get() = p.hero
    val Ink: Color @Composable @ReadOnlyComposable get() = p.ink
    val Muted: Color @Composable @ReadOnlyComposable get() = p.muted
    val Line: Color @Composable @ReadOnlyComposable get() = p.line
    val Accent: Color @Composable @ReadOnlyComposable get() = p.accent
    val VioletSoft: Color @Composable @ReadOnlyComposable get() = p.violetSoft
    val TealSoft: Color @Composable @ReadOnlyComposable get() = p.tealSoft
    val TealInk: Color @Composable @ReadOnlyComposable get() = p.tealInk
    val AmberSoft: Color @Composable @ReadOnlyComposable get() = p.amberSoft
    val AmberInk: Color @Composable @ReadOnlyComposable get() = p.amberInk
    val Bubble: Color @Composable @ReadOnlyComposable get() = p.bubble
    val BubbleInk: Color @Composable @ReadOnlyComposable get() = p.bubbleInk
    val Pass: Color @Composable @ReadOnlyComposable get() = p.pass
    val Warn: Color @Composable @ReadOnlyComposable get() = p.warn
    val Fail: Color @Composable @ReadOnlyComposable get() = p.fail
    val Blocked: Color @Composable @ReadOnlyComposable get() = p.blocked
}

private fun scheme(p: Palette) = if (p.dark) {
    darkColorScheme(
        primary = p.accent,
        onPrimary = Sh.Night,
        primaryContainer = p.violetSoft,
        onPrimaryContainer = p.ink,
        secondary = Sh.Teal,
        onSecondary = Sh.Night,
        secondaryContainer = p.tealSoft,
        onSecondaryContainer = p.ink,
        background = p.background,
        onBackground = p.ink,
        surface = p.card,
        onSurface = p.ink,
        surfaceVariant = p.inset,
        onSurfaceVariant = p.muted,
        surfaceContainer = p.bar,
        surfaceContainerHigh = p.inset,
        surfaceContainerHighest = p.inset,
        error = p.fail,
        outline = Color(0xFF4A4878),
        outlineVariant = p.line,
    )
} else {
    lightColorScheme(
        primary = Sh.Violet,
        onPrimary = Color.White,
        primaryContainer = p.violetSoft,
        onPrimaryContainer = Sh.Night,
        secondary = Sh.Teal,
        onSecondary = Color.White,
        secondaryContainer = p.tealSoft,
        onSecondaryContainer = Sh.Night,
        background = p.background,
        onBackground = p.ink,
        surface = p.card,
        onSurface = p.ink,
        surfaceVariant = p.inset,
        onSurfaceVariant = p.muted,
        surfaceContainer = p.bar,
        error = p.fail,
        outline = Color(0xFFB9BECC),
        outlineVariant = p.line,
    )
}

@Composable
fun StarHashTheme(dark: Boolean, content: @Composable () -> Unit) {
    val p = if (dark) DarkPalette else LightPalette
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme(p), content = content)
    }
}

@Composable
@ReadOnlyComposable
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
    Row(verticalAlignment = Alignment.Top) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Logo()
            Column(Modifier.padding(start = 12.dp)) {
                Text("StarHash", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Text(subtitle, color = Color.White.copy(alpha = 0.72f), fontSize = 13.sp)
            }
        }
        InnoviiLogo(Modifier.padding(start = 8.dp, top = 2.dp), height = 20.dp)
    }
}

/** The INNOVII logo, in white (for the navy header and the loading screen). */
@Composable
fun InnoviiLogo(modifier: Modifier = Modifier, height: Dp = 20.dp) {
    Image(
        painter = painterResource(R.drawable.innovii_white),
        contentDescription = "INNOVII",
        modifier = modifier.height(height),
    )
}
