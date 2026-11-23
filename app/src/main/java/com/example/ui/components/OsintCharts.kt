package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.DiscoveryStats
import com.example.ui.theme.AmberLimited
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldFree
import com.example.ui.theme.RosePaid
import com.example.ui.theme.TacticalCyan

@Composable
fun AccessDistributionChartCard(
    stats: DiscoveryStats,
    modifier: Modifier = Modifier
) {
    val total = (stats.freeCount + stats.limitedCount + stats.paidCount).coerceAtLeast(1)
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ACCESS MODEL DISTRIBUTION",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Verified pricing split across ${stats.totalDiscovered} indexed sites",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(110.dp)
                ) {
                    val trackColor = MaterialTheme.colorScheme.surfaceVariant
                    Canvas(modifier = Modifier.size(100.dp)) {
                        val stroke = Stroke(width = 18f, cap = StrokeCap.Butt)
                        drawArc(
                            color = trackColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = stroke
                        )
                        if (stats.totalDiscovered > 0) {
                            val freeAngle = (stats.freeCount.toFloat() / total) * 360f
                            val limAngle = (stats.limitedCount.toFloat() / total) * 360f
                            val paidAngle = (stats.paidCount.toFloat() / total) * 360f
                            var start = -90f
                            if (freeAngle > 0f) {
                                drawArc(EmeraldFree, start, freeAngle, false, style = stroke)
                                start += freeAngle
                            }
                            if (limAngle > 0f) {
                                drawArc(AmberLimited, start, limAngle, false, style = stroke)
                                start += limAngle
                            }
                            if (paidAngle > 0f) {
                                drawArc(RosePaid, start, paidAngle, false, style = stroke)
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${stats.totalDiscovered}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "SITES",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    LegendRow(color = EmeraldFree, label = "Free Tools", count = stats.freeCount)
                    LegendRow(color = AmberLimited, label = "Limited Access", count = stats.limitedCount)
                    LegendRow(color = RosePaid, label = "Paid Only", count = stats.paidCount)
                    LegendRow(color = TacticalCyan, label = "Open Source", count = stats.openSourceCount)
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdownChartCard(
    topCategories: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    val maxCount = (topCategories.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "TOP INTELLIGENCE DOMAINS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Most active categories in your library",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            if (topCategories.isEmpty()) {
                Text(
                    text = "Run an AI Search to populate category metrics.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val trackColor = MaterialTheme.colorScheme.surfaceVariant
                    topCategories.forEachIndexed { idx, (name, count) ->
                        val barColor = if (idx % 2 == 0) ElectricBlue else TacticalCyan
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = barColor
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                            ) {
                                drawRoundRect(
                                    color = trackColor,
                                    topLeft = Offset.Zero,
                                    size = size,
                                    cornerRadius = CornerRadius(4f, 4f)
                                )
                                val fillWidth = (count.toFloat() / maxCount) * size.width
                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset.Zero,
                                    size = Size(fillWidth.coerceAtLeast(8f), size.height),
                                    cornerRadius = CornerRadius(4f, 4f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
