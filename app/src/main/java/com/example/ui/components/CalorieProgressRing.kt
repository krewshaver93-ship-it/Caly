package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun CalorieProgressRing(
    consumedCalories: Int,
    targetCalories: Int,
    consumedProtein: Double,
    targetProtein: Int,
    consumedCarbs: Double,
    targetCarbs: Int,
    consumedFat: Double,
    targetFat: Int,
    consumedSugar: Double,
    limitSugar: Int,
    consumedFiber: Double,
    targetFiber: Int,
    modifier: Modifier = Modifier
) {
    val progress = (consumedCalories.toFloat() / targetCalories.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1.5f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800)
    )

    val remaining = targetCalories - consumedCalories
    val isNearLimit = consumedCalories >= targetCalories * 0.9 && consumedCalories <= targetCalories
    val isOverLimit = consumedCalories > targetCalories

    val ringColor = when {
        isOverLimit -> Color(0xFFEF4444) // Crimson Red
        isNearLimit -> Color(0xFFF59E0B) // Warning Amber
        else -> EmeraldDark
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("calorie_progress_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Near/Over Limit Alert Banner
            if (isOverLimit || isNearLimit) {
                Surface(
                    color = if (isOverLimit) Color(0xFFFEE2E2) else Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alert",
                            tint = if (isOverLimit) Color(0xFFDC2626) else Color(0xFFD97706),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isOverLimit) "Exceeded calorie goal by ${-remaining} kcal" else "Nearing daily limit (~${(progress * 100).toInt()}%)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOverLimit) Color(0xFF991B1B) else Color(0xFF92400E)
                        )
                    }
                }
            }

            // Ring Canvas + Center Numbers
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    // Background track
                    drawCircle(
                        color = trackColor,
                        style = Stroke(width = strokeWidth)
                    )
                    // Progress arc
                    val sweepAngle = (animatedProgress.coerceAtMost(1f)) * 360f
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Inner content
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (remaining >= 0) "$remaining" else "+${-remaining}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isOverLimit) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (remaining >= 0) "kcal remaining" else "kcal over goal",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$consumedCalories / $targetCalories",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Macro Breakdown Bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroPill(
                    name = "Protein",
                    current = consumedProtein,
                    target = targetProtein.toDouble(),
                    unit = "g",
                    color = MacroProtein,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                MacroPill(
                    name = "Carbs",
                    current = consumedCarbs,
                    target = targetCarbs.toDouble(),
                    unit = "g",
                    color = MacroCarbs,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                MacroPill(
                    name = "Fat",
                    current = consumedFat,
                    target = targetFat.toDouble(),
                    unit = "g",
                    color = MacroFat,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary: Sugar & Fiber
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroPill(
                    name = "Fiber",
                    current = consumedFiber,
                    target = targetFiber.toDouble(),
                    unit = "g",
                    color = MacroFiber,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                MacroPill(
                    name = "Sugar",
                    current = consumedSugar,
                    target = limitSugar.toDouble(),
                    unit = "g",
                    color = if (consumedSugar > limitSugar) Color(0xFFEF4444) else MacroSugar,
                    isLimit = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MacroPill(
    name: String,
    current: Double,
    target: Double,
    unit: String,
    color: Color,
    isLimit: Boolean = false,
    modifier: Modifier = Modifier
) {
    val progress = (current / target.coerceAtLeast(1.0)).coerceIn(0.0, 1.0).toFloat()

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${current.toInt()} / ${target.toInt()}$unit",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Mini progress line
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            )
        }
    }
}
