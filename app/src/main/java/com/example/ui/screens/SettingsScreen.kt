package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.LanguageSelectorDialog
import com.example.ui.theme.EmeraldDark
import com.example.viewmodel.AppScreen
import com.example.viewmodel.CalyViewModel

@Composable
fun SettingsScreen(
    viewModel: CalyViewModel,
    userProfile: UserProfile,
    onNavigate: (AppScreen) -> Unit
) {
    var ageInput by remember { mutableStateOf(userProfile.age.toString()) }
    var heightInput by remember { mutableStateOf(userProfile.heightCm.toInt().toString()) }
    var weightInput by remember { mutableStateOf(userProfile.weightKg.toString()) }
    var targetWeightInput by remember { mutableStateOf(userProfile.targetWeightKg.toString()) }
    var customCalorieInput by remember { mutableStateOf(userProfile.calorieTarget.toString()) }

    var selectedGender by remember { mutableStateOf(userProfile.gender) }
    var selectedActivity by remember { mutableStateOf(userProfile.activityLevel) }
    var selectedGoal by remember { mutableStateOf(userProfile.goal) }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showGlucoseDialog by remember { mutableStateOf(false) }
    var showDisclaimerDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var saveStatusMsg by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "SETTINGS & PROFILE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Text(
                text = "Nutrition Targets & Health Modes",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        // BMR & Daily Targets Calculator Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Metabolic Profile (Mifflin-St Jeor)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your BMR and calorie ceiling adapt automatically to your physical stats.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = ageInput,
                            onValueChange = { ageInput = it },
                            label = { Text("Age") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = heightInput,
                            onValueChange = { heightInput = it },
                            label = { Text("Height (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            label = { Text("Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = targetWeightInput,
                            onValueChange = { targetWeightInput = it },
                            label = { Text("Goal (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Gender:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Gender.values().forEach { g ->
                            FilterChip(
                                selected = selectedGender == g,
                                onClick = { selectedGender = g },
                                label = { Text(g.label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Activity Level:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    ActivityLevel.values().take(3).forEach { act ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedActivity = act }
                                .padding(vertical = 3.dp)
                        ) {
                            RadioButton(selected = selectedActivity == act, onClick = { selectedActivity = act })
                            Text(act.label, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Goal:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    HealthGoal.values().forEach { goal ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedGoal = goal }
                                .padding(vertical = 3.dp)
                        ) {
                            RadioButton(selected = selectedGoal == goal, onClick = { selectedGoal = goal })
                            Text(goal.label, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val updated = userProfile.copy(
                                age = ageInput.toIntOrNull() ?: userProfile.age,
                                heightCm = heightInput.toDoubleOrNull() ?: userProfile.heightCm,
                                weightKg = weightInput.toDoubleOrNull() ?: userProfile.weightKg,
                                targetWeightKg = targetWeightInput.toDoubleOrNull() ?: userProfile.targetWeightKg,
                                gender = selectedGender,
                                activityLevel = selectedActivity,
                                goal = selectedGoal
                            )
                            viewModel.updateProfile(updated)
                            saveStatusMsg = "Metabolic targets recalculated successfully!"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                    ) {
                        Text("Recalculate & Save Targets", fontWeight = FontWeight.Bold)
                    }

                    saveStatusMsg?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = msg, fontSize = 12.sp, color = EmeraldDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Health Modes & Blood Glucose
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Specialized Health Tracking",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // GLP-1 Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("GLP-1 Companion Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Extra protein buffer, hydration tracker & dose logs", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = userProfile.isGlp1Mode,
                            onCheckedChange = { isEnabled ->
                                viewModel.updateProfile(userProfile.copy(isGlp1Mode = isEnabled))
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Diabetes Awareness Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Diabetes Risk Awareness", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Track added sugars & carb clustering (NOT a diagnosis)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = userProfile.isDiabetesAwareness,
                            onCheckedChange = { isEnabled ->
                                viewModel.updateProfile(userProfile.copy(isDiabetesAwareness = isEnabled))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showGlucoseDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Bloodtype, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Blood Glucose Entry (mg/dL)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // App Preferences (Language, Billing, Dark Mode)
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "App Preferences",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Language
                    val lang = SupportedLanguages.find { it.code == userProfile.languageCode } ?: SupportedLanguages.first()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showLanguageDialog = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(lang.flagEmoji, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Language", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                        Text("${lang.nativeName} (${lang.code.uppercase()})", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Billing & PayPal
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onNavigate(AppScreen.BILLING) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Subscription & Billing", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                        Text(if (userProfile.isPremium) "PRO" else "Free Tier", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Safety, Medical Disclaimer & Legal
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Safety & Legal Policies",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(onClick = { showDisclaimerDialog = true }) {
                        Text("⚠️ Mandatory Medical Disclaimer", color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                    }

                    TextButton(onClick = { showPrivacyDialog = true }) {
                        Text("Privacy Policy & Data Security")
                    }

                    TextButton(onClick = { showTermsDialog = true }) {
                        Text("Terms of Service")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Account & Reset All Data", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showLanguageDialog) {
        LanguageSelectorDialog(
            currentCode = userProfile.languageCode,
            onSelect = {
                viewModel.setLanguage(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showGlucoseDialog) {
        var glucoseValue by remember { mutableStateOf("105") }
        var timing by remember { mutableStateOf("Fasting") }

        AlertDialog(
            onDismissRequest = { showGlucoseDialog = false },
            title = { Text("Log Blood Glucose Reading") },
            text = {
                Column {
                    OutlinedTextField(
                        value = glucoseValue,
                        onValueChange = { glucoseValue = it },
                        label = { Text("Glucose (mg/dL)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Timing context:", fontSize = 12.sp)
                    listOf("Fasting", "Pre-Meal", "Post-Meal (1h)", "Post-Meal (2h)").forEach { t ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { timing = t }
                        ) {
                            RadioButton(selected = timing == t, onClick = { timing = t })
                            Text(t, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val v = glucoseValue.toDoubleOrNull() ?: 100.0
                    viewModel.logGlucose(v, timing)
                    showGlucoseDialog = false
                }) {
                    Text("Save Reading")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGlucoseDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showDisclaimerDialog = false },
            title = { Text("Medical Disclaimer") },
            text = {
                Text(
                    text = "Caly is an informational nutrition tracking tool and AI lifestyle companion. It is NOT a medical device and does NOT provide medical advice, diagnosis, or treatment. Never disregard professional medical advice or delay seeking it because of something you read in Caly. For any glucose irregularities or GLP-1 side effects, always consult your licensed healthcare provider.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showDisclaimerDialog = false }) { Text("Understood") }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy") },
            text = {
                Text(
                    text = "Your health, nutrition, and weight data is stored securely on your device. We do not sell personal telemetry to third parties. Images analyzed by Gemini Vision are processed transiently for nutritional computation and are not retained for profiling.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("Close") }
            }
        )
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Terms of Service") },
            text = {
                Text(
                    text = "By using Caly, you agree to our fair use policies. Subscriptions renew automatically every 30 days unless cancelled through the Billing screen or your PayPal account management panel.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) { Text("Close") }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Permanently Delete Account?") },
            text = { Text("This will erase all logged meals, weight entries, GLP-1 injection logs, and reset Caly to factory state.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAll()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
}
