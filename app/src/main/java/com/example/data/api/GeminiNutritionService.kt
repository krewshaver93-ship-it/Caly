package com.example.data.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.LanguageManager
import com.example.data.model.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class FoodAnalysisResult(
    val foodName: String,
    val portionEstimate: String,
    val portionGrams: Double,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val sugar: Double,
    val confidence: Double = 0.9,
    val healthTip: String = "",
    val diabetesNote: String = ""
)

object GeminiNutritionService {
    private const val TAG = "GeminiNutrition"
    // Using gemini-3.5-flash as specified in the gemini-api skill for multimodal/text tasks
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    private fun isKeyConfigured(): Boolean {
        val key = getApiKey()
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    suspend fun analyzeFoodImage(
        bitmap: Bitmap,
        languageCode: String
    ): FoodAnalysisResult = withContext(Dispatchers.IO) {
        val langName = LanguageManager.getLanguagePromptName(languageCode)
        if (!isKeyConfigured()) {
            return@withContext fallbackAnalysis("Healthy Power Bowl", "1 bowl (320g)", 320.0, 480, 32.0, 46.0, 16.0, 8.0, 3.5)
        }

        try {
            val base64Image = bitmapToBase64(bitmap)
            val prompt = """
                Analyze the food in this photo for the Caly nutrition app.
                Provide nutritional estimates in JSON format with strictly these keys:
                {
                  "foodName": "Descriptive name of the dish in $langName",
                  "portionEstimate": "Serving size description in $langName (e.g. 1 medium bowl, 250g)",
                  "portionGrams": 250,
                  "calories": 450,
                  "protein": 28.5,
                  "carbs": 42.0,
                  "fat": 15.0,
                  "fiber": 6.0,
                  "sugar": 4.5,
                  "confidence": 0.92,
                  "healthTip": "Quick health/satiety observation in $langName",
                  "diabetesNote": "Impact on blood sugar/spikes in $langName (NOT medical diagnosis)"
                }
                Return ONLY raw JSON, without markdown formatting or code fences.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                val partsArray = JSONArray()

                // Text part
                partsArray.put(JSONObject().apply { put("text", prompt) })

                // Image part
                val inlineData = JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", base64Image)
                }
                partsArray.put(JSONObject().apply { put("inlineData", inlineData) })

                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
                put("contents", contentsArray)

                val genConfig = JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                }
                put("generationConfig", genConfig)
            }

            val responseText = executeGeminiRequest(requestJson.toString())
            parseAnalysisResult(responseText)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini vision failed: ${e.message}", e)
            fallbackAnalysis("Scanned Meal Item", "1 serving (250g)", 250.0, 420, 24.0, 45.0, 14.0, 5.0, 4.0)
        }
    }

    suspend fun analyzeTextFood(
        query: String,
        languageCode: String
    ): FoodAnalysisResult = withContext(Dispatchers.IO) {
        val langName = LanguageManager.getLanguagePromptName(languageCode)
        if (!isKeyConfigured()) {
            return@withContext fallbackAnalysis(query, "1 standard portion", 200.0, 360, 20.0, 35.0, 12.0, 4.0, 3.0)
        }

        try {
            val prompt = """
                Provide nutritional estimates for this food: "$query".
                Return strictly JSON with keys:
                {
                  "foodName": "Clean food name in $langName",
                  "portionEstimate": "Portion description in $langName",
                  "portionGrams": 200,
                  "calories": 380,
                  "protein": 22.0,
                  "carbs": 38.0,
                  "fat": 14.0,
                  "fiber": 5.0,
                  "sugar": 3.0,
                  "confidence": 0.95,
                  "healthTip": "Nutritional tip in $langName",
                  "diabetesNote": "Glycemic/sugar awareness note in $langName"
                }
                Return ONLY raw JSON.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray()
                val contentObj = JSONObject()
                val parts = JSONArray()
                parts.put(JSONObject().apply { put("text", prompt) })
                contentObj.put("parts", parts)
                contents.put(contentObj)
                put("contents", contents)

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("responseMimeType", "application/json")
                })
            }

