package com.innovii.starhash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shown for a moment when the app opens: StarHash in the middle, INNOVII at the bottom. */
@Composable
fun LoadingScreen() {
    Box(Modifier.fillMaxSize().background(DarkPalette.background)) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Logo(84.dp)
            Spacer(Modifier.height(18.dp))
            Text("StarHash", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp)
            Text("USSD & VAS quality checks", color = DarkPalette.muted, fontSize = 14.sp)
            Spacer(Modifier.height(28.dp))
            LinearProgressIndicator(
                color = Sh.Teal,
                trackColor = DarkPalette.line,
                modifier = Modifier.width(140.dp),
            )
        }
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("by", color = DarkPalette.muted, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            InnoviiLogo(height = 34.dp)
        }
    }
}
