package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.FoodAnalysisResult
import com.example.data.api.GeminiNutritionService
import com.example.data.model.*
import com.example.data.repository.CalyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppScreen {
    TODAY,
    CAMERA,
    INSIGHTS,
    RECIPES,
    CALY_CHAT,
    GLP1_MODE,
    SETTINGS,
    BILLING
}

class CalyViewModel(application: Application) : AndroidViewModel(application) {
    val repository = CalyRepository(application)

    val userProfile: StateFlow<UserProfile> = repository.userProfile
    val allMeals: StateFlow<List<MealItem>> = repository.meals
    val waterIntakeMl: StateFlow<Int> = repository.waterIntakeMl
    val weightLogs: StateFlow<List<WeightLog>> = repository.weightLogs
    val glp1Logs: StateFlow<List<Glp1Injection>> = repository.glp1Logs
    val glucoseLogs: StateFlow<List<GlucoseLog>> = repository.glucoseLogs
    val chatMessages: StateFlow<List<ChatMessage>> = repository.chatMessages
    val recipes: StateFlow<List<Recipe>> = repository.recipes
    val isPremium: StateFlow<Boolean> = repository.isPremium

    private val _currentScreen = MutableStateFlow(AppScreen.TODAY)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _navigationStack = MutableStateFlow<List<AppScreen>>(listOf(AppScreen.TODAY))

    private val _showPaywall = MutableStateFlow(false)
    val showPaywall: StateFlow<Boolean> = _showPaywall.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _scannedFoodResult = MutableStateFlow<FoodAnalysisResult?>(null)
    val scannedFoodResult: StateFlow<FoodAnalysisResult?> = _scannedFoodResult.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val _isRecipeLoading = MutableStateFlow(false)
    val isRecipeLoading: StateFlow<Boolean> = _isRecipeLoading.asStateFlow()

    private val _aiWeeklyReport = MutableStateFlow<String?>(null)
    val aiWeeklyReport: StateFlow<String?> = _aiWeeklyReport.asStateFlow()

