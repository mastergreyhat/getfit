package com.getfit

import com.getfit.data.FoodDatabase
import com.getfit.data.remote.SmartAiNutritionist
import com.getfit.data.util.FuzzySearchEngine
import org.junit.Assert.*
import org.junit.Test

class SmartAiNutritionistAndFoodTest {

    @Test
    fun testDefaultFoodsUnitsAreStrictlyGramsOrPieces() {
        val foods = FoodDatabase.getDefaultFoods()
        assertTrue("Default foods should not be empty", foods.isNotEmpty())

        for (food in foods) {
            val unit = food.servingUnit
            assertTrue(
                "Food item '${food.name}' has invalid unit '$unit'. Must be 'g' or 'piece'!",
                unit == "g" || unit == "piece"
            )
        }
    }

    @Test
    fun testCarrotExistsInDefaultDatabase() {
        val foods = FoodDatabase.getDefaultFoods()
        val carrotRaw = foods.find { it.name.contains("Carrot", ignoreCase = true) }
        assertNotNull("Carrot should exist in default foods database", carrotRaw)
        assertEquals("Vegetables & Paneer", carrotRaw?.category)
        assertTrue("Carrot calories should be ~41 kcal/100g", carrotRaw!!.caloriesPer100g in 35.0..45.0)
    }

    @Test
    fun testWheyProteinScoopMacrosAreAccurate() {
        val foods = FoodDatabase.getDefaultFoods()
        val wheyScoop = foods.find { it.name == "Whey Protein (1 Scoop / 30g)" }
        assertNotNull("Whey Protein (1 Scoop / 30g) must exist in database", wheyScoop)
        assertEquals("piece", wheyScoop?.servingUnit)
        assertEquals(120.0, wheyScoop?.caloriesPer100g ?: 0.0, 1.0)
        assertEquals(24.0, wheyScoop?.proteinPer100g ?: 0.0, 1.0)

        // Verify discrete calculation: 1 scoop = 1 piece = 120 kcal, 24.0g protein
        val qty = 1.0
        val totalCalories = (wheyScoop!!.caloriesPer100g * qty).toInt()
        val totalProtein = wheyScoop.proteinPer100g * qty
        assertEquals(120, totalCalories)
        assertEquals(24.0, totalProtein, 0.1)
    }

    @Test
    fun testSmartAiNutritionistTypoHandling() {
        val results = SmartAiNutritionist.analyzeFood("whay proteub")
        assertTrue("Should return matches for 'whay proteub'", results.isNotEmpty())
        val top = results.first()
        assertTrue("Match should be Whey Protein, but was ${top.name}", top.name.contains("Whey", ignoreCase = true))
        assertTrue("Protein should be >= 20g, but was ${top.proteinPer100g}", top.proteinPer100g >= 20.0)
    }

    @Test
    fun testSmartAiNutritionistCarrotSearch() {
        val results = SmartAiNutritionist.analyzeFood("carrot")
        assertTrue("Should return matches for 'carrot'", results.isNotEmpty())
        val carrot = results.find { it.name.contains("Carrot", ignoreCase = true) }
        assertNotNull("Should contain carrot", carrot)
        assertTrue("Carrot calories should be ~41 kcal/100g", carrot!!.caloriesPer100g in 20.0..45.0)
    }

    @Test
    fun testSmartAiNutritionistAmulWheyProtein() {
        val results = SmartAiNutritionist.analyzeFood("amul whey protein")
        assertTrue("Should return matches for 'amul whey protein'", results.isNotEmpty())
        val amul = results.find { it.name.contains("Amul", ignoreCase = true) }
        assertNotNull("Should find Amul Whey Protein", amul)
        assertEquals(24.0, amul!!.proteinPer100g, 1.0)
    }

    @Test
    fun testFuzzySearchTypoToleranceForCarrotAndWhey() {
        val foods = FoodDatabase.getDefaultFoods()
        val carrotMatches = FuzzySearchEngine.search("carot", foods)
        assertTrue("Fuzzy search should find Carrot for query 'carot'", carrotMatches.any { it.name.contains("Carrot", ignoreCase = true) })

        val wheyMatches = FuzzySearchEngine.search("whay proteub", foods)
        assertTrue("Fuzzy search should find Whey for 'whay proteub'", wheyMatches.any { it.name.contains("Whey", ignoreCase = true) })
    }

    @Test
    fun testMayonnaiseAccurateNutrition() {
        val results = SmartAiNutritionist.analyzeFood("mayonnaise")
        assertTrue("Should return matches for 'mayonnaise'", results.isNotEmpty())
        val mayo = results.first()
        assertTrue("Name should be Mayonnaise", mayo.name.contains("Mayonnaise", ignoreCase = true))
        assertEquals("Fat should be ~75g/100g, but was ${mayo.fatPer100g}", 75.0, mayo.fatPer100g, 5.0)
        assertTrue("Calories should be ~680 kcal/100g, but was ${mayo.caloriesPer100g}", mayo.caloriesPer100g > 500.0)
        assertNotEquals("Must NEVER return fake fallback 130 kcal!", 130.0, mayo.caloriesPer100g, 0.1)
    }

    @Test
    fun testHalwaAccurateNutrition() {
        val results = SmartAiNutritionist.analyzeFood("halwa")
        assertTrue("Should return matches for 'halwa'", results.isNotEmpty())
        val halwa = results.first()
        assertTrue("Name should contain Halwa", halwa.name.contains("Halwa", ignoreCase = true))
        assertTrue("Calories should be > 250 kcal/100g, but was ${halwa.caloriesPer100g}", halwa.caloriesPer100g > 250.0)
        assertTrue("Carbs should be > 30g/100g, but was ${halwa.carbsPer100g}", halwa.carbsPer100g > 30.0)
        assertNotEquals("Must NEVER return fake fallback 130 kcal!", 130.0, halwa.caloriesPer100g, 0.1)
    }

    @Test
    fun testPringlesAccurateNutrition() {
        val results = SmartAiNutritionist.analyzeFood("pringles")
        assertTrue("Should return matches for 'pringles'", results.isNotEmpty())
        val pringles = results.first()
        assertTrue("Name should contain Pringles", pringles.name.contains("Pringles", ignoreCase = true))
        assertEquals("Calories should be ~536 kcal/100g", 536.0, pringles.caloriesPer100g, 20.0)
        assertEquals("Fat should be ~34g/100g", 34.0, pringles.fatPer100g, 5.0)
    }

    @Test
    fun testGibberishQueryReturnsEmptyListNeverFakeMacros() {
        val gibberish1 = SmartAiNutritionist.analyzeFood("priglesonoppwe")
        assertTrue("Gibberish 'priglesonoppwe' must return ZERO results, but returned: ${gibberish1.map { it.name }}", gibberish1.isEmpty())

        val gibberish2 = SmartAiNutritionist.analyzeFood("dshhd")
        assertTrue("Gibberish 'dshhd' must return ZERO results", gibberish2.isEmpty())

        val gibberish3 = SmartAiNutritionist.analyzeFood("xyz123abc")
        assertTrue("Gibberish 'xyz123abc' must return ZERO results", gibberish3.isEmpty())
    }
}
