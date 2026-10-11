package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalyRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("caly_health_data", Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(loadUserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _meals = MutableStateFlow(loadMeals())
    val meals: StateFlow<List<MealItem>> = _meals.asStateFlow()

    private val _waterIntakeMl = MutableStateFlow(prefs.getInt("water_${getTodayDate()}", 1250))
    val waterIntakeMl: StateFlow<Int> = _waterIntakeMl.asStateFlow()

    private val _weightLogs = MutableStateFlow(loadWeightLogs())
    val weightLogs: StateFlow<List<WeightLog>> = _weightLogs.asStateFlow()

    private val _glp1Logs = MutableStateFlow(loadGlp1Logs())
    val glp1Logs: StateFlow<List<Glp1Injection>> = _glp1Logs.asStateFlow()

    private val _glucoseLogs = MutableStateFlow(loadGlucoseLogs())
    val glucoseLogs: StateFlow<List<GlucoseLog>> = _glucoseLogs.asStateFlow()

    private val _chatMessages = MutableStateFlow(loadChatMessages())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _recipes = MutableStateFlow(loadRecipes())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    // PayPal & Subscription state
    private val _isPremium = MutableStateFlow(prefs.getBoolean("is_premium", false))
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    companion object {
        fun getTodayDate(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        fun getYesterdayDate(): String {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -1)
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        }
    }

    fun getTodayMeals(): List<MealItem> {
        val today = getTodayDate()
        return _meals.value.filter { it.dateString == today }
    }

    fun getTodayMealCount(): Int {
        return getTodayMeals().size
    }

    fun canLogMeal(): Boolean {
        if (_isPremium.value) return true
        return getTodayMealCount() < 3
    }

    fun addMeal(meal: MealItem): Boolean {
        if (!canLogMeal()) {
            return false // Free plan limit reached!
        }
        val updated = listOf(meal) + _meals.value
        _meals.value = updated
        saveMeals(updated)

        // Increment streak if not logged today yet
        val prevTodayCount = _meals.value.count { it.dateString == meal.dateString }
        if (prevTodayCount == 1) {
            updateStreak()
        }
        return true
    }

    fun deleteMeal(mealId: String) {
        val updated = _meals.value.filterNot { it.id == mealId }
        _meals.value = updated
        saveMeals(updated)
    }

    fun updateMeal(updatedMeal: MealItem) {
        val updated = _meals.value.map { if (it.id == updatedMeal.id) updatedMeal else it }
        _meals.value = updated
        saveMeals(updated)
    }

    fun logWater(deltaMl: Int) {
        val todayKey = "water_${getTodayDate()}"
        val newTotal = (_waterIntakeMl.value + deltaMl).coerceAtLeast(0)
        _waterIntakeMl.value = newTotal
        prefs.edit().putInt(todayKey, newTotal).apply()
    }

    fun logWeight(kg: Double, notes: String = "") {
        val entry = WeightLog(
            timestamp = System.currentTimeMillis(),
            dateString = getTodayDate(),
            weightKg = kg,
            notes = notes
        )
        val updated = listOf(entry) + _weightLogs.value
        _weightLogs.value = updated
        saveWeightLogs(updated)

        // Update profile current weight
        updateUserProfile(_userProfile.value.copy(weightKg = kg))
    }

    fun logGlp1Injection(injection: Glp1Injection) {
        val updated = listOf(injection) + _glp1Logs.value
        _glp1Logs.value = updated
        saveGlp1Logs(updated)

        updateUserProfile(_userProfile.value.copy(lastGlp1InjectionDate = injection.dateString))
    }

    fun logGlucose(readingMgDl: Double, timing: String, notes: String = "") {
        val entry = GlucoseLog(
            timestamp = System.currentTimeMillis(),
            dateString = getTodayDate(),
            valueMgDl = readingMgDl,
            timing = timing,
            notes = notes
        )
        val updated = listOf(entry) + _glucoseLogs.value
        _glucoseLogs.value = updated
        saveGlucoseLogs(updated)
    }

    fun addChatMessage(msg: ChatMessage) {
        val updated = _chatMessages.value + msg
        _chatMessages.value = updated
        saveChatMessages(updated)
    }

    fun addRecipe(recipe: Recipe) {
        val updated = listOf(recipe) + _recipes.value
        _recipes.value = updated
        saveRecipes(updated)
    }

    fun toggleFavoriteRecipe(recipeId: String) {
        val updated = _recipes.value.map {
            if (it.id == recipeId) it.copy(isFavorite = !it.isFavorite) else it
        }
        _recipes.value = updated
        saveRecipes(updated)
    }

    fun setLanguage(langCode: String) {
        updateUserProfile(_userProfile.value.copy(languageCode = langCode))
    }

    fun setDarkMode(isDark: Boolean) {
        updateUserProfile(_userProfile.value.copy(isDarkMode = isDark))
    }

    fun setPremiumStatus(isPremium: Boolean) {
        _isPremium.value = isPremium
        prefs.edit().putBoolean("is_premium", isPremium).apply()
        updateUserProfile(_userProfile.value.copy(isPremium = isPremium))
    }

    fun updateUserProfile(profile: UserProfile) {
        // Calculate recommended calorie target based on Mifflin-St Jeor
        val calculated = recalculateTargets(profile)
        _userProfile.value = calculated
        saveUserProfile(calculated)
    }

    private fun recalculateTargets(p: UserProfile): UserProfile {
        // BMR calculation
        val bmr = if (p.gender == Gender.MALE) {
            (10.0 * p.weightKg) + (6.25 * p.heightCm) - (5.0 * p.age) + 5
        } else {
            (10.0 * p.weightKg) + (6.25 * p.heightCm) - (5.0 * p.age) - 161
        }
        val tdee = bmr * p.activityLevel.multiplier
        val targetKcal = (tdee + p.goal.calorieAdjustment).toInt().coerceIn(1200, 4500)

        // Protein targets: GLP-1 mode needs extra protein (1.4-1.6g/kg) to protect muscle mass
        val proteinGrams = if (p.isGlp1Mode) {
            (p.weightKg * 1.5).toInt().coerceIn(90, 220)
        } else {
            (targetKcal * 0.28 / 4.0).toInt().coerceIn(60, 200)
        }

        // Fat: ~25-28% of calories
        val fatGrams = (targetKcal * 0.26 / 9.0).toInt().coerceIn(35, 120)
        // Carbs: remainder
        val carbCalories = targetKcal - (proteinGrams * 4) - (fatGrams * 9)
        val carbGrams = (carbCalories / 4.0).toInt().coerceAtLeast(40)

        // Water target: 35ml per kg, +500ml in GLP-1 mode
        val waterMl = ((p.weightKg * 35) + if (p.isGlp1Mode) 600 else 0).toInt().coerceIn(2000, 4000)

        return p.copy(
            calorieTarget = targetKcal,
            proteinTargetGrams = proteinGrams,
            carbsTargetGrams = carbGrams,
            fatTargetGrams = fatGrams,
            waterTargetMl = waterMl,
            sugarLimitGrams = if (p.isDiabetesAwareness) 30 else 40
        )
    }

    private fun updateStreak() {
        val currentStreak = _userProfile.value.streakDays + 1
        updateUserProfile(_userProfile.value.copy(streakDays = currentStreak))
    }

    fun resetAllData() {
        prefs.edit().clear().apply()
        _userProfile.value = UserProfile()
        _meals.value = emptyList()
        _waterIntakeMl.value = 0
        _weightLogs.value = emptyList()
        _glp1Logs.value = emptyList()
        _glucoseLogs.value = emptyList()
        _chatMessages.value = emptyList()
        _isPremium.value = false
    }

    // --- Persistence helpers ---

    private fun loadUserProfile(): UserProfile {
        val jsonStr = prefs.getString("user_profile_json", null) ?: return recalculateTargets(UserProfile())
        return try {
            val obj = JSONObject(jsonStr)
            UserProfile(
                name = obj.optString("name", "Health Seeker"),
                age = obj.optInt("age", 30),
                gender = Gender.valueOf(obj.optString("gender", Gender.FEMALE.name)),
                heightCm = obj.optDouble("heightCm", 170.0),
                weightKg = obj.optDouble("weightKg", 72.0),
                targetWeightKg = obj.optDouble("targetWeightKg", 65.0),
                activityLevel = ActivityLevel.valueOf(obj.optString("activityLevel", ActivityLevel.MODERATE.name)),
                goal = HealthGoal.valueOf(obj.optString("goal", HealthGoal.LOSE_STEADY.name)),
                dietaryPreference = DietaryPreference.valueOf(obj.optString("dietaryPreference", DietaryPreference.BALANCED.name)),
                calorieTarget = obj.optInt("calorieTarget", 1950),
                proteinTargetGrams = obj.optInt("proteinTargetGrams", 130),
                carbsTargetGrams = obj.optInt("carbsTargetGrams", 180),
                fatTargetGrams = obj.optInt("fatTargetGrams", 58),
                fiberTargetGrams = obj.optInt("fiberTargetGrams", 30),
                sugarLimitGrams = obj.optInt("sugarLimitGrams", 32),
                waterTargetMl = obj.optInt("waterTargetMl", 2500),
                isGlp1Mode = obj.optBoolean("isGlp1Mode", false),
                glp1Medication = obj.optString("glp1Medication", "Semaglutide (Ozempic/Wegovy)"),
                glp1Dose = obj.optString("glp1Dose", "0.5 mg"),
                glp1DayOfWeek = obj.optInt("glp1DayOfWeek", 1),
                lastGlp1InjectionDate = obj.optString("lastGlp1InjectionDate", ""),
                isDiabetesAwareness = obj.optBoolean("isDiabetesAwareness", true),
                languageCode = obj.optString("languageCode", "en"),
                isDarkMode = obj.optBoolean("isDarkMode", false),
                isPremium = prefs.getBoolean("is_premium", false),
                streakDays = obj.optInt("streakDays", 5),
                totalMealsLogged = obj.optInt("totalMealsLogged", 24)
            )
        } catch (e: Exception) {
            recalculateTargets(UserProfile())
        }
    }

    private fun saveUserProfile(profile: UserProfile) {
        val obj = JSONObject().apply {
            put("name", profile.name)
            put("age", profile.age)
            put("gender", profile.gender.name)
            put("heightCm", profile.heightCm)
            put("weightKg", profile.weightKg)
            put("targetWeightKg", profile.targetWeightKg)
            put("activityLevel", profile.activityLevel.name)
            put("goal", profile.goal.name)
            put("dietaryPreference", profile.dietaryPreference.name)
            put("calorieTarget", profile.calorieTarget)
            put("proteinTargetGrams", profile.proteinTargetGrams)
            put("carbsTargetGrams", profile.carbsTargetGrams)
            put("fatTargetGrams", profile.fatTargetGrams)
            put("fiberTargetGrams", profile.fiberTargetGrams)
            put("sugarLimitGrams", profile.sugarLimitGrams)
            put("waterTargetMl", profile.waterTargetMl)
            put("isGlp1Mode", profile.isGlp1Mode)
            put("glp1Medication", profile.glp1Medication)
            put("glp1Dose", profile.glp1Dose)
            put("glp1DayOfWeek", profile.glp1DayOfWeek)
            put("lastGlp1InjectionDate", profile.lastGlp1InjectionDate)
            put("isDiabetesAwareness", profile.isDiabetesAwareness)
            put("languageCode", profile.languageCode)
            put("isDarkMode", profile.isDarkMode)
            put("streakDays", profile.streakDays)
            put("totalMealsLogged", profile.totalMealsLogged)
        }
        prefs.edit().putString("user_profile_json", obj.toString()).apply()
    }

    private fun loadMeals(): List<MealItem> {
        val jsonStr = prefs.getString("meals_json", null)
        if (jsonStr.isNullOrBlank()) {
            return generateInitialSampleMeals()
        }
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<MealItem>()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list.add(
                    MealItem(
                        id = o.optString("id"),
                        name = o.optString("name"),
                        mealType = MealType.valueOf(o.optString("mealType")),
                        calories = o.optInt("calories"),
                        protein = o.optDouble("protein"),
                        carbs = o.optDouble("carbs"),
                        fat = o.optDouble("fat"),
                        fiber = o.optDouble("fiber", 0.0),
                        sugar = o.optDouble("sugar", 0.0),
                        portionGrams = o.optDouble("portionGrams", 100.0),
                        portionUnit = o.optString("portionUnit", "serving"),
                        timestamp = o.optLong("timestamp"),
                        dateString = o.optString("dateString"),
                        notes = o.optString("notes", ""),
                        isFavorite = o.optBoolean("isFavorite", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            generateInitialSampleMeals()
        }
    }

    private fun saveMeals(meals: List<MealItem>) {
        val array = JSONArray()
        for (m in meals) {
            val o = JSONObject().apply {
                put("id", m.id)
                put("name", m.name)
                put("mealType", m.mealType.name)
                put("calories", m.calories)
                put("protein", m.protein)
                put("carbs", m.carbs)
                put("fat", m.fat)
                put("fiber", m.fiber)
                put("sugar", m.sugar)
                put("portionGrams", m.portionGrams)
                put("portionUnit", m.portionUnit)
                put("timestamp", m.timestamp)
                put("dateString", m.dateString)
                put("notes", m.notes)
                put("isFavorite", m.isFavorite)
            }
            array.put(o)
        }
        prefs.edit().putString("meals_json", array.toString()).apply()
    }

    private fun loadWeightLogs(): List<WeightLog> {
        val json = prefs.getString("weights_json", null)
        if (json.isNullOrBlank()) {
            return listOf(
                WeightLog(dateString = getTodayDate(), weightKg = 72.0),
                WeightLog(dateString = getYesterdayDate(), weightKg = 72.3),
                WeightLog(dateString = "2026-10-06", weightKg = 72.8),
                WeightLog(dateString = "2026-10-01", weightKg = 73.5)
            )
        }
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<WeightLog>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(WeightLog(o.getString("id"), o.getLong("timestamp"), o.getString("dateString"), o.getDouble("weightKg"), o.optString("notes")))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveWeightLogs(list: List<WeightLog>) {
        val arr = JSONArray()
        for (w in list) {
            arr.put(JSONObject().apply {
                put("id", w.id)
                put("timestamp", w.timestamp)
                put("dateString", w.dateString)
                put("weightKg", w.weightKg)
                put("notes", w.notes)
            })
        }
        prefs.edit().putString("weights_json", arr.toString()).apply()
    }

    private fun loadGlp1Logs(): List<Glp1Injection> {
        val json = prefs.getString("glp1_json", null)
        if (json.isNullOrBlank()) {
            return listOf(
                Glp1Injection(
                    dateString = getYesterdayDate(),
                    medicationName = "Semaglutide",
                    doseMg = "0.5 mg",
                    injectionSite = "Abdomen (Left)",
                    sideEffects = listOf("Mild early satiety", "Slight thirst"),
                    appetiteLevel = 2,
                    notes = "Dose 3 completed on schedule"
                )
            )
        }
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<Glp1Injection>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val seArr = o.optJSONArray("sideEffects") ?: JSONArray()
                val seList = mutableListOf<String>()
                for (j in 0 until seArr.length()) seList.add(seArr.getString(j))

                list.add(
                    Glp1Injection(
                        id = o.getString("id"),
                        timestamp = o.getLong("timestamp"),
                        dateString = o.getString("dateString"),
                        medicationName = o.getString("medicationName"),
                        doseMg = o.getString("doseMg"),
                        injectionSite = o.getString("injectionSite"),
                        sideEffects = seList,
                        appetiteLevel = o.optInt("appetiteLevel", 3),
                        notes = o.optString("notes", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveGlp1Logs(list: List<Glp1Injection>) {
        val arr = JSONArray()
        for (g in list) {
            arr.put(JSONObject().apply {
                put("id", g.id)
                put("timestamp", g.timestamp)
                put("dateString", g.dateString)
                put("medicationName", g.medicationName)
                put("doseMg", g.doseMg)
                put("injectionSite", g.injectionSite)
                val seArr = JSONArray()
                g.sideEffects.forEach { seArr.put(it) }
                put("sideEffects", seArr)
                put("appetiteLevel", g.appetiteLevel)
                put("notes", g.notes)
            })
        }
        prefs.edit().putString("glp1_json", arr.toString()).apply()
    }

    private fun loadGlucoseLogs(): List<GlucoseLog> {
        val json = prefs.getString("glucose_json", null)
        if (json.isNullOrBlank()) {
            return listOf(
                GlucoseLog(dateString = getTodayDate(), valueMgDl = 95.0, timing = "Fasting", notes = "Normal baseline"),
                GlucoseLog(dateString = getYesterdayDate(), valueMgDl = 124.0, timing = "Post-Meal 2h", notes = "After lunch")
            )
        }
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<GlucoseLog>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(GlucoseLog(o.getString("id"), o.getLong("timestamp"), o.getString("dateString"), o.getDouble("valueMgDl"), o.getString("timing"), o.optString("notes")))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveGlucoseLogs(list: List<GlucoseLog>) {
        val arr = JSONArray()
        for (g in list) {
            arr.put(JSONObject().apply {
                put("id", g.id)
                put("timestamp", g.timestamp)
                put("dateString", g.dateString)
                put("valueMgDl", g.valueMgDl)
                put("timing", g.timing)
                put("notes", g.notes)
            })
        }
        prefs.edit().putString("glucose_json", arr.toString()).apply()
    }

    private fun loadChatMessages(): List<ChatMessage> {
        return listOf(
            ChatMessage(
                isFromUser = false,
                text = "Hello! I'm Caly, your personal AI health agent. I'm connected to your meal logs, macro targets, and wellness goals. How can I help you thrive today?",
                suggestionChips = listOf(
                    "Review today's meals",
                    "What should I eat for dinner?",
                    "How to manage blood sugar spikes?",
                    "GLP-1 protein advice"
                )
            )
        )
    }

    private fun saveChatMessages(list: List<ChatMessage>) {
        // Keeps state alive during session
    }

    private fun loadRecipes(): List<Recipe> {
        return listOf(
            Recipe(
                title = "High-Protein Greek Quinoa Bowl",
                description = "Crisp cucumber, cherry tomatoes, kalamata olives, grilled chicken breast, and creamy tzatziki over protein-packed quinoa.",
                prepTimeMinutes = 15,
                calories = 490,
                protein = 42.0,
                carbs = 38.0,
                fat = 16.0,
                fiber = 7.0,
                sugar = 4.0,
                ingredients = listOf(
                    "150g Grilled chicken breast (cubed)",
                    "1 cup Cooked quinoa",
                    "1/2 cup Diced cucumber & tomatoes",
                    "2 tbsp Feta cheese crumbles",
                    "2 tbsp Greek yogurt tzatziki"
                ),
                instructions = listOf(
                    "Layer warm cooked quinoa into your favorite bowl.",
                    "Arrange grilled chicken breast, cucumbers, and tomatoes on top.",
                    "Garnish with crumbles of feta and a spoonful of chilled tzatziki.",
                    "Finish with cracked black pepper and fresh oregano."
                ),
                tags = listOf("High Protein", "Mediterranean", "Metabolic Friendly"),
                isFavorite = true
            ),
            Recipe(
                title = "Avocado & Smoked Salmon Egg Scramble",
                description = "Omega-3 powerhouse breakfast with pasture-raised eggs, flaky wild smoked salmon, and ripe avocado.",
                prepTimeMinutes = 10,
                calories = 380,
                protein = 28.0,
                carbs = 6.0,
                fat = 26.0,
                fiber = 5.0,
                sugar = 1.0,
                ingredients = listOf(
                    "2 Pasture-raised large eggs",
                    "60g Wild smoked salmon",
                    "1/2 Ripe Hass avocado",
                    "Handful baby spinach",
                    "1 tsp Grass-fed butter"
                ),
                instructions = listOf(
                    "Whisk eggs with a pinch of sea salt and pepper.",
                    "Melt butter in a non-stick skillet over medium-low heat; add spinach until wilted.",
                    "Pour eggs and gently fold until soft curds form.",
                    "Top with smoked salmon ribbons and sliced avocado."
                ),
                tags = listOf("Keto", "High Protein", "Low Sugar"),
                isFavorite = false
            ),
            Recipe(
                title = "GLP-1 Gentle Ginger Lentil Soup",
                description = "Soothing, easily digestible vegetable broth infused with yellow lentils, fresh ginger, turmeric, and zucchini ribbons.",
                prepTimeMinutes = 22,
                calories = 320,
                protein = 20.0,
                carbs = 44.0,
                fat = 5.0,
                fiber = 11.0,
                sugar = 3.5,
                ingredients = listOf(
                    "1/2 cup Yellow split lentils (soaked)",
                    "2 cups Bone broth or vegetable broth",
                    "1 tbsp Minced fresh ginger root",
                    "1/2 tsp Ground turmeric",
                    "1 small Zucchini (spiralized or diced)"
                ),
                instructions = listOf(
                    "Simmer lentils in broth with ginger and turmeric for 15 minutes until tender.",
                    "Stir in zucchini for the final 3 minutes.",
                    "Blend half the soup for a silky soothing texture, leaving some texture.",
                    "Sip slowly warm for gentle metabolic comfort."
                ),
                tags = listOf("GLP-1 Gentle", "Gut Comfort", "High Fiber"),
                isFavorite = true
            )
        )
    }

    private fun saveRecipes(recipes: List<Recipe>) {
        // In-memory + future sync
    }

    private fun generateInitialSampleMeals(): List<MealItem> {
        val today = getTodayDate()
        val yesterday = getYesterdayDate()
        return listOf(
            MealItem(
                name = "Avocado Toast & Poached Eggs",
                mealType = MealType.BREAKFAST,
                calories = 420,
                protein = 24.0,
                carbs = 32.0,
                fat = 22.0,
                fiber = 7.0,
                sugar = 2.0,
                portionGrams = 220.0,
                portionUnit = "plate",
                dateString = today,
                notes = "Sourdough toast with chia seeds"
            ),
            MealItem(
                name = "Grilled Chicken Caesar Salad",
                mealType = MealType.LUNCH,
                calories = 540,
                protein = 46.0,
                carbs = 18.0,
                fat = 28.0,
                fiber = 5.0,
                sugar = 3.0,
                portionGrams = 350.0,
                portionUnit = "large bowl",
                dateString = today,
                notes = "Light parmesan & olive oil vinaigrette"
            ),
            // Yesterday's meals for immediate insights comparison
            MealItem(
                name = "Protein Berry Smoothie Bowl",
                mealType = MealType.BREAKFAST,
                calories = 380,
                protein = 32.0,
                carbs = 42.0,
                fat = 8.0,
                fiber = 9.0,
                sugar = 14.0,
                portionGrams = 300.0,
                portionUnit = "bowl",
                dateString = yesterday
            ),
            MealItem(
                name = "Turkey & Quinoa Wrap",
                mealType = MealType.LUNCH,
                calories = 490,
                protein = 38.0,
                carbs = 48.0,
                fat = 14.0,
                fiber = 6.0,
                sugar = 4.0,
                portionGrams = 280.0,
                portionUnit = "wrap",
                dateString = yesterday
            ),
            MealItem(
                name = "Baked Herb Cod with Green Beans",
                mealType = MealType.DINNER,
                calories = 460,
                protein = 44.0,
                carbs = 20.0,
                fat = 16.0,
                fiber = 7.0,
                sugar = 2.0,
                portionGrams = 320.0,
                portionUnit = "plate",
                dateString = yesterday
            )
        )
    }
}
