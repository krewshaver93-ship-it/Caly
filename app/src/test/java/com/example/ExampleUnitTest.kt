package com.example

import com.example.data.model.HealthGoal
import com.example.data.model.LanguageManager
import com.example.data.model.MealItem
import com.example.data.model.MealType
import com.example.data.model.SupportedLanguages
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testLanguageManager_translations() {
    assertEquals("Today", LanguageManager.getString("today", "en"))
    assertEquals("今日", LanguageManager.getString("today", "zh"))
    assertEquals("Hoy", LanguageManager.getString("today", "es"))
    assertEquals("اليوم", LanguageManager.getString("today", "ar"))
    assertTrue(LanguageManager.isRtl("ar"))
    assertTrue(LanguageManager.isRtl("ur"))
    assertFalse(LanguageManager.isRtl("en"))
    assertEquals(10, SupportedLanguages.size)
  }

  @Test
  fun testMealItem_properties() {
    val meal = MealItem(
      name = "Greek Yogurt",
      mealType = MealType.BREAKFAST,
      calories = 150,
      protein = 15.0,
      carbs = 8.0,
      fat = 2.0
    )
    assertEquals(150, meal.calories)
    assertEquals(15.0, meal.protein, 0.01)
    assertEquals(MealType.BREAKFAST, meal.mealType)
  }
}

