package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class MealType(val displayName: String, val iconName: String) {
    BREAKFAST("Breakfast", "wb_sunny"),
    LUNCH("Lunch", "restaurant"),
    DINNER("Dinner", "dinner_dining"),
    SNACK("Snack", "coffee")
}

data class MealItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val mealType: MealType,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double = 0.0,
    val sugar: Double = 0.0,
    val portionGrams: Double = 100.0,
    val portionUnit: String = "serving",
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    val photoUri: String? = null,
    val notes: String = "",
    val isFavorite: Boolean = false
)

enum class Gender(val label: String) {
    MALE("Male"),
    FEMALE("Female"),
    OTHER("Other")
}

enum class ActivityLevel(val label: String, val multiplier: Double) {
    SEDENTARY("Sedentary (office job, little exercise)", 1.2),
    LIGHT("Lightly Active (1-3 days/wk)", 1.375),
    MODERATE("Moderately Active (3-5 days/wk)", 1.55),
    VERY_ACTIVE("Very Active (6-7 days/wk)", 1.725),
    EXTRA_ACTIVE("Extra Active (physical job / 2x train)", 1.9)
}

enum class HealthGoal(val label: String, val calorieAdjustment: Int) {
    LOSE_FAST("Lose weight (-500 kcal/day)", -500),
    LOSE_STEADY("Gentle loss (-250 kcal/day)", -250),
    MAINTAIN("Maintain weight (balanced)", 0),
    GAIN_LEAN("Lean bulk (+250 kcal/day)", 250),
    GAIN_BUILD("Muscle gain (+500 kcal/day)", 500)
}

enum class DietaryPreference(val label: String) {
    BALANCED("Balanced"),
    HIGH_PROTEIN("High Protein"),
    LOW_CARB("Low Carb / Keto"),
    MEDITERRANEAN("Mediterranean"),
    VEGAN("Vegan"),
    VEGETARIAN("Vegetarian"),
    DIABETIC_FRIENDLY("Diabetic-Friendly (Low Sugar)"),
    GLP1_GENTLE("GLP-1 Gentle (Digestive Ease)")
}

data class UserProfile(
    val name: String = "Health Seeker",
    val age: Int = 30,
    val gender: Gender = Gender.FEMALE,
    val heightCm: Double = 170.0,
    val weightKg: Double = 72.0,
    val targetWeightKg: Double = 65.0,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val goal: HealthGoal = HealthGoal.LOSE_STEADY,
    val dietaryPreference: DietaryPreference = DietaryPreference.BALANCED,
    val calorieTarget: Int = 2000,
    val proteinTargetGrams: Int = 130,
    val carbsTargetGrams: Int = 180,
    val fatTargetGrams: Int = 60,
    val fiberTargetGrams: Int = 30,
    val sugarLimitGrams: Int = 35,
    val waterTargetMl: Int = 2500,
    // GLP-1 Mode features
    val isGlp1Mode: Boolean = false,
    val glp1Medication: String = "Semaglutide (Ozempic/Wegovy)",
    val glp1Dose: String = "0.5 mg",
    val glp1DayOfWeek: Int = 1, // 1 = Monday
    val lastGlp1InjectionDate: String = "",
    // Diabetes Risk features
    val isDiabetesAwareness: Boolean = true,
    // App settings
    val languageCode: String = "en",
    val isDarkMode: Boolean = false,
    val isPremium: Boolean = false,
    val streakDays: Int = 5,
    val totalMealsLogged: Int = 24
)

data class Glp1Injection(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    val medicationName: String = "Semaglutide",
    val doseMg: String = "0.5 mg",
    val injectionSite: String = "Abdomen (Left)",
    val sideEffects: List<String> = emptyList(),
    val appetiteLevel: Int = 3, // 1 (none) to 5 (high)
    val notes: String = ""
)

data class GlucoseLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    val valueMgDl: Double,
    val timing: String = "Fasting", // Fasting, Pre-Meal, Post-Meal (1h), Post-Meal (2h), Bedtime
    val notes: String = ""
)

data class WeightLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    val weightKg: Double,
    val notes: String = ""
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isFromUser: Boolean = false,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val suggestionChips: List<String> = emptyList()
)

data class Recipe(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val prepTimeMinutes: Int,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double = 0.0,
    val sugar: Double = 0.0,
    val ingredients: List<String>,
    val instructions: List<String>,
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false
)

data class AppLanguage(
    val code: String,
    val nameEnglish: String,
    val nativeName: String,
    val flagEmoji: String,
    val isRtl: Boolean = false
)

val SupportedLanguages = listOf(
    AppLanguage("en", "English", "English", "🇺🇸", false),
    AppLanguage("zh", "Chinese", "中文", "🇨🇳", false),
    AppLanguage("hi", "Hindi", "हिन्दी", "🇮🇳", false),
    AppLanguage("es", "Spanish", "Español", "🇪🇸", false),
    AppLanguage("fr", "French", "Français", "🇫🇷", false),
    AppLanguage("ar", "Arabic", "العربية", "🇸🇦", true),
    AppLanguage("bn", "Bengali", "বাংলা", "🇧🇩", false),
    AppLanguage("pt", "Portuguese", "Português", "🇧🇷", false),
    AppLanguage("ru", "Russian", "Русский", "🇷🇺", false),
    AppLanguage("ur", "Urdu", "اردو", "🇵🇰", true)
)
