package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import com.example.data.model.DietaryPreference
import com.example.data.model.MealType
import com.example.data.model.Recipe
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.CalyViewModel

@Composable
fun RecipesScreen(
    viewModel: CalyViewModel,
    onNavigate: (AppScreen) -> Unit
) {
    val recipes by viewModel.recipes.collectAsState()
    val isRecipeLoading by viewModel.isRecipeLoading.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val consumedKcal by viewModel.todayCaloriesConsumed.collectAsState()

    val remainingKcal = (userProfile.calorieTarget - consumedKcal).coerceAtLeast(0)

    var selectedPreference by remember { mutableStateOf(DietaryPreference.BALANCED) }
    var ingredientsInput by remember { mutableStateOf("") }
    var showLogMealDialogForRecipe by remember { mutableStateOf<Recipe?>(null) }
    var expandedRecipeId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("recipes_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Generator Header Card
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
                                text = "AI Recipe Architect",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Calibrated to your ~$remainingKcal kcal remaining today",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Dietary Style:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Preference chips row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            DietaryPreference.HIGH_PROTEIN,
                            DietaryPreference.BALANCED,
                            DietaryPreference.LOW_CARB,
                            DietaryPreference.GLP1_GENTLE
                        ).forEach { pref ->
                            FilterChip(
                                selected = selectedPreference == pref,
                                onClick = { selectedPreference = pref },
                                label = { Text(pref.label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Ingredients on hand input
                    OutlinedTextField(
                        value = ingredientsInput,
                        onValueChange = { ingredientsInput = it },
                        placeholder = { Text("Ingredients you have (e.g. chicken breast, spinach, feta)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Generate Button
                    Button(
                        onClick = {
                            viewModel.generateRecipe(
                                preference = selectedPreference.label,
                                ingredients = ingredientsInput
                            )
                        },
                        enabled = !isRecipeLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("generate_recipe_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                    ) {
                        if (isRecipeLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating Personalized Recipe...", fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Recipe with Gemini", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "Metabolic-Friendly Recipes (${recipes.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        // List of Recipes
        items(recipes, key = { it.id }) { recipe ->
            val isExpanded = expandedRecipeId == recipe.id

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recipe_card_${recipe.id}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = recipe.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "⏱️ ${recipe.prepTimeMinutes} mins prep • ${recipe.calories} kcal",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleFavoriteRecipe(recipe.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (recipe.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (recipe.isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = recipe.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Macro Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MacroProtein.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Protein: ${recipe.protein.toInt()}g",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MacroProtein,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            color = MacroCarbs.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Carbs: ${recipe.carbs.toInt()}g",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MacroCarbs,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            color = MacroFat.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Fat: ${recipe.fat.toInt()}g",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MacroFat,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Expanded Ingredients & Instructions
                    AnimatedVisibility(visible = isExpanded) {
                        Column(modifier = Modifier.padding(top = 14.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Ingredients:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            recipe.ingredients.forEach { ing ->
                                Text(
                                    text = "• $ing",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Instructions:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            recipe.instructions.forEachIndexed { idx, step ->
                                Text(
                                    text = "${idx + 1}. $step",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons: Expand & One-Tap Log as Meal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                expandedRecipeId = if (isExpanded) null else recipe.id
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isExpanded) "Hide Details" else "View Steps",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = { showLogMealDialogForRecipe = recipe },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("log_recipe_btn_${recipe.id}")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Log as Meal",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal to pick meal type when logging recipe
    showLogMealDialogForRecipe?.let { recipe ->
        var chosenMealType by remember { mutableStateOf(MealType.DINNER) }

        AlertDialog(
            onDismissRequest = { showLogMealDialogForRecipe = null },
            title = { Text("Log \"${recipe.title}\"") },
            text = {
                Column {
                    Text(
                        text = "Select meal slot to add ${recipe.calories} kcal to your daily log:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    MealType.values().forEach { mType ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { chosenMealType = mType }
                                .padding(vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = chosenMealType == mType,
                                onClick = { chosenMealType = mType }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(mType.displayName, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = viewModel.logRecipeAsMeal(recipe, chosenMealType)
                        showLogMealDialogForRecipe = null
                        if (success) {
                            onNavigate(AppScreen.TODAY)
                        }
                    }
                ) {
                    Text("Confirm & Log")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogMealDialogForRecipe = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
