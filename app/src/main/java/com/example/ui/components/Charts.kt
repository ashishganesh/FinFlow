package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.min

// Glowing vibrant colors matching our premium theme
val ChartColors = listOf(
    Color(0xFFCD9BFF), // Premium Light Purple
    Color(0xFF34D399), // Pastel Emerald
    Color(0xFF60A5FA), // Ambient Blue
    Color(0xFFF87171), // Soft Coral Red
    Color(0xFFFBBF24), // Vibrant Honey Yellow
    Color(0xFF2DD4BF), // Neon Turquoise
    Color(0xFFF472B6)  // Blush Rose
)

@Composable
fun CategoryDonutChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    currencySymbol: String = "₹"
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No transaction records found for this period.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val total = data.sumOf { it.second }
    if (total <= 0) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Empty zero values logged.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Donut circle
        Canvas(
            modifier = Modifier
                .size(140.dp)
                .weight(1f)
        ) {
            val canvasSize = size
            val radius = min(canvasSize.width, canvasSize.height) / 2f
            val strokeWidth = 24.dp.toPx()
            val innerSize = Size(
                canvasSize.width - strokeWidth,
                canvasSize.height - strokeWidth
            )
            val offset = strokeWidth / 2f

            var startAngle = -90f
            data.forEachIndexed { index, pair ->
                val sweepAngle = ((pair.second / total) * 360f).toFloat()
                val color = ChartColors[index % ChartColors.size]
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth),
                    size = innerSize,
                    topLeft = Offset(offset, offset)
                )
                startAngle += sweepAngle
            }
        }

        // Legends
        Column(
            modifier = Modifier.weight(1.2f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.take(5).forEachIndexed { index, pair ->
                val color = ChartColors[index % ChartColors.size]
                val percent = (pair.second / total * 100).toInt()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color, CircleShape)
                    )
                    Column {
                        Text(
                            text = pair.first,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = "$percent% • $currencySymbol${String.format("%.1f", pair.second)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (data.size > 5) {
                val remainingSum = data.drop(5).sumOf { it.second }
                val remainingPercent = (remainingSum / total * 100).toInt()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color.Gray, CircleShape)
                    )
                    Column {
                        Text(
                            text = "Other",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$remainingPercent% • $currencySymbol${String.format("%.1f", remainingSum)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BarChartComparing(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    currencySymbol: String = "₹"
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No chart coordinates.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxVal = data.maxOfOrNull { it.second } ?: 1.0
    val limit = if (maxVal == 0.0) 1.0 else maxVal

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { pair ->
                val ratio = (pair.second / limit).toFloat().coerceIn(0.01f, 1f)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = "$currencySymbol${pair.second.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(ratio)
                            .width(18.dp)
                            .background(barColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = pair.first,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
