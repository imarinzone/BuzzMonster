package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NoiseLog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NoiseLineChart(
    logs: List<NoiseLog>,
    modifier: Modifier = Modifier
) {
    if (logs.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    RoundedCornerShape(16.dp)
                )
                .testTag("chart_empty_state"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "No data",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No Sound Data Captured",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Start a silence timer to capture classroom noise data.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
        return;
    }

    // Sort logs chronologically to draw the line correctly
    val sortedLogs = logs.sortedBy { it.timestamp }.takeLast(40) // take recent 40 samples to prevent cluttering

    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Classroom Noise Fluctuations (Last 40 Samples)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val primaryColor = MaterialTheme.colorScheme.primary
        val secondaryColor = MaterialTheme.colorScheme.secondary
        val surfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
        val onSurface = MaterialTheme.colorScheme.onSurface

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    RoundedCornerShape(16.dp)
                )
                .padding(top = 16.dp, bottom = 24.dp, start = 16.dp, end = 16.dp)
                .testTag("noise_line_canvas")
        ) {
            val width = size.width
            val height = size.height

            // 1. Draw horizontal grid lines (at 25%, 50%, 75%, 100% of 100 max sound level)
            val levels = listOf(25f, 50f, 75f, 100f)
            levels.forEach { level ->
                val y = height - (level / 100f) * height
                drawLine(
                    color = surfaceVariant,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 2f
                )
            }

            // 2. Map data points to coordinates
            val maxIndex = sortedLogs.size - 1
            val points = sortedLogs.mapIndexed { index, log ->
                val x = if (maxIndex > 0) (index.toFloat() / maxIndex) * width else width / 2f
                // Inverse Y because Canvas (0,0) is top-left
                val y = height - (log.amplitude / 100f) * height
                Offset(x, y)
            }

            // 3. Draw gradient background under line
            if (points.size > 1) {
                val fillPath = Path().apply {
                    moveTo(points.first().x, height)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(points.last().x, height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.4f), Color.Transparent),
                        startY = 0f,
                        endY = height
                    )
                )
            }

            // 4. Draw the actual noise line
            if (points.size > 1) {
                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }

                drawPath(
                    path = linePath,
                    color = primaryColor,
                    style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // 5. Draw active threshold line (dashed/dotted red line)
            val avgThreshold = sortedLogs.map { it.threshold }.average().toFloat().coerceIn(20f, 90f)
            val thresholdY = height - (avgThreshold / 100f) * height
            drawLine(
                color = Color.Red.copy(alpha = 0.7f),
                start = Offset(0f, thresholdY),
                end = Offset(width, thresholdY),
                strokeWidth = 4f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
            )

            // 6. Highlight breach points in red and regular points in theme color
            sortedLogs.forEachIndexed { index, log ->
                val pt = points[index]
                val isBreached = log.amplitude >= log.threshold
                drawCircle(
                    color = if (isBreached) Color.Red else secondaryColor,
                    radius = if (isBreached) 8f else 4f,
                    center = pt
                )
            }
        }

        // Legends
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val startTime = sortedLogs.firstOrNull()?.timestamp ?: 0L
            val endTime = sortedLogs.lastOrNull()?.timestamp ?: 0L

            Text(
                text = if (startTime > 0) timeFormat.format(Date(startTime)) else "",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp, 3.dp)
                        .background(Color.Red)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Threshold Limit",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = if (endTime > 0) timeFormat.format(Date(endTime)) else "",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
