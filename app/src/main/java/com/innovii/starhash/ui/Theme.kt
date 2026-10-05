package com.innovii.starhash.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.Brush
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

/**
 * StarHash colours: charcoal surfaces, a soft blue (#5B8DEF) for actions and selection, a mint green (#4CD08A)
 * for success and money, and the two together as a gradient for the main buttons.
 */
data class Palette(
    val dark: Boolean,
    val bg: Color,
    val surface: Color,
    val raised: Color,
    val bar: Color,
    val stroke: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val blue: Color,
    val green: Color,
    val amber: Color,
    val coral: Color,
    val grey: Color,
    val bubble: Color,
    val bubbleText: Color,
)

val DarkPalette = Palette(
    dark = true,
    bg = Color(0xFF111114),
    surface = Color(0xFF1A1B20),
    raised = Color(0xFF23252B),
    bar = Color(0xFF16171B),
    stroke = Color(0xFF2A2C33),
    text = Color(0xFFECEDF1),
    text2 = Color(0xFFA0A3AE),
    text3 = Color(0xFF6E717C),
    blue = Color(0xFF5B8DEF),
    green = Color(0xFF4CD08A),
    amber = Color(0xFFF2B84B),
    coral = Color(0xFFF0656B),
    grey = Color(0xFF8A8F9C),
    bubble = Color(0xFF23252B),
    bubbleText = Color(0xFFE6E7EC),
)

val LightPalette = Palette(
    dark = false,
    bg = Color(0xFFECEEF2),
    surface = Color(0xFFF6F7F9),
    raised = Color(0xFFE3E6EC),
    bar = Color(0xFFF1F2F5),
    stroke = Color(0xFFD6D9E0),
    text = Color(0xFF15161A),
    text2 = Color(0xFF5C606B),
    text3 = Color(0xFF8A8E99),
    blue = Color(0xFF3F74DE),
    green = Color(0xFF1FA463),
    amber = Color(0xFFC98A0C),
    coral = Color(0xFFD9474E),
    grey = Color(0xFF6B7080),
    bubble = Color(0xFF23252B),
    bubbleText = Color(0xFFE6E7EC),
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }

object Sh {
    val Blue = Color(0xFF5B8DEF)
    val Green = Color(0xFF4CD08A)
    val Charcoal = Color(0xFF161619)
    val Mono = FontFamily.Monospace

    /** Blue into green: the main buttons, the progress bars and the StarHash mark. */
    val Gradient: Brush get() = Brush.linearGradient(listOf(Blue, Green))

    private val p: Palette @Composable @ReadOnlyComposable get() = LocalPalette.current
    val Background: Color @Composable @ReadOnlyComposable get() = p.bg
    val Card: Color @Composable @ReadOnlyComposable get() = p.surface
    val Inset: Color @Composable @ReadOnlyComposable get() = p.raised
    val Bar: Color @Composable @ReadOnlyComposable get() = p.bar
    val Line: Color @Composable @ReadOnlyComposable get() = p.stroke
    val Ink: Color @Composable @ReadOnlyComposable get() = p.text
    val Muted: Color @Composable @ReadOnlyComposable get() = p.text2
    val Faint: Color @Composable @ReadOnlyComposable get() = p.text3
    val Accent: Color @Composable @ReadOnlyComposable get() = p.blue
    val Money: Color @Composable @ReadOnlyComposable get() = p.green
    val Amber: Color @Composable @ReadOnlyComposable get() = p.amber
    val Coral: Color @Composable @ReadOnlyComposable get() = p.coral
    val Bubble: Color @Composable @ReadOnlyComposable get() = p.bubble
    val BubbleInk: Color @Composable @ReadOnlyComposable get() = p.bubbleText
    val Pass: Color @Composable @ReadOnlyComposable get() = p.green
    val Warn: Color @Composable @ReadOnlyComposable get() = p.amber
    val Fail: Color @Composable @ReadOnlyComposable get() = p.coral
    val Blocked: Color @Composable @ReadOnlyComposable get() = p.grey
}

private fun scheme(p: Palette) = if (p.dark) {
    darkColorScheme(
        primary = p.blue,
        onPrimary = Color.White,
        primaryContainer = p.blue.copy(alpha = 0.18f),
        onPrimaryContainer = p.text,
        secondary = p.green,
        onSecondary = Sh.Charcoal,
        secondaryContainer = p.green.copy(alpha = 0.16f),
        onSecondaryContainer = p.text,
        background = p.bg,
        onBackground = p.text,
        surface = p.surface,
        onSurface = p.text,
        surfaceVariant = p.raised,
        onSurfaceVariant = p.text2,
        surfaceContainerLowest = p.bg,
        surfaceContainerLow = p.surface,
        surfaceContainer = p.surface,
        surfaceContainerHigh = p.raised,
        surfaceContainerHighest = p.raised,
        error = p.coral,
        outline = Color(0xFF3A3D46),
        outlineVariant = p.stroke,
    )
} else {
    lightColorScheme(
        primary = p.blue,
        onPrimary = Color.White,
        primaryContainer = p.blue.copy(alpha = 0.16f),
        onPrimaryContainer = p.text,
        secondary = p.green,
        onSecondary = Color.White,
        secondaryContainer = p.green.copy(alpha = 0.16f),
        onSecondaryContainer = p.text,
        background = p.bg,
        onBackground = p.text,
        surface = p.surface,
        onSurface = p.text,
        surfaceVariant = p.raised,
        onSurfaceVariant = p.text2,
        surfaceContainerLowest = p.bg,
        surfaceContainerLow = p.surface,
        surfaceContainer = p.surface,
        surfaceContainerHigh = p.raised,
        surfaceContainerHighest = p.raised,
        error = p.coral,
        outline = Color(0xFFBFC3CC),
        outlineVariant = p.stroke,
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
    Status.PASS -> Icons.Filled.Check
    Status.WARN -> Icons.Filled.Warning
    Status.FAIL -> Icons.Filled.Close
    Status.BLOCKED -> Icons.Filled.Lock
}

/** A status as a tinted circle with its sign, like a check mark in a soft ring. */
@Composable
fun StatusIcon(s: Status, size: Dp = 28.dp) {
    val c = statusColor(s)
    Box(
        Modifier.size(size).clip(CircleShape).background(c.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(statusIcon(s), contentDescription = s.label, tint = c, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
fun StatusPill(s: Status, text: String = s.label) {
    val c = statusColor(s)
    Text(
        text,
        color = c,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(c.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** The StarHash mark: "*#" in charcoal on the blue-to-green gradient. */
@Composable
fun Logo(size: Dp = 40.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(Sh.Gradient),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy((-1).dp), verticalAlignment = Alignment.CenterVertically) {
            Text("*", color = Sh.Charcoal, fontWeight = FontWeight.Black, fontSize = (size.value * 0.55f).sp)
            Text("#", color = Sh.Charcoal, fontWeight = FontWeight.Black, fontSize = (size.value * 0.42f).sp)
        }
    }
}

/** The INNOVII logo: white on dark, navy on light. */
@Composable
fun InnoviiLogo(modifier: Modifier = Modifier, height: Dp = 20.dp, onDark: Boolean = LocalPalette.current.dark) {
    Image(
        painter = painterResource(if (onDark) R.drawable.innovii_white else R.drawable.innovii_navy),
        contentDescription = "INNOVII",
        modifier = modifier.height(height),
    )
}
