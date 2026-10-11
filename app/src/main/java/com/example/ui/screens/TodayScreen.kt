package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MealItem
import com.example.data.model.MealType
import com.example.data.model.UserProfile
import com.example.ui.components.CalorieProgressRing
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.CalyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TodayScreen(
    viewModel: CalyViewModel,
    userProfile: UserProfile,
    onNavigate: (AppScreen) -> Unit
) {
    val todayMeals by viewModel.todayMeals.collectAsState()
    val consumedCalories by viewModel.todayCaloriesConsumed.collectAsState()
    val consumedProtein by viewModel.todayProteinConsumed.collectAsState()
    val consumedCarbs by viewModel.todayCarbsConsumed.collectAsState()
    val consumedFat by viewModel.todayFatConsumed.collectAsState()
    val consumedFiber by viewModel.todayFiberConsumed.collectAsState()
    val consumedSugar by viewModel.todaySugarConsumed.collectAsState()
    val waterMl by viewModel.waterIntakeMl.collectAsState()

    val formattedDate = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    }

    var mealToDelete by remember { mutableStateOf<MealItem?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("today_screen_list"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date & Streak Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TODAY'S METABOLIC LOG",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                // Streak pill
                Surface(
                    color = CoralContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🔥", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${userProfile.streakDays} Day Streak",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoralAccent
                        )
                    }
                }
            }
        }

        // Daily Free Log Usage Meter (if not premium)
        if (!userProfile.isPremium) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Free Plan: ${todayMeals.size}/3 meals logged today",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        TextButton(
                            onClick = { viewModel.showPaywallModal() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Unlock Unlimited",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Diabetes Risk Awareness Banner (if enabled)
        if (userProfile.isDiabetesAwareness) {
            item {
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = "Health notice",
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Blood Sugar Awareness (Not a Diagnosis)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val sugarStatus = if (consumedSugar > userProfile.sugarLimitGrams) {
                                "Sugar today (${consumedSugar.toInt()}g) exceeds recommended threshold (${userProfile.sugarLimitGrams}g). Pair with fiber and consider discussing patterns with your doctor."
                            } else {
                                "Current sugar intake (${consumedSugar.toInt()}g / ${userProfile.sugarLimitGrams}g) is in a healthy range. Maintaining steady fiber intake blunts glucose spikes."
                            }
                            Text(
                                text = sugarStatus,
                                fontSize = 11.sp,
                                color = Color(0xFF78350F),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // Calorie & Macro Progress Ring
        item {
            CalorieProgressRing(
                consumedCalories = consumedCalories,
                targetCalories = userProfile.calorieTarget,
                consumedProtein = consumedProtein,
                targetProtein = userProfile.proteinTargetGrams,
                consumedCarbs = consumedCarbs,
                targetCarbs = userProfile.carbsTargetGrams,
                consumedFat = consumedFat,
                targetFat = userProfile.fatTargetGrams,
                consumedSugar = consumedSugar,
                limitSugar = userProfile.sugarLimitGrams,
                consumedFiber = consumedFiber,
                targetFiber = userProfile.fiberTargetGrams
            )
        }

        // Water Tracking Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CyanTertiary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "💧", fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Hydration Tracker",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$waterMl / ${userProfile.waterTargetMl} ml",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Water progress percentage
                        val waterProgress = (waterMl.toFloat() / userProfile.waterTargetMl).coerceIn(0f, 1f)
                        Text(
                            text = "${(waterProgress * 100).toInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyanTertiary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { (waterMl.toFloat() / userProfile.waterTargetMl).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = CyanTertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.logWater(250) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanTertiary.copy(alpha = 0.15f),
                                contentColor = CyanTertiary
                            ),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("+250 ml", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.logWater(500) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanTertiary.copy(alpha = 0.15f),
                                contentColor = CyanTertiary
                            ),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("+500 ml", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.logWater(-250) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("-250", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Section: Today's Meals Header + Quick Add
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Logged Meals (${todayMeals.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                FilledTonalButton(
                    onClick = { onNavigate(AppScreen.CAMERA) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_meal_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Log Meal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // List of Meals or Empty State
        if (todayMeals.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🍽️", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No meals logged yet today",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Snap a food photo or search manually to get started.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { onNavigate(AppScreen.CAMERA) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Scan First Meal")
                        }
                    }
                }
            }
        } else {
            items(todayMeals, key = { it.id }) { meal ->
                MealCard(
                    meal = meal,
                    onDelete = { mealToDelete = meal }
                )
            }
        }
    }

    // Delete confirmation dialog
    mealToDelete?.let { meal ->
        AlertDialog(
            onDismissRequest = { mealToDelete = null },
            title = { Text("Delete Meal Entry?") },
            text = { Text("Remove \"${meal.name}\" (${meal.calories} kcal) from today's log?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMeal(meal.id)
                        mealToDelete = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { mealToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MealCard(
    meal: MealItem,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("meal_card_${meal.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = meal.mealType.displayName.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = meal.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Portion description & Notes
            if (meal.notes.isNotBlank()) {
                Text(
                    text = meal.notes,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            // Calories & Macro pills row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${meal.calories} kcal",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MacroTag("P", "${meal.protein.toInt()}g", MacroProtein)
                    MacroTag("C", "${meal.carbs.toInt()}g", MacroCarbs)
                    MacroTag("F", "${meal.fat.toInt()}g", MacroFat)
                    if (meal.fiber > 0) MacroTag("Fib", "${meal.fiber.toInt()}g", MacroFiber)
                }
            }
        }
    }
}

@Composable
private fun MacroTag(label: String, value: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.width(3.dp))
            Text(text = value, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