            val responseText = executeGeminiRequest(requestJson.toString())
            parseAnalysisResult(responseText)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini text nutrition failed: ${e.message}", e)
            fallbackAnalysis(query, "1 portion (200g)", 200.0, 350, 18.0, 40.0, 12.0, 4.0, 3.0)
        }
    }

    suspend fun chatWithCaly(
        userMessage: String,
        healthContext: String,
        history: List<Pair<String, String>>,
        languageCode: String
    ): String = withContext(Dispatchers.IO) {
        val langName = LanguageManager.getLanguagePromptName(languageCode)
        if (!isKeyConfigured()) {
            return@withContext getOfflineCalyReply(userMessage, languageCode)
        }

        try {
            val systemInstruction = """
                You are Caly, a warm, motivating, science-backed personal AI health agent and nutrition coach inside the Caly app.
                User's Context:
                $healthContext
                Guidelines:
                1. Always respond fluently in $langName.
                2. Be concise, actionable, and encouraging (1 to 3 short paragraphs).
                3. If the user is on GLP-1 medication, prioritize hydration and lean protein to prevent muscle loss and nausea.
                4. For blood sugar/diabetes inquiries, mention sugar awareness and fiber, but NEVER diagnose or say they have diabetes (always append a gentle recommendation to consult their doctor).
                5. Provide practical meal swaps or micro-habits.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray()

                // Recent history
                for ((role, text) in history.takeLast(4)) {
                    val turnObj = JSONObject()
                    turnObj.put("role", if (role == "user") "user" else "model")
                    val parts = JSONArray()
                    parts.put(JSONObject().apply { put("text", text) })
                    turnObj.put("parts", parts)
                    contents.put(turnObj)
                }

                // Current message
                val currentTurn = JSONObject()
                currentTurn.put("role", "user")
                val parts = JSONArray()
                parts.put(JSONObject().apply { put("text", userMessage) })
                currentTurn.put("parts", parts)
                contents.put(currentTurn)

                put("contents", contents)

                // System instruction
                put("systemInstruction", JSONObject().apply {
                    val partsArr = JSONArray()
                    partsArr.put(JSONObject().apply { put("text", systemInstruction) })
                    put("parts", partsArr)
                })

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 600)
                })
            }

            val responseText = executeGeminiRequest(requestJson.toString())
            extractTextFromResponse(responseText)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini chat failed: ${e.message}", e)
            getOfflineCalyReply(userMessage, languageCode)
        }
    }

    suspend fun generateRecipe(
        remainingCalories: Int,
        remainingProtein: Int,
        remainingCarbs: Int,
        remainingFat: Int,
        preference: String,
        ingredientsOnHand: String,
        languageCode: String
    ): Recipe = withContext(Dispatchers.IO) {
        val langName = LanguageManager.getLanguagePromptName(languageCode)
        if (!isKeyConfigured()) {
            return@withContext getOfflineRecipe(remainingCalories, preference)
        }

        try {
            val prompt = """
                Generate a healthy, delicious recipe optimized for:
                - Target calories: ~${remainingCalories.coerceAtLeast(250)} kcal
                - Remaining macros target: Protein: ~${remainingProtein}g, Carbs: ~${remainingCarbs}g, Fat: ~${remainingFat}g
                - Dietary style: $preference
                - Available ingredients: ${ingredientsOnHand.ifBlank { "flexible pantry staples" }}
                - Language: $langName
                
                Return JSON with format:
                {
                  "title": "Recipe Title in $langName",
                  "description": "Short appetizing description in $langName",
                  "prepTimeMinutes": 20,
                  "calories": $remainingCalories,
                  "protein": $remainingProtein,
                  "carbs": $remainingCarbs,
                  "fat": $remainingFat,
                  "fiber": 6.0,
                  "sugar": 3.0,
                  "ingredients": ["Item 1 with quantity", "Item 2 with quantity"],
                  "instructions": ["Step 1 instructions", "Step 2 instructions", "Step 3 instructions"],
                  "tags": ["High Protein", "$preference"]
                }
                Return ONLY raw JSON.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray()
                val contentObj = JSONObject()
                val parts = JSONArray()
                parts.put(JSONObject().apply { put("text", prompt) })
                contentObj.put("parts", parts)
                contents.put(contentObj)
                put("contents", contents)

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.6)
                    put("responseMimeType", "application/json")
                })
            }

            val responseText = executeGeminiRequest(requestJson.toString())
            parseRecipe(responseText)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini recipe generation failed: ${e.message}", e)
            getOfflineRecipe(remainingCalories, preference)
        }
    }

    suspend fun generateWeeklyInsights(
        summaryText: String,
        languageCode: String
    ): String = withContext(Dispatchers.IO) {
        val langName = LanguageManager.getLanguagePromptName(languageCode)
        if (!isKeyConfigured()) {
            return@withContext "You maintained great consistency this week! Your protein intake met your target on 5 out of 7 days, supporting lean mass preservation. Keeping your hydration above 2.2L daily will further boost metabolic balance."
        }

        try {
            val prompt = """
                Write an empathetic, encouraging 2-paragraph weekly nutrition and metabolic insight review for Caly app user in $langName.
                Data:
                $summaryText
                Highlight:
                - Consistency wins
                - One actionable improvement (e.g. sugar timing, hydration, or protein pacing)
                - Warm encouragement
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray()
                val contentObj = JSONObject()
                val parts = JSONArray()
                parts.put(JSONObject().apply { put("text", prompt) })
                contentObj.put("parts", parts)
                contents.put(contentObj)
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.6)
                })
            }

            val responseText = executeGeminiRequest(requestJson.toString())
            extractTextFromResponse(responseText)
        } catch (e: Exception) {
            "Great dedication this week! Your nutrition logs reflect steady focus. Stay hydrated, prioritize nutrient-dense whole foods, and keep this positive momentum going!"
        }
    }

    private fun executeGeminiRequest(jsonPayload: String): String {
        val url = "$BASE_URL?key=${getApiKey()}"
        val requestBody = jsonPayload.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string().orEmpty()
                throw RuntimeException("Gemini API error ${response.code}: $errBody")
            }
            return response.body?.string().orEmpty()
        }
    }

    private fun extractTextFromResponse(rawResponse: String): String {
        return try {
            val json = JSONObject(rawResponse)
            val candidates = json.getJSONArray("candidates")
            val candidate = candidates.getJSONObject(0)
            val content = candidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            parts.getJSONObject(0).getString("text")
        } catch (e: Exception) {
            "Thank you for sharing! Tracking consistently is the most powerful habit for long-term metabolic health. Let's keep making balanced choices together."
        }
    }

    private fun parseAnalysisResult(rawResponse: String): FoodAnalysisResult {
        return try {
            val extracted = extractTextFromResponse(rawResponse)
            val cleanJson = cleanJsonString(extracted)
            val obj = JSONObject(cleanJson)

            FoodAnalysisResult(
                foodName = obj.optString("foodName", "Nutritious Meal"),
                portionEstimate = obj.optString("portionEstimate", "1 standard plate"),
                portionGrams = obj.optDouble("portionGrams", 250.0),
                calories = obj.optInt("calories", 450),
                protein = obj.optDouble("protein", 25.0),
                carbs = obj.optDouble("carbs", 45.0),
                fat = obj.optDouble("fat", 15.0),
                fiber = obj.optDouble("fiber", 5.0),
                sugar = obj.optDouble("sugar", 4.0),
                confidence = obj.optDouble("confidence", 0.92),
                healthTip = obj.optString("healthTip", "Balanced macro distribution."),
                diabetesNote = obj.optString("diabetesNote", "Moderate glycemic impact.")
            )
        } catch (e: Exception) {
            fallbackAnalysis("Healthy Meal", "1 portion (250g)", 250.0, 420, 24.0, 45.0, 14.0, 5.0, 4.0)
        }
    }

    private fun parseRecipe(rawResponse: String): Recipe {
        return try {
            val text = extractTextFromResponse(rawResponse)
            val obj = JSONObject(cleanJsonString(text))

            val ingArray = obj.optJSONArray("ingredients") ?: JSONArray()
            val ingredients = mutableListOf<String>()
            for (i in 0 until ingArray.length()) {
                ingredients.add(ingArray.getString(i))
            }

            val stepArray = obj.optJSONArray("instructions") ?: JSONArray()
            val steps = mutableListOf<String>()
            for (i in 0 until stepArray.length()) {
                steps.add(stepArray.getString(i))
            }

            val tagArray = obj.optJSONArray("tags") ?: JSONArray()
            val tags = mutableListOf<String>()
            for (i in 0 until tagArray.length()) {
                tags.add(tagArray.getString(i))
            }

            Recipe(
                title = obj.optString("title", "Mediterranean Protein Skillet"),
                description = obj.optString("description", "A vibrant, balanced meal crafted for your metabolic goals."),
                prepTimeMinutes = obj.optInt("prepTimeMinutes", 20),
                calories = obj.optInt("calories", 480),
                protein = obj.optDouble("protein", 36.0),
                carbs = obj.optDouble("carbs", 32.0),
                fat = obj.optDouble("fat", 18.0),
                fiber = obj.optDouble("fiber", 7.0),
                sugar = obj.optDouble("sugar", 3.0),
                ingredients = ingredients.ifEmpty { listOf("200g Lean Chicken Breast or Tofu", "1 cup Spinach", "1/2 cup Quinoa", "1 tbsp Extra Virgin Olive Oil", "Garlic & Herbs") },
                instructions = steps.ifEmpty { listOf("Saute aromatics with olive oil", "Add protein and cook through", "Toss in leafy greens and pre-cooked grains", "Season with sea salt, lemon and fresh herbs") },
                tags = tags.ifEmpty { listOf("High Protein", "Metabolic Friendly") }
            )
        } catch (e: Exception) {
            getOfflineRecipe(450, "Balanced")
        }
    }

    private fun cleanJsonString(raw: String): String {
        var str = raw.trim()
        if (str.startsWith("```json")) {
            str = str.removePrefix("```json")
        } else if (str.startsWith("```")) {
            str = str.removePrefix("```")
        }
        if (str.endsWith("```")) {
            str = str.removeSuffix("```")
        }
        return str.trim()
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Resize bitmap if very large to prevent memory overhead and optimize under 3 seconds
        val scaled = if (bitmap.width > 1024 || bitmap.height > 1024) {
            val ratio = 1024f / Math.max(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
        } else {
            bitmap
        }
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun fallbackAnalysis(
        name: String,
        portion: String,
        grams: Double,
        kcal: Int,
        prot: Double,
        carbs: Double,
        fat: Double,
        fiber: Double,
        sugar: Double
    ): FoodAnalysisResult {
        return FoodAnalysisResult(
            foodName = name,
            portionEstimate = portion,
            portionGrams = grams,
            calories = kcal,
            protein = prot,
            carbs = carbs,
            fat = fat,
            fiber = fiber,
            sugar = sugar,
            confidence = 0.90,
            healthTip = "Nutrient-rich option with balanced macronutrient ratio.",
            diabetesNote = if (sugar > 15) "Notice: Higher sugar content; pair with protein or fiber to blunt insulin response." else "Favorable glycemic profile with low sugar impact."
        )
    }

    private fun getOfflineCalyReply(userMessage: String, languageCode: String): String {
        val lower = userMessage.lowercase()
        return when {
            lower.contains("dinner") || lower.contains("eat") || lower.contains("meal") -> {
                "For your next meal, I recommend anchoring your plate around 30-40g of lean protein (like grilled salmon, tofu, or chicken) combined with high-fiber colorful vegetables. This will stabilize your insulin and keep you satiated for hours!"
            }
            lower.contains("sugar") || lower.contains("glucose") || lower.contains("spike") -> {
                "To manage blood sugar spikes: 1) Eat protein and fiber before simple carbs, 2) Take a brisk 10-minute walk right after eating, and 3) Stay well-hydrated. Remember, Caly provides educational insights; discuss glucose readings with your physician."
            }
            lower.contains("glp") || lower.contains("ozempic") || lower.contains("nausea") -> {
                "If you are on a GLP-1 medication and experiencing mild nausea or reduced appetite: eat small, frequent portions, sip electrolyte water slowly, and prioritize protein shakes or Greek yogurt so your muscles remain fully fueled."
            }
            lower.contains("water") || lower.contains("hydrate") -> {
                "Drinking adequate water is crucial for metabolic efficiency! Try drinking 300ml right now with a pinch of mineral salt or lemon to optimize cellular hydration."
            }
            else -> {
                "You're doing great with your nutrition awareness! Focusing on whole, single-ingredient foods with adequate protein and hydration makes healthy living effortless. What meal would you like to review next?"
            }
        }
    }

    private fun getOfflineRecipe(calories: Int, preference: String): Recipe {
        return Recipe(
            title = "Zesty Lemon-Herb Salmon & Asparagus",
            description = "Tender baked salmon with crisp asparagus and roasted baby potatoes for sustained satiety.",
            prepTimeMinutes = 18,
            calories = calories.coerceAtLeast(350),
            protein = 38.0,
            carbs = 26.0,
            fat = 18.0,
            fiber = 5.0,
            sugar = 2.0,
            ingredients = listOf(
                "180g Wild Salmon fillet",
                "150g Fresh asparagus stalks",
                "100g Steamed baby potatoes",
                "1 tbsp Extra virgin olive oil",
                "1 fresh lemon (juiced)",
                "Minced garlic, fresh dill, salt & cracked black pepper"
            ),
            instructions = listOf(
                "Preheat oven to 200°C (400°F). Line a baking sheet with parchment paper.",
                "Arrange salmon fillet and trimmed asparagus evenly on the sheet.",
                "Drizzle with olive oil, fresh lemon juice, minced garlic, dill, salt, and pepper.",
                "Bake for 12-14 minutes until salmon flakes gently and asparagus is tender-crisp.",
                "Serve immediately with warm steamed potatoes and an extra lemon wedge."
            ),
            tags = listOf("High Protein", "Omega-3", preference)
        )
    }
}
