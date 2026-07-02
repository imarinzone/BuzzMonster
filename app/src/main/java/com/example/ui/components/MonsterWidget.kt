package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.MonsterCharacter
import com.example.viewmodel.MonsterMood
import kotlin.math.sin

@Composable
fun MonsterWidget(
    character: MonsterCharacter,
    mood: MonsterMood,
    currentNoise: Float,
    threshold: Float,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {}
) {
    // Infinite transition for breathing, pulsing, and snoozing bubbles
    val infiniteTransition = rememberInfiniteTransition(label = "monster_anim")

    // Breathing scale: subtle expansion and contraction
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Jitter scale for Awake state (shaking effect)
    val jitterOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jitter"
    )

    // Rising Zzz coordinates
    val zPercent1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "z1"
    )

    val zPercent2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, delayMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "z2"
    )

    val zPercent3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, delayMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "z3"
    )

    // Color conversion
    val monsterColor = Color(android.graphics.Color.parseColor(character.primaryColorHex))
    val monsterColorDark = Color(
        android.graphics.Color.parseColor(
            when (character) {
                MonsterCharacter.MIO -> "#1976D2"
                MonsterCharacter.GLOOP -> "#388E3C"
                MonsterCharacter.SPIKE -> "#F57C00"
                MonsterCharacter.LUNA -> "#8E24AA"
                MonsterCharacter.TRIXIE -> "#D81B60"
                MonsterCharacter.IGNIS -> "#E53935"
            }
        )
    )

    Box(
        modifier = modifier
            .testTag("monster_widget_container")
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        // Render snoozing Z's or Roar alerts in background overlay
        if (mood == MonsterMood.SLEEPING) {
            // Rising Zzz animation
            Box(modifier = Modifier.fillMaxSize()) {
                val zList = listOf(
                    Pair(zPercent1, Offset(-40f, -80f)),
                    Pair(zPercent2, Offset(60f, -140f)),
                    Pair(zPercent3, Offset(-90f, -190f))
                )

                zList.forEach { (percent, offset) ->
                    val yPos = offset.y * percent
                    val xPos = offset.x + (sin(percent * Math.PI * 4) * 20).toFloat()
                    val size = 12f + (16f * percent)
                    val alpha = 1f - percent

                    Text(
                        text = "Z",
                        fontSize = size.sp,
                        fontWeight = FontWeight.Bold,
                        color = monsterColor.copy(alpha = alpha),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(x = xPos.dp, y = yPos.dp)
                    )
                }
            }
        } else if (mood == MonsterMood.AWAKE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-150).dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ROAR!!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Red
                )
            }
        }

        // Draw the physical monster
        Canvas(
            modifier = Modifier
                .size(240.dp)
                .testTag("monster_canvas")
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f

            // Apply shaking if awake
            val currentJitter = if (mood == MonsterMood.AWAKE) jitterOffset else 0f
            val currentScale = if (mood == MonsterMood.AWAKE) 1.15f else breathingScale

            val drawX = centerX + currentJitter
            val drawY = centerY + (if (mood == MonsterMood.AWAKE) -10f else 0f)

            // Let's draw characters specifically
            when (character) {
                MonsterCharacter.MIO -> {
                    // Blue Cub with cute bear-cat ears
                    // Ears
                    val earWidth = 50f * currentScale
                    val earHeight = 60f * currentScale

                    // Left Ear
                    val leftEarPath = Path().apply {
                        moveTo(drawX - 100f * currentScale, drawY - 80f * currentScale)
                        lineTo(drawX - 140f * currentScale, drawY - 180f * currentScale)
                        lineTo(drawX - 50f * currentScale, drawY - 120f * currentScale)
                        close()
                    }
                    drawPath(leftEarPath, monsterColorDark)

                    // Right Ear
                    val rightEarPath = Path().apply {
                        moveTo(drawX + 100f * currentScale, drawY - 80f * currentScale)
                        lineTo(drawX + 140f * currentScale, drawY - 180f * currentScale)
                        lineTo(drawX + 50f * currentScale, drawY - 120f * currentScale)
                        close()
                    }
                    drawPath(rightEarPath, monsterColorDark)

                    // Inner ears
                    val leftInnerEarPath = Path().apply {
                        moveTo(drawX - 95f * currentScale, drawY - 90f * currentScale)
                        lineTo(drawX - 125f * currentScale, drawY - 165f * currentScale)
                        lineTo(drawX - 60f * currentScale, drawY - 115f * currentScale)
                        close()
                    }
                    drawPath(leftInnerEarPath, Color(0xFFFFA07A)) // Peach pink inner ear

                    val rightInnerEarPath = Path().apply {
                        moveTo(drawX + 95f * currentScale, drawY - 90f * currentScale)
                        lineTo(drawX + 125f * currentScale, drawY - 165f * currentScale)
                        lineTo(drawX + 60f * currentScale, drawY - 115f * currentScale)
                        close()
                    }
                    drawPath(rightInnerEarPath, Color(0xFFFFA07A))

                    // Main Body
                    drawCircle(
                        color = monsterColor,
                        radius = 110f * currentScale,
                        center = Offset(drawX, drawY)
                    )

                    // Belly patch
                    drawCircle(
                        color = Color.White.copy(alpha = 0.25f),
                        radius = 60f * currentScale,
                        center = Offset(drawX, drawY + 40f * currentScale)
                    )
                }
                MonsterCharacter.GLOOP -> {
                    // Green Slime-Blob (wobbles and has liquid shape)
                    val bodyPath = Path().apply {
                        val baseRadius = 110f * currentScale
                        // Custom organic shape
                        moveTo(drawX - baseRadius, drawY + baseRadius * 0.5f)
                        cubicTo(
                            drawX - baseRadius * 1.1f, drawY - baseRadius * 0.8f,
                            drawX - baseRadius * 0.4f, drawY - baseRadius * 1.2f,
                            drawX, drawY - baseRadius
                        )
                        cubicTo(
                            drawX + baseRadius * 0.4f, drawY - baseRadius * 1.2f,
                            drawX + baseRadius * 1.1f, drawY - baseRadius * 0.8f,
                            drawX + baseRadius, drawY + baseRadius * 0.5f
                        )
                        cubicTo(
                            drawX + baseRadius * 0.9f, drawY + baseRadius * 1.1f,
                            drawX - baseRadius * 0.9f, drawY + baseRadius * 1.1f,
                            drawX - baseRadius, drawY + baseRadius * 0.5f
                        )
                        close()
                    }
                    drawPath(bodyPath, monsterColor)

                    // Drops / Bubbles on body
                    drawCircle(
                        color = monsterColorDark,
                        radius = 12f * currentScale,
                        center = Offset(drawX - 40f * currentScale, drawY - 50f * currentScale)
                    )
                    drawCircle(
                        color = monsterColorDark,
                        radius = 8f * currentScale,
                        center = Offset(drawX + 50f * currentScale, drawY + 20f * currentScale)
                    )
                }
                MonsterCharacter.SPIKE -> {
                    // Orange Dino with spines
                    // Spikes
                    val spineSize = 35f * currentScale
                    val spinePathL = Path().apply {
                        moveTo(drawX - 80f * currentScale, drawY - 80f * currentScale)
                        lineTo(drawX - 130f * currentScale, drawY - 110f * currentScale)
                        lineTo(drawX - 50f * currentScale, drawY - 110f * currentScale)
                        close()
                    }
                    drawPath(spinePathL, Color(0xFFFFD54F)) // Yellow-Gold Spikes

                    val spinePathC = Path().apply {
                        moveTo(drawX, drawY - 110f * currentScale)
                        lineTo(drawX, drawY - 160f * currentScale)
                        lineTo(drawX + 30f * currentScale, drawY - 120f * currentScale)
                        close()
                    }
                    drawPath(spinePathC, Color(0xFFFFD54F))

                    val spinePathR = Path().apply {
                        moveTo(drawX + 80f * currentScale, drawY - 80f * currentScale)
                        lineTo(drawX + 130f * currentScale, drawY - 110f * currentScale)
                        lineTo(drawX + 50f * currentScale, drawY - 110f * currentScale)
                        close()
                    }
                    drawPath(spinePathR, Color(0xFFFFD54F))

                    // Main Body
                    drawRoundRect(
                        color = monsterColor,
                        topLeft = Offset(drawX - 110f * currentScale, drawY - 100f * currentScale),
                        size = Size(220f * currentScale, 200f * currentScale),
                        cornerRadius = CornerRadius(80f * currentScale, 80f * currentScale)
                    )
                }
                MonsterCharacter.LUNA -> {
                    // Purple Owl
                    // Left ear tuft
                    val tuftL = Path().apply {
                        moveTo(drawX - 60f * currentScale, drawY - 90f * currentScale)
                        lineTo(drawX - 90f * currentScale, drawY - 140f * currentScale)
                        lineTo(drawX - 20f * currentScale, drawY - 100f * currentScale)
                        close()
                    }
                    drawPath(tuftL, monsterColorDark)
                    // Right ear tuft
                    val tuftR = Path().apply {
                        moveTo(drawX + 60f * currentScale, drawY - 90f * currentScale)
                        lineTo(drawX + 90f * currentScale, drawY - 140f * currentScale)
                        lineTo(drawX + 20f * currentScale, drawY - 100f * currentScale)
                        close()
                    }
                    drawPath(tuftR, monsterColorDark)
                    // Main Body
                    drawCircle(color = monsterColor, radius = 105f * currentScale, center = Offset(drawX, drawY))
                    // Belly
                    drawCircle(color = Color.White.copy(alpha = 0.4f), radius = 65f * currentScale, center = Offset(drawX, drawY + 35f * currentScale))
                    // Beak
                    val beak = Path().apply {
                        moveTo(drawX - 15f * currentScale, drawY - 5f * currentScale)
                        lineTo(drawX + 15f * currentScale, drawY - 5f * currentScale)
                        lineTo(drawX, drawY + 15f * currentScale)
                        close()
                    }
                    drawPath(beak, Color(0xFFFFCA28))
                }
                MonsterCharacter.TRIXIE -> {
                    // Pink Bunny
                    // Left Ear
                    drawRoundRect(
                        color = monsterColor,
                        topLeft = Offset(drawX - 55f * currentScale, drawY - 190f * currentScale),
                        size = Size(35f * currentScale, 120f * currentScale),
                        cornerRadius = CornerRadius(20f * currentScale, 20f * currentScale)
                    )
                    // Right Ear
                    drawRoundRect(
                        color = monsterColor,
                        topLeft = Offset(drawX + 20f * currentScale, drawY - 190f * currentScale),
                        size = Size(35f * currentScale, 120f * currentScale),
                        cornerRadius = CornerRadius(20f * currentScale, 20f * currentScale)
                    )
                    // Main Body
                    drawCircle(
                        color = monsterColor,
                        radius = 100f * currentScale,
                        center = Offset(drawX, drawY)
                    )
                    // Little tail
                    drawCircle(
                        color = Color.White,
                        radius = 25f * currentScale,
                        center = Offset(drawX + 85f * currentScale, drawY + 70f * currentScale)
                    )
                }
                MonsterCharacter.IGNIS -> {
                    // Fire Spark
                    val flamePath = Path().apply {
                        moveTo(drawX, drawY - 130f * currentScale)
                        cubicTo(
                            drawX + 120f * currentScale, drawY - 20f * currentScale,
                            drawX + 110f * currentScale, drawY + 110f * currentScale,
                            drawX, drawY + 110f * currentScale
                        )
                        cubicTo(
                            drawX - 110f * currentScale, drawY + 110f * currentScale,
                            drawX - 120f * currentScale, drawY - 20f * currentScale,
                            drawX, drawY - 130f * currentScale
                        )
                        close()
                    }
                    drawPath(flamePath, monsterColor)
                    
                    val innerFlame = Path().apply {
                        moveTo(drawX, drawY - 50f * currentScale)
                        cubicTo(
                            drawX + 60f * currentScale, drawY + 20f * currentScale,
                            drawX + 50f * currentScale, drawY + 90f * currentScale,
                            drawX, drawY + 90f * currentScale
                        )
                        cubicTo(
                            drawX - 50f * currentScale, drawY + 90f * currentScale,
                            drawX - 60f * currentScale, drawY + 20f * currentScale,
                            drawX, drawY - 50f * currentScale
                        )
                        close()
                    }
                    drawPath(innerFlame, Color(0xFFFFCA28))
                }
            }

            // Draw Face depending on Mood
            val eyeRadius = 18f * currentScale
            val eyeSpacing = 45f * currentScale
            val eyeY = drawY - 20f * currentScale

            when (mood) {
                MonsterMood.SLEEPING -> {
                    // Slept curved closed eyes (smile paths inverted)
                    // Left closed eye
                    drawArc(
                        color = Color.Black,
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(drawX - eyeSpacing - eyeRadius, eyeY),
                        size = Size(eyeRadius * 2, eyeRadius * 0.8f),
                        style = Stroke(width = 5f)
                    )
                    // Right closed eye
                    drawArc(
                        color = Color.Black,
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(drawX + eyeSpacing - eyeRadius, eyeY),
                        size = Size(eyeRadius * 2, eyeRadius * 0.8f),
                        style = Stroke(width = 5f)
                    )

                    // Small smiling sleep mouth
                    drawArc(
                        color = Color.Black,
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(drawX - 15f * currentScale, drawY + 15f * currentScale),
                        size = Size(30f * currentScale, 15f * currentScale),
                        style = Stroke(width = 4f)
                    )
                }
                MonsterMood.WARY -> {
                    // Worried eyes looking around (eyes partly open, looking wide)
                    // Left white eye background
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(drawX - eyeSpacing - eyeRadius, eyeY - 10f * currentScale),
                        size = Size(eyeRadius * 2, eyeRadius * 1.5f),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                    // Right white eye background
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(drawX + eyeSpacing - eyeRadius, eyeY - 10f * currentScale),
                        size = Size(eyeRadius * 2, eyeRadius * 1.5f),
                        cornerRadius = CornerRadius(10f, 10f)
                    )

                    // Pupils looking sideways
                    drawCircle(
                        color = Color.Black,
                        radius = 8f * currentScale,
                        center = Offset(drawX - eyeSpacing + 4f, eyeY + 2f)
                    )
                    drawCircle(
                        color = Color.Black,
                        radius = 8f * currentScale,
                        center = Offset(drawX + eyeSpacing + 4f, eyeY + 2f)
                    )

                    // Worried mouth: straight squiggly line
                    val mouthY = drawY + 20f * currentScale
                    drawLine(
                        color = Color.Black,
                        start = Offset(drawX - 25f * currentScale, mouthY),
                        end = Offset(drawX + 25f * currentScale, mouthY),
                        strokeWidth = 5f
                    )
                }
                MonsterMood.AWAKE -> {
                    // Huge shocked eyes
                    // Left white eye
                    drawCircle(
                        color = Color.White,
                        radius = eyeRadius * 1.6f,
                        center = Offset(drawX - eyeSpacing, eyeY)
                    )
                    // Right white eye
                    drawCircle(
                        color = Color.White,
                        radius = eyeRadius * 1.6f,
                        center = Offset(drawX + eyeSpacing, eyeY)
                    )

                    // Huge black pupils
                    drawCircle(
                        color = Color.Black,
                        radius = 12f * currentScale,
                        center = Offset(drawX - eyeSpacing, eyeY)
                    )
                    drawCircle(
                        color = Color.Black,
                        radius = 12f * currentScale,
                        center = Offset(drawX + eyeSpacing, eyeY)
                    )

                    // Tiny white reflection in pupils
                    drawCircle(
                        color = Color.White,
                        radius = 4f * currentScale,
                        center = Offset(drawX - eyeSpacing - 3f, eyeY - 3f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4f * currentScale,
                        center = Offset(drawX + eyeSpacing - 3f, eyeY - 3f)
                    )

                    // Huge circular roaring mouth
                    drawCircle(
                        color = Color.Black,
                        radius = 28f * currentScale,
                        center = Offset(drawX, drawY + 35f * currentScale)
                    )
                    // Red tongue inside mouth
                    drawCircle(
                        color = Color(0xFFEF5350),
                        radius = 14f * currentScale,
                        center = Offset(drawX, drawY + 45f * currentScale)
                    )
                }
            }

            // Draw real-time noise ring around the monster
            val ringRadius = 135f * currentScale
            val maxStrokeWidth = 14f
            val ringProgress = (currentNoise / 100f).coerceIn(0f, 1f)
            val ringThresholdProgress = (threshold / 100f).coerceIn(0f, 1f)

            // 1. Draw inactive background ring
            drawCircle(
                color = Color.LightGray.copy(alpha = 0.2f),
                radius = ringRadius,
                center = Offset(drawX, drawY),
                style = Stroke(width = maxStrokeWidth)
            )

            // 2. Draw active noise level ring (color-coded: Green -> Yellow -> Red)
            val ringColor = when {
                currentNoise >= threshold -> Color.Red
                currentNoise >= threshold * 0.75f -> Color(0xFFFFB300) // Orange-yellow
                else -> Color(0xFF4CAF50) // Beautiful Material Green
            }

            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * ringProgress,
                useCenter = false,
                topLeft = Offset(drawX - ringRadius, drawY - ringRadius),
                size = Size(ringRadius * 2, ringRadius * 2),
                style = Stroke(width = maxStrokeWidth)
            )

            // 3. Draw threshold marker tick on the ring
            val angleRad = Math.toRadians((-90f + 360f * ringThresholdProgress).toDouble())
            val tickX = drawX + (ringRadius * Math.cos(angleRad)).toFloat()
            val tickY = drawY + (ringRadius * Math.sin(angleRad)).toFloat()

            drawCircle(
                color = Color.Red,
                radius = 8f,
                center = Offset(tickX, tickY)
            )
        }
    }
}
