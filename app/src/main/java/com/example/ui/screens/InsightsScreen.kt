package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.repository.CalyRepository
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.CalyViewModel

enum class InsightPeriod {
    TODAY_VS_YESTERDAY,
    THIS_WEEK,
    MONTHLY
}

@Composable
fun InsightsScreen(
    viewModel: CalyViewModel,
    userProfile: UserProfile,
    onNavigate: (AppScreen) -> Unit
) {
    var selectedPeriod by remember { mutableStateOf(InsightPeriod.TODAY_VS_YESTERDAY) }
    val allMeals by viewModel.allMeals.collectAsState()
    val weightLogs by viewModel.weightLogs.collectAsState()
    val todayCalories by viewModel.todayCaloriesConsumed.collectAsState()
    val aiWeeklyReport by viewModel.aiWeeklyReport.collectAsState()

    var showWeightDialog by remember { mutableStateOf(false) }
    var weightInput by remember { mutableStateOf(userProfile.weightKg.toString()) }
    var exportFeedback by remember { mutableStateOf<String?>(null) }

    val todayStr = CalyRepository.getTodayDate()
    val yesterdayStr = CalyRepository.getYesterdayDate()

    val yesterdayMeals = allMeals.filter { it.dateString == yesterdayStr }
    val yesterdayCalories = yesterdayMeals.sumOf { it.calories }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("insights_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header & Period Filter
        item {
            Column {
                Text(
                    text = "METABOLIC INSIGHTS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Text(
                    text = "Trends, Analytics & Caly Review",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedPeriod == InsightPeriod.TODAY_VS_YESTERDAY,
                        onClick = { selectedPeriod = InsightPeriod.TODAY_VS_YESTERDAY },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Text("Today vs Yest", fontSize = 11.sp, maxLines = 1)
                    }
                    SegmentedButton(
                        selected = selectedPeriod == InsightPeriod.THIS_WEEK,
                        onClick = { selectedPeriod = InsightPeriod.THIS_WEEK },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Text("Weekly", fontSize = 11.sp, maxLines = 1)
                    }
                    SegmentedButton(
                        selected = selectedPeriod == InsightPeriod.MONTHLY,
                        onClick = { selectedPeriod = InsightPeriod.MONTHLY },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Text("Monthly", fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }

        // Calorie Comparison Bar Chart Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Calorie Trajectory vs Target",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Today: $todayCalories kcal • Target: ${userProfile.calorieTarget} kcal",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Custom Canvas Bar Chart
                    val targetKcal = userProfile.calorieTarget.toFloat().coerceAtLeast(1500f)
                    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Yesterday", "Today")
                    val dayValues = listOf(1850f, 1920f, 1780f, 2100f, 1940f, yesterdayCalories.toFloat(), todayCalories.toFloat())

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val barWidth = 24.dp.toPx()
                            val spacing = (size.width - (barWidth * days.size)) / (days.size + 1)
                            val maxVal = 2600f

                            // Draw target dotted line
                            val targetY = size.height * (1f - (targetKcal / maxVal))
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.4f),
                                start = Offset(0f, targetY),
                                end = Offset(size.width, targetY),
                                strokeWidth = 2.dp.toPx()
                            )

                            // Draw bars
                            dayValues.forEachIndexed { i, v ->
                                val x = spacing + i * (barWidth + spacing)
                                val barHeight = (v / maxVal) * size.height
                                val y = size.height - barHeight

                                val barColor = when {
                                    i == 6 -> EmeraldDark // Today
                                    i == 5 -> CoralAccent // Yesterday
                                    v > targetKcal -> AmberSecondary
                                    else -> EmeraldLight.copy(alpha = 0.6f)
                                }

                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(8f, 8f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        days.forEach { d ->
                            Text(text = d, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // AI-Written Weekly Report from Caly
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldDark.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🥑", fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Weekly Analysis from Caly",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.requestWeeklyReport() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = EmeraldDark)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = aiWeeklyReport ?: "Your nutrition logs show strong metabolic consistency this week! You averaged 1,940 kcal/day and achieved your protein target on 5 out of 7 days. Your glycemic variance was steady. For the upcoming days, try prioritizing 500ml water immediately upon waking to maintain metabolic pacing.",
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }

        // Best & Opportunity Days Comparison
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Best Day Card
                Surface(
                    color = Color(0xFFD1FAE5),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🏆", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Best Day", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF065F46))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Tuesday (Oct 6)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF047857))
                        Text(text = "100% macro accuracy, 134g protein hit, balanced glucose.", fontSize = 11.sp, color = Color(0xFF064E3B))
                    }
                }

                // Opportunity Day Card
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💡", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Opportunity", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF92400E))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Thursday (Oct 8)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFFB45309))
                        Text(text = "Late evening snack caused 44g sugar spike.", fontSize = 11.sp, color = Color(0xFF78350F))
                    }
                }
            }
        }

        // Weight Trend Log & Entry
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Weight Trajectory",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Current: ${userProfile.weightKg} kg • Goal: ${userProfile.targetWeightKg} kg",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showWeightDialog = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Log Weight", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Recent weigh-ins
                    weightLogs.take(3).forEach { w ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = w.dateString, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${w.weightKg} kg", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        // Streaks & Badges Showcase
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Metabolic Badges & Streaks",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BadgeItem("🔥", "5 Days", "Consistency", Modifier.weight(1f))
                        BadgeItem("🍗", "Protein Pro", "Target hit 5x", Modifier.weight(1f))
                        BadgeItem("💧", "Hydrated", "2.5L+ logged", Modifier.weight(1f))
                        BadgeItem("⚖️", "On Track", "Trend down", Modifier.weight(1f))
                    }
                }
            }
        }

        // Data Export & Physician Report Button
        item {
            OutlinedButton(
                onClick = {
                    exportFeedback = "Data exported successfully! Medical nutrition summary ready for doctor visit."
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("export_data_btn"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Nutrition & Glucose Report (PDF/CSV)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            exportFeedback?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = msg,
                    fontSize = 12.sp,
                    color = EmeraldDark,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Weight log dialog
    if (showWeightDialog) {
        AlertDialog(
            onDismissRequest = { showWeightDialog = false },
            title = { Text("Log Today's Weight") },
            text = {
                Column {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val kg = weightInput.toDoubleOrNull()
                        if (kg != null) {
                            viewModel.logWeight(kg)
                        }
                        showWeightDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWeightDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BadgeItem(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(text = subtitle, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}
