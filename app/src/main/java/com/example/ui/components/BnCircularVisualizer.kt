package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.DarkVioletSurface
import com.example.ui.theme.GlowPink
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class VoiceState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

/**
 * 3D Animated Circular Interface with real-time sound wave patterns,
 * rotating particle rings, and audio-reactive amplitude pulses.
 */
@Composable
fun BnCircularVisualizer(
    modifier: Modifier = Modifier,
    voiceState: VoiceState,
    amplitude: Float,
    onTapBn: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VisualizerTransition")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    val counterRotationAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CounterRotation"
    )

    val pulseScale = remember { Animatable(1f) }

    LaunchedEffect(amplitude, voiceState) {
        val targetScale = when (voiceState) {
            VoiceState.LISTENING -> 1f + (amplitude * 0.45f)
            VoiceState.SPEAKING -> 1f + (amplitude * 0.35f)
            VoiceState.THINKING -> 1.1f
            VoiceState.IDLE -> 1f
        }
        pulseScale.animateTo(
            targetValue = targetScale,
            animationSpec = tween(120, easing = FastOutSlowInEasing)
        )
    }

    val stateColor = when (voiceState) {
        VoiceState.IDLE -> NeonCyan
        VoiceState.LISTENING -> CyberBlue
        VoiceState.THINKING -> NeonPurple
        VoiceState.SPEAKING -> GlowPink
    }

    val stateText = when (voiceState) {
        VoiceState.IDLE -> "TAP BN TO START"
        VoiceState.LISTENING -> "LISTENING..."
        VoiceState.THINKING -> "THINKING..."
        VoiceState.SPEAKING -> "BN AI SPEAKING"
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(CircleShape)
                .testTag("bn_circular_visualizer")
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTapBn
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.width / 3.4f
                val scale = pulseScale.value

                // 1. Outer Ambient Glow Ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            stateColor.copy(alpha = 0.35f * scale),
                            stateColor.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * 1.6f * scale
                    ),
                    radius = baseRadius * 1.6f * scale,
                    center = center
                )

                // 2. Concentric Wave Rings
                for (i in 1..3) {
                    val ringRadius = baseRadius * (1f + (i * 0.18f * scale))
                    drawCircle(
                        color = stateColor.copy(alpha = (0.25f / i) * scale),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // 3. Rotating Segment Arc Rings (3D Effect)
                val arcCount = 12
                val arcRadius = baseRadius * 1.15f * scale
                for (i in 0 until arcCount) {
                    val startAngle = rotationAngle + (i * (360f / arcCount))
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(NeonCyan, NeonPurple, GlowPink, NeonCyan)
                        ),
                        startAngle = startAngle,
                        sweepAngle = 18f,
                        useCenter = false,
                        topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                        size = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2),
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // 4. Counter-Rotating Inner Soundwave Particles
                val particleCount = 24
                val particleRadius = baseRadius * 0.95f
                for (i in 0 until particleCount) {
                    val angleRad = Math.toRadians((counterRotationAngle + (i * (360.0 / particleCount))).toDouble())
                    val waveOffset = (sin(angleRad * 3 + rotationAngle * 0.05) * (15f * scale)).toFloat()
                    val pX: Float = (center.x + (particleRadius + waveOffset) * cos(angleRad)).toFloat()
                    val pY: Float = (center.y + (particleRadius + waveOffset) * sin(angleRad)).toFloat()

                    drawCircle(
                        color = if (i % 2 == 0) NeonCyan else GlowPink,
                        radius = (if (i % 3 == 0) 4.dp else 2.5.dp).toPx(),
                        center = Offset(x = pX, y = pY)
                    )
                }

                // 5. Core Animated Orb Circle
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            DarkVioletSurface,
                            stateColor.copy(alpha = 0.6f),
                            stateColor
                        ),
                        center = center,
                        radius = baseRadius * scale
                    ),
                    radius = baseRadius * scale,
                    center = center
                )

                // Core Border Highlight
                drawCircle(
                    color = NeonCyan.copy(alpha = 0.8f),
                    radius = baseRadius * scale,
                    center = center,
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            // Center Text Badge "BN"
            Text(
                text = "BN",
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 2.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // State Indicator
        Text(
            text = stateText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (voiceState == VoiceState.IDLE) TextMuted else stateColor,
            letterSpacing = 1.5.sp
        )
    }
}
