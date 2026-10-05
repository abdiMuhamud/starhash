package com.innovii.starhash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PanelShape = RoundedCornerShape(20.dp)

/** A charcoal panel with a hairline edge: the basic card of the app. */
@Composable
fun Panel(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val m = modifier.fillMaxWidth().clip(PanelShape).background(Sh.Card).border(1.dp, Sh.Line, PanelShape)
    Box(if (onClick != null) m.clickable(onClick = onClick) else m) { content() }
}

/** The top card of a page: charcoal with a soft blue-green glow and a gradient edge. */
@Composable
fun HeroCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val glow = Brush.linearGradient(listOf(Sh.Blue.copy(alpha = 0.20f), Sh.Green.copy(alpha = 0.08f)))
    val edge = Brush.linearGradient(listOf(Sh.Blue.copy(alpha = 0.55f), Sh.Green.copy(alpha = 0.35f)))
    Box(
        modifier.fillMaxWidth().clip(shape).background(Sh.Card).background(glow).border(1.dp, edge, shape),
    ) { content() }
}

/** The main action: charcoal text on the blue-to-green gradient. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier.clip(shape)
            .background(if (enabled) Sh.Gradient else Brush.linearGradient(listOf(Sh.Inset, Sh.Inset)))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val ink = if (enabled) Sh.Charcoal else Sh.Faint
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

/** A quiet button: tinted background, coloured text. */
@Composable
fun SoftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Sh.Accent,
    icon: ImageVector? = null,
    padding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
) {
    Row(
        modifier.clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.14f)).clickable(onClick = onClick).padding(padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

/** An icon (or a short sign like "$") in a tinted circle. */
@Composable
fun IconBadge(color: Color, icon: ImageVector? = null, glyph: String? = null, size: Dp = 40.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
        if (icon != null) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(size * 0.5f))
        if (glyph != null) Text(glyph, color = color, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp)
    }
}

/** The round check: a blue disc with a white tick and a soft ring when on, an empty ring when off. */
@Composable
fun CheckDot(checked: Boolean, onClick: () -> Unit, size: Dp = 26.dp) {
    Box(
        Modifier.size(size + 10.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Box(
                Modifier.size(size).border(2.dp, Sh.Blue.copy(alpha = 0.45f), CircleShape).padding(3.dp)
                    .clip(CircleShape).background(Sh.Blue),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(size * 0.55f))
            }
        } else {
            Box(Modifier.size(size).border(2.dp, Sh.Faint, CircleShape))
        }
    }
}

/** A small capitalised label above a group, with an optional line of help. */
@Composable
fun SectionTitle(title: String, hint: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.padding(top = 26.dp, bottom = 10.dp, start = 4.dp)) {
        Text(title.uppercase(), fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.2.sp, color = Sh.Muted)
        if (hint != null) Text(hint, fontSize = 13.sp, color = Sh.Faint, modifier = Modifier.padding(top = 2.dp))
    }
}

/** A big number with a label, for result counts and balances. */
@Composable
fun StatTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.10f)).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(label, color = Sh.Muted, fontSize = 12.sp)
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Sh.Line))
}

/** A thin bar filling with the blue-to-green gradient. */
@Composable
fun GradientProgress(fraction: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(Sh.Inset)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0.02f, 1f)).fillMaxHeight().clip(RoundedCornerShape(50)).background(Sh.Gradient))
    }
}

@Composable
fun PageHeader(title: String, onBack: (() -> Unit)?, actions: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = if (onBack != null) 4.dp else 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Sh.Ink) }
        }
        Text(title, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Sh.Ink, modifier = Modifier.weight(1f))
        actions()
    }
}
