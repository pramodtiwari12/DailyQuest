package com.example.dayquest.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun DayQuestLogo(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    animated: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_anim")
    val pulse by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "logo_pulse"
        )
    } else {
        rememberUpdatedState(1.0f)
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f

            // 1. Subtle Outer Glow Ring
            val ringRadius = (w / 2f - 2.dp.toPx()) * pulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF5B5BD6).copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = ringRadius
                ),
                radius = ringRadius,
                center = Offset(cx, cy)
            )

            // 2. Geometric Shield Base
            val shieldPath = Path().apply {
                moveTo(cx, cy - h * 0.42f)
                lineTo(cx + w * 0.38f, cy - h * 0.22f)
                lineTo(cx + w * 0.30f, cy + h * 0.16f)
                lineTo(cx, cy + h * 0.42f)
                lineTo(cx - w * 0.30f, cy + h * 0.16f)
                lineTo(cx - w * 0.38f, cy - h * 0.22f)
                close()
            }

            drawPath(
                path = shieldPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF5B5BD6), Color(0xFF3730A3))
                )
            )

            drawPath(
                path = shieldPath,
                color = Color(0xFFF59E0B),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 3. Central Compass Needle / Sword Emblem
            val swordBlade = Path().apply {
                moveTo(cx, cy - h * 0.32f)
                lineTo(cx + w * 0.08f, cy)
                lineTo(cx, cy + h * 0.26f)
                lineTo(cx - w * 0.08f, cy)
                close()
            }
            drawPath(
                path = swordBlade,
                color = Color.White
            )

            // Sword Crossguard
            drawLine(
                color = Color(0xFFF59E0B),
                start = Offset(cx - w * 0.16f, cy - h * 0.04f),
                end = Offset(cx + w * 0.16f, cy - h * 0.04f),
                strokeWidth = 2.dp.toPx()
            )

            // Center Point
            drawCircle(
                color = Color(0xFFF59E0B),
                radius = 2.dp.toPx(),
                center = Offset(cx, cy - h * 0.04f)
            )
        }
    }
}
