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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.example.data.model.Glp1Injection
import com.example.data.model.UserProfile
import com.example.ui.theme.CyanTertiary
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.MacroProtein
import com.example.viewmodel.AppScreen
import com.example.viewmodel.CalyViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Glp1Screen(
    viewModel: CalyViewModel,
    userProfile: UserProfile,
    onNavigate: (AppScreen) -> Unit
) {
    val glp1Logs by viewModel.glp1Logs.collectAsState()
    val waterIntake by viewModel.waterIntakeMl.collectAsState()

    var showLogInjectionDialog by remember { mutableStateOf(false) }
    var selectedSideEffects by remember { mutableStateOf(setOf<String>()) }
    var exportSuccessMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("glp1_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header & Mode Status
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
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(CyanTertiary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "💉", fontSize = 22.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "GLP-1 Companion Mode",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${userProfile.glp1Medication} • ${userProfile.glp1Dose}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = userProfile.isGlp1Mode,
                            onCheckedChange = { isEnabled ->
                                viewModel.updateProfile(userProfile.copy(isGlp1Mode = isEnabled))
                            }
                        )
                    }

                    if (userProfile.isGlp1Mode) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Next Injection Due: Monday (in 3 days)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E40AF)
                                    )
                                    Text(
                                        text = "Last logged: ${userProfile.lastGlp1InjectionDate.ifBlank { "Recently" }}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF3B82F6)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Daily GLP-1 Protections (Protein & Water Focus)
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Essential GLP-1 Daily Targets",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "GLP-1 medications reduce overall appetite. Caly automatically raises your protein and hydration targets to protect lean muscle mass and prevent gastric slowing.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // High Protein Target Card
                        Surface(
                            color = MacroProtein.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "💪 Muscle Shield", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MacroProtein)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "${userProfile.proteinTargetGrams}g Protein", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                Text(text = "1.5g per kg target", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Hydration Target Card
                        Surface(
                            color = CyanTertiary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "💧 Hydration Floor", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanTertiary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "$waterIntake / ${userProfile.waterTargetMl}ml", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                Text(text = "+600ml GLP-1 buffer", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Side-Effect Check-in Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Side-Effect & Appetite Check-In",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap any symptoms experienced today. Caly adapts meal recommendations accordingly.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val sideEffectsList = listOf(
                        "Mild Nausea", "Fatigue", "Low Appetite", "Acid Reflux", "Constipation", "Feeling Great"
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        sideEffectsList.forEach { symptom ->
                            val isSelected = selectedSideEffects.contains(symptom)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedSideEffects = if (isSelected) {
                                        selectedSideEffects - symptom
                                    } else {
                                        selectedSideEffects + symptom
                                    }
                                },
                                label = { Text(symptom, fontSize = 11.sp) }
                            )
                        }
                    }

                    if (selectedSideEffects.contains("Mild Nausea")) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💡 Caly Tip: Sip iced ginger tea or peppermint water, avoid greasy foods, and eat small handfuls of cold foods instead of hot heavy meals.",
                                fontSize = 11.sp,
                                color = Color(0xFF78350F),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Log Injection Action & History
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dose & Site Log (${glp1Logs.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                Button(
                    onClick = { showLogInjectionDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanTertiary),
                    modifier = Modifier.testTag("log_injection_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Injection", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(glp1Logs, key = { it.id }) { log ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${log.medicationName} • ${log.doseMg}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Site: ${log.injectionSite} • ${log.dateString}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Administered",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Export PDF for Doctor
        item {
            OutlinedButton(
                onClick = {
                    exportSuccessMessage = "GLP-1 clinical summary generated: includes weight trajectory, injection sites, protein adherence, and side-effects ready for your prescribing physician."
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("doctor_export_btn"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Doctor's Visit Summary (PDF)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            exportSuccessMessage?.let { msg ->
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

    // Log Injection Modal
    if (showLogInjectionDialog) {
        var medName by remember { mutableStateOf("Semaglutide") }
        var dose by remember { mutableStateOf("0.5 mg") }
        var site by remember { mutableStateOf("Abdomen (Left)") }

        AlertDialog(
            onDismissRequest = { showLogInjectionDialog = false },
            title = { Text("Log GLP-1 Injection") },
            text = {
                Column {
                    Text("Medication:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    listOf("Semaglutide (Ozempic/Wegovy)", "Tirzepatide (Mounjaro/Zepbound)").forEach { m ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { medName = m.split(" ").first() }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = medName == m.split(" ").first(), onClick = { medName = m.split(" ").first() })
                            Text(m, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Injection Site:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    listOf("Abdomen (Left)", "Abdomen (Right)", "Thigh (Left)", "Thigh (Right)", "Upper Arm").forEach { s ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { site = s }
                                .padding(vertical = 3.dp)
                        ) {
                            RadioButton(selected = site == s, onClick = { site = s })
                            Text(s, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val injection = Glp1Injection(
                            medicationName = medName,
                            doseMg = dose,
                            injectionSite = site,
                            sideEffects = selectedSideEffects.toList()
                        )
                        viewModel.logGlp1(injection)
                        showLogInjectionDialog = false
                    }
                ) {
                    Text("Save Injection")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogInjectionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
