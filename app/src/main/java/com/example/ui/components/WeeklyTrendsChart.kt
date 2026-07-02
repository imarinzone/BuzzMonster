package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SessionLog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WeeklyTrendsChart(
    sessions: List<SessionLog>,
    modifier: Modifier = Modifier
) {
    if (sessions.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    RoundedCornerShape(16.dp)
                )
                .testTag("weekly_chart_empty"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "No trends",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No Session History Yet",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Completed sessions will compile here to show weekly engagement trends.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
        return
    }

    // Grab the last 7 recorded sessions (most recent sessions)
    // Sort chronological left-to-right (oldest of the last 7 to newest of the last 7)
    val trendSessions = sessions.sortedBy { it.timestamp }.takeLast(7)

    val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Classroom Quiet Success (Last 7 Sessions)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    RoundedCornerShape(16.dp)
                )
                .padding(top = 24.dp, bottom = 12.dp, start = 24.dp, end = 24.dp)
                .testTag("weekly_trends_canvas")
        ) {
            val width = size.width
            val height = size.height

            val barCount = trendSessions.size
            val gapRatio = 0.4f // spacing between bars
            val rawBarWidth = width / barCount
            val barSpacing = rawBarWidth * gapRatio
            val barWidth = rawBarWidth - barSpacing

            // Draw bars
            trendSessions.forEachIndexed { index, session ->
                // Normalizing height by standard maximum duration. Let's cap max visual height at 10 minutes (600s)
                val maxDuration = 600f
                val heightPercent = (session.durationSeconds.toFloat() / maxDuration).coerceIn(0.15f, 1f)
                val barHeight = heightPercent * height

                val x = (index * rawBarWidth) + (barSpacing / 2f)
                val y = height - barHeight

                // Select color based on resets count
                val barColor = when {
                    session.successful -> Color(0xFF4CAF50) // Solid green: 100% success!
                    session.resetsCount <= 2 -> Color(0xFFFFB300) // Yellow/orange: minor noise
                    else -> Color(0xFFEF5350) // Red: loud classroom
                }

                // Draw solid background column
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(16f, 16f)
                )

                // Optional: Draw a tiny successful star on top if completed with 0 resets!
                if (session.successful) {
                    drawCircle(
                        color = Color(0xFFFFD54F),
                        radius = 8f,
                        center = Offset(x + barWidth / 2f, y - 12f)
                    )
                }
            }
        }

        // Draw Dates and Labels under each column
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            trendSessions.forEach { session ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = dateFormat.format(Date(session.id)),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (session.resetsCount == 0) "✨ Ok" else "⚡ ${session.resetsCount}",
                        fontSize = 9.sp,
                        color = if (session.resetsCount == 0) Color(0xFF4CAF50) else Color(0xFFD32F2F)
                    )
                }
            }
        }
    }
}
