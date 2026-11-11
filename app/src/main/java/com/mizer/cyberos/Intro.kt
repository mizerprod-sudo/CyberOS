package com.mizer.cyberos

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun Intro(acc: Color, onDone: () -> Unit) {
    val purple = Color(0xFFB026FF)
    val title = "CYBER//OS"
    val lines = listOf(
        "> INIT NEURAL CORE ........ OK",
        "> LOAD HUD INTERFACE ...... OK",
        "> SYNC NEON GRID .......... OK",
        "> ACCESS GRANTED"
    )
    var typed by remember { mutableIntStateOf(0) }
    var shown by remember { mutableIntStateOf(0) }
    var credit by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val scan = rememberInfiniteTransition(label = "scan").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "y"
    )
    val creditAlpha by animateFloatAsState(if (credit) 1f else 0f, tween(700), label = "credit")

    LaunchedEffect(Unit) {
        launch { progress.animateTo(1f, tween(3200, easing = LinearEasing)) }
        for (i in 1..title.length) {
            typed = i
            delay(80)
        }
        for (i in 1..lines.size) {
            delay(300)
            shown = i
        }
        delay(300)
        credit = true
        delay(1000)
        onDone()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .drawBehind {
                val y = scan.value * size.height
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.Transparent, acc.copy(alpha = 0.18f), Color.Transparent),
                        startY = y - 60f,
                        endY = y + 60f
                    ),
                    topLeft = Offset(0f, y - 60f),
                    size = Size(size.width, 120f)
                )
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDone() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title.take(typed) + if (typed < title.length) "█" else "",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 4.sp,
                style = TextStyle(shadow = Shadow(acc, Offset.Zero, 28f))
            )
            Spacer(Modifier.height(22.dp))
            lines.forEachIndexed { i, l ->
                Text(
                    text = l,
                    color = acc,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .alpha(if (i < shown) 1f else 0f)
                        .padding4()
                )
            }
            Spacer(Modifier.height(22.dp))
            Box(
                Modifier
                    .width(260.dp)
                    .height(3.dp)
                    .background(Color(0x22FFFFFF))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.value)
                        .fillMaxSize()
                        .background(acc)
                )
            }
            Spacer(Modifier.height(36.dp))
            Column(
                Modifier.alpha(creditAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "PROJECT CREATED BY",
                    color = Color(0x99FFFFFF),
                    fontSize = 11.sp,
                    letterSpacing = 4.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "MIZER",
                    color = purple,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    style = TextStyle(shadow = Shadow(purple, Offset.Zero, 30f))
                )
            }
        }
    }
}

private fun Modifier.padding4(): Modifier = this.then(Modifier.height(20.dp))
