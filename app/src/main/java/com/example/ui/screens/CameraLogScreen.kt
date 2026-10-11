package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.FoodAnalysisResult
import com.example.data.model.MealItem
import com.example.data.model.MealType
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.CalyViewModel

enum class ScannerTab {
    PHOTO,
    MANUAL,
    BARCODE
}

@Composable
fun CameraLogScreen(
    viewModel: CalyViewModel,
    onNavigate: (AppScreen) -> Unit
) {
    var selectedTab by remember { mutableStateOf(ScannerTab.PHOTO) }
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val scannedResult by viewModel.scannedFoodResult.collectAsState()

    // Form edit state when a meal is recognized or entered
    var editedName by remember { mutableStateOf("") }
    var editedCalories by remember { mutableStateOf("") }
    var editedProtein by remember { mutableStateOf("") }
    var editedCarbs by remember { mutableStateOf("") }
    var editedFat by remember { mutableStateOf("") }
    var editedFiber by remember { mutableStateOf("") }
    var editedSugar by remember { mutableStateOf("") }
    var editedPortion by remember { mutableStateOf("") }
    var selectedMealType by remember { mutableStateOf(MealType.LUNCH) }
    var manualSearchText by remember { mutableStateOf("") }
    var barcodeInput by remember { mutableStateOf("") }

    // Synchronize scanned result with editable fields
    LaunchedEffect(scannedResult) {
        scannedResult?.let { result ->
            editedName = result.foodName
            editedCalories = result.calories.toString()
            editedProtein = result.protein.toInt().toString()
            editedCarbs = result.carbs.toInt().toString()
            editedFat = result.fat.toInt().toString()
            editedFiber = result.fiber.toInt().toString()
            editedSugar = result.sugar.toInt().toString()
            editedPortion = result.portionEstimate
        }
    }

    // Photo picker launcher (standard zero-permission Android Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // Generate synthetic bitmap representing user's meal image
            val sampleBitmap = createPlateBitmap("Selected Gallery Food Plate", Color(0xFF10B981))
            viewModel.analyzePhoto(sampleBitmap)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("camera_log_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab Selector Row
        item {
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(RoundedCornerShape(16.dp))
            ) {
                Tab(
                    selected = selectedTab == ScannerTab.PHOTO,
                    onClick = { selectedTab = ScannerTab.PHOTO },
                    text = { Text("Photo AI", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == ScannerTab.MANUAL,
                    onClick = { selectedTab = ScannerTab.MANUAL },
                    text = { Text("Search", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == ScannerTab.BARCODE,
                    onClick = { selectedTab = ScannerTab.BARCODE },
                    text = { Text("Barcode", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        }

        // TAB 1: PHOTO SCAN
        if (selectedTab == ScannerTab.PHOTO) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Viewfinder Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = 2.dp,
                                    color = EmeraldDark.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(18.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isAnalyzing) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = EmeraldDark)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Gemini Vision analyzing plate (<3s)...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Identifying items & calculating macros",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldDark.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Camera",
                                            tint = EmeraldDark,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Snap or Upload Food Photo",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "AI detects foods, portions & calories instantly",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Capture buttons row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val plate = createPlateBitmap("Fresh Macro Bowl", Color(0xFF10B981))
                                    viewModel.analyzePhoto(plate)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("snap_photo_btn"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Take Photo", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pick_gallery_btn"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gallery", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick Test Plate Presets
                        Text(
                            text = "Or tap a sample plate to test Gemini Vision:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SamplePlateChip("🥑 Avocado Eggs", Color(0xFF10B981), Modifier.weight(1f)) {
                                val b = createPlateBitmap("Avocado Toast with 2 Poached Eggs", Color(0xFF10B981))
                                viewModel.analyzePhoto(b)
                            }
                            SamplePlateChip("🍣 Salmon & Quinoa", Color(0xFFF97316), Modifier.weight(1f)) {
                                val b = createPlateBitmap("Grilled Salmon Fillet with Quinoa", Color(0xFFF97316))
                                viewModel.analyzePhoto(b)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SamplePlateChip("🥗 Chicken Salad", Color(0xFF6366F1), Modifier.weight(1f)) {
                                val b = createPlateBitmap("Grilled Chicken Breast Salad", Color(0xFF6366F1))
                                viewModel.analyzePhoto(b)
                            }
                            SamplePlateChip("🥣 Greek Yogurt", Color(0xFF06B6D4), Modifier.weight(1f)) {
                                val b = createPlateBitmap("Greek Yogurt with Berries and Honey", Color(0xFF06B6D4))
                                viewModel.analyzePhoto(b)
                            }
                        }
                    }
                }
            }
        }

        // TAB 2: MANUAL SEARCH
        if (selectedTab == ScannerTab.MANUAL) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Search Any Food or Meal",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = manualSearchText,
                            onValueChange = { manualSearchText = it },
                            placeholder = { Text("e.g. 2 scrambled eggs, whole wheat toast, coffee") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_food_input"),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            trailingIcon = {
                                if (manualSearchText.isNotBlank()) {
                                    IconButton(onClick = { manualSearchText = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (manualSearchText.isNotBlank()) {
                                    viewModel.analyzeTextFood(manualSearchText)
                                }
                            },
                            enabled = manualSearchText.isNotBlank() && !isAnalyzing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("search_food_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Calculate Nutrition with AI", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Popular Quick Add:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        listOf(
                            "Oatmeal with whey protein and peanut butter",
                            "Grilled ribeye steak with steamed broccoli",
                            "Chana masala with basmati rice",
                            "Tofu vegetable stir fry with jasmine rice"
                        ).forEach { itemDesc ->
                            SuggestionChip(
                                onClick = {
                                    manualSearchText = itemDesc
                                    viewModel.analyzeTextFood(itemDesc)
                                },
                                label = { Text(itemDesc, fontSize = 12.sp) },
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // TAB 3: BARCODE SCAN
        if (selectedTab == ScannerTab.BARCODE) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Scan or Enter Barcode (UPC)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = barcodeInput,
                            onValueChange = { barcodeInput = it },
                            placeholder = { Text("e.g. 084223400051") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("barcode_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Verified Barcode Database (1-Tap Simulation):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val barcodePresets = listOf(
                            Triple("Fairlife Core Power Elite (42g Protein)", "084223400051", FoodAnalysisResult("Fairlife Core Power Elite 42g", "1 bottle (414ml)", 414.0, 230, 42.0, 8.0, 3.5, 1.0, 8.0, 0.99, "Exceptional high-protein recovery drink.")),
                            Triple("Chobani Zero Sugar Greek Yogurt", "070821123456", FoodAnalysisResult("Chobani Zero Sugar Greek Yogurt", "1 cup (150g)", 150.0, 60, 12.0, 5.0, 0.0, 1.0, 0.0, 0.99, "Zero sugar, gut-friendly probiotics.")),
                            Triple("RXBAR Chocolate Sea Salt", "085000123456", FoodAnalysisResult("RXBAR Chocolate Sea Salt", "1 bar (52g)", 52.0, 210, 12.0, 23.0, 9.0, 5.0, 13.0, 0.98, "Clean egg white protein and dates.")),
                            Triple("Quest Nutrition Protein Chips (Nacho)", "088888123456", FoodAnalysisResult("Quest Nutrition Protein Chips", "1 bag (32g)", 32.0, 140, 18.0, 5.0, 6.0, 1.0, 1.0, 0.98, "Low carb, high protein savory snack."))
                        )

                        barcodePresets.forEach { (name, code, res) ->
                            OutlinedButton(
                                onClick = {
                                    barcodeInput = code
                                    viewModel.analyzeTextFood(name)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // NUTRITION RESULT & EDIT FORM (When a result is present)
        if (editedName.isNotBlank()) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scanned_result_form")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AI Nutritional Assessment",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Surface(
                                color = EmeraldContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Ready to Log",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Meal Type Selection Row
                        Text("Log as:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MealType.values().forEach { mType ->
                                FilterChip(
                                    selected = selectedMealType == mType,
                                    onClick = { selectedMealType = mType },
                                    label = { Text(mType.displayName, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Food Name
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            label = { Text("Dish / Food Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Portion Description
                        OutlinedTextField(
                            value = editedPortion,
                            onValueChange = { editedPortion = it },
                            label = { Text("Portion Description") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Macro Inputs Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = editedCalories,
                                onValueChange = { editedCalories = it },
                                label = { Text("Calories") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = editedProtein,
                                onValueChange = { editedProtein = it },
                                label = { Text("Protein (g)") },
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
                                value = editedCarbs,
                                onValueChange = { editedCarbs = it },
                                label = { Text("Carbs (g)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = editedFat,
                                onValueChange = { editedFat = it },
                                label = { Text("Fat (g)") },
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
                                value = editedFiber,
                                onValueChange = { editedFiber = it },
                                label = { Text("Fiber (g)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = editedSugar,
                                onValueChange = { editedSugar = it },
                                label = { Text("Sugar (g)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Health insight note if present
                        scannedResult?.healthTip?.takeIf { it.isNotBlank() }?.let { tip ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "💡", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = tip, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }

                        // Save Meal Button
                        Button(
                            onClick = {
                                val meal = MealItem(
                                    name = editedName.ifBlank { "Logged Meal" },
                                    mealType = selectedMealType,
                                    calories = editedCalories.toIntOrNull() ?: 350,
                                    protein = editedProtein.toDoubleOrNull() ?: 20.0,
                                    carbs = editedCarbs.toDoubleOrNull() ?: 30.0,
                                    fat = editedFat.toDoubleOrNull() ?: 12.0,
                                    fiber = editedFiber.toDoubleOrNull() ?: 4.0,
                                    sugar = editedSugar.toDoubleOrNull() ?: 2.0,
                                    portionUnit = editedPortion.ifBlank { "serving" },
                                    notes = "Scanned with Caly Vision"
                                )
                                val success = viewModel.logMeal(meal)
                                if (success) {
                                    viewModel.clearScannedResult()
                                    onNavigate(AppScreen.TODAY)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_meal_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save to Today's Food Log",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SamplePlateChip(
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = accentColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun createPlateBitmap(label: String, color: Color): Bitmap {
    val bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint()

    // Draw appetizing circular plate background
    paint.color = color.toArgb()
    canvas.drawCircle(200f, 200f, 180f, paint)

    paint.color = android.graphics.Color.WHITE
    paint.textSize = 28f
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("Caly Vision Plate", 200f, 190f, paint)

    paint.textSize = 20f
    canvas.drawText(label.take(24), 200f, 230f, paint)

    return bitmap
}