    // Derived today's meals
    val todayMeals: StateFlow<List<MealItem>> = repository.meals.map { meals ->
        val today = CalyRepository.getTodayDate()
        meals.filter { it.dateString == today }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val todayCaloriesConsumed: StateFlow<Int> = todayMeals.map { list ->
        list.sumOf { it.calories }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val todayProteinConsumed: StateFlow<Double> = todayMeals.map { list ->
        list.sumOf { it.protein }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val todayCarbsConsumed: StateFlow<Double> = todayMeals.map { list ->
        list.sumOf { it.carbs }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val todayFatConsumed: StateFlow<Double> = todayMeals.map { list ->
        list.sumOf { it.fat }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val todayFiberConsumed: StateFlow<Double> = todayMeals.map { list ->
        list.sumOf { it.fiber }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val todaySugarConsumed: StateFlow<Double> = todayMeals.map { list ->
        list.sumOf { it.sugar }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value != screen) {
            _navigationStack.value = _navigationStack.value + screen
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        val stack = _navigationStack.value
        if (stack.size > 1) {
            val newStack = stack.dropLast(1)
            _navigationStack.value = newStack
            _currentScreen.value = newStack.last()
            return true
        } else if (_currentScreen.value != AppScreen.TODAY) {
            _currentScreen.value = AppScreen.TODAY
            _navigationStack.value = listOf(AppScreen.TODAY)
            return true
        }
        return false
    }

    fun logMeal(meal: MealItem): Boolean {
        val success = repository.addMeal(meal)
        if (!success) {
            _showPaywall.value = true
            return false
        }
        return true
    }

    fun deleteMeal(mealId: String) {
        repository.deleteMeal(mealId)
    }

    fun logWater(deltaMl: Int) {
        repository.logWater(deltaMl)
    }

    fun logWeight(kg: Double, notes: String = "") {
        repository.logWeight(kg, notes)
    }

    fun logGlp1(injection: Glp1Injection) {
        repository.logGlp1Injection(injection)
    }

    fun logGlucose(mgDl: Double, timing: String, notes: String = "") {
        repository.logGlucose(mgDl, timing, notes)
    }

    fun analyzePhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            val lang = userProfile.value.languageCode
            val result = GeminiNutritionService.analyzeFoodImage(bitmap, lang)
            _scannedFoodResult.value = result
            _isAnalyzing.value = false
        }
    }

    fun analyzeTextFood(query: String) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            val lang = userProfile.value.languageCode
            val result = GeminiNutritionService.analyzeTextFood(query, lang)
            _scannedFoodResult.value = result
            _isAnalyzing.value = false
        }
    }

    fun clearScannedResult() {
        _scannedFoodResult.value = null
    }

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return
        val userMsg = ChatMessage(isFromUser = true, text = userText)
        repository.addChatMessage(userMsg)

        viewModelScope.launch {
            _isChatLoading.value = true
            val profile = userProfile.value
            val todayKcal = todayCaloriesConsumed.value
            val targetKcal = profile.calorieTarget
            val mealsToday = todayMeals.value.joinToString { "${it.name} (${it.calories} kcal)" }

            val healthContext = """
                Profile: ${profile.name}, Age ${profile.age}, ${profile.gender.label}, Goal: ${profile.goal.label}
                Calories: Consumed $todayKcal / Target $targetKcal kcal
                Macros: Protein ${todayProteinConsumed.value.toInt()}g / ${profile.proteinTargetGrams}g, Carbs ${todayCarbsConsumed.value.toInt()}g, Fat ${todayFatConsumed.value.toInt()}g, Sugar ${todaySugarConsumed.value.toInt()}g (Limit ${profile.sugarLimitGrams}g)
                Hydration: ${waterIntakeMl.value}ml / ${profile.waterTargetMl}ml
                GLP-1 Mode: ${if (profile.isGlp1Mode) "ACTIVE with ${profile.glp1Medication} ${profile.glp1Dose}" else "OFF"}
                Meals today: ${if (mealsToday.isBlank()) "None logged yet" else mealsToday}
            """.trimIndent()

            val history = chatMessages.value.takeLast(6).map {
                (if (it.isFromUser) "user" else "model") to it.text
            }

            val reply = GeminiNutritionService.chatWithCaly(userText, healthContext, history, profile.languageCode)
            val calyMsg = ChatMessage(
                isFromUser = false,
                text = reply,
                suggestionChips = listOf("Track next meal", "Adjust protein goal", "Show recipe idea")
            )
            repository.addChatMessage(calyMsg)
            _isChatLoading.value = false
        }
    }

    fun generateRecipe(preference: String, ingredients: String) {
        viewModelScope.launch {
            _isRecipeLoading.value = true
            val profile = userProfile.value
            val remainingKcal = (profile.calorieTarget - todayCaloriesConsumed.value).coerceAtLeast(300)
            val remainingProt = (profile.proteinTargetGrams - todayProteinConsumed.value.toInt()).coerceAtLeast(20)
            val remainingCarb = (profile.carbsTargetGrams - todayCarbsConsumed.value.toInt()).coerceAtLeast(20)
            val remainingFat = (profile.fatTargetGrams - todayFatConsumed.value.toInt()).coerceAtLeast(10)

            val recipe = GeminiNutritionService.generateRecipe(
                remainingCalories = remainingKcal,
                remainingProtein = remainingProt,
                remainingCarbs = remainingCarb,
                remainingFat = remainingFat,
                preference = preference,
                ingredientsOnHand = ingredients,
                languageCode = profile.languageCode
            )
            repository.addRecipe(recipe)
            _isRecipeLoading.value = false
        }
    }

    fun logRecipeAsMeal(recipe: Recipe, mealType: MealType): Boolean {
        val meal = MealItem(
            name = recipe.title,
            mealType = mealType,
            calories = recipe.calories,
            protein = recipe.protein,
            carbs = recipe.carbs,
            fat = recipe.fat,
            fiber = recipe.fiber,
            sugar = recipe.sugar,
            portionGrams = 300.0,
            portionUnit = "recipe serving",
            notes = "AI Recipe: ${recipe.description}"
        )
        return logMeal(meal)
    }

    fun toggleFavoriteRecipe(id: String) {
        repository.toggleFavoriteRecipe(id)
    }

    fun requestWeeklyReport() {
        viewModelScope.launch {
            val summary = "Meals logged: ${allMeals.value.size}. Average calories: 1950. Protein goal hit on 5 days. Water intake avg 2.3L. Weight shift: -0.4 kg."
            val text = GeminiNutritionService.generateWeeklyInsights(summary, userProfile.value.languageCode)
            _aiWeeklyReport.value = text
        }
    }

    fun setLanguage(code: String) {
        repository.setLanguage(code)
    }

    fun setDarkMode(dark: Boolean) {
        repository.setDarkMode(dark)
    }

    fun updateProfile(profile: UserProfile) {
        repository.updateUserProfile(profile)
    }

    fun showPaywallModal() {
        _showPaywall.value = true
    }

    fun dismissPaywall() {
        _showPaywall.value = false
    }

    fun upgradeToPremium() {
        repository.setPremiumStatus(true)
        _showPaywall.value = false
    }

    fun cancelSubscription() {
        repository.setPremiumStatus(false)
    }

    fun resetAll() {
        repository.resetAllData()
        _currentScreen.value = AppScreen.TODAY
        _navigationStack.value = listOf(AppScreen.TODAY)
    }
}
