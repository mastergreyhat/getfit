package com.getfit

import com.getfit.data.entity.FoodItem
import com.getfit.data.util.FuzzySearchEngine
import org.junit.Assert.*
import org.junit.Test

class FuzzySearchEngineTest {

    private val sampleFoods = listOf(
        FoodItem(id = 1, name = "Chicken Breast", category = "Non-Veg", caloriesPer100g = 165.0, proteinPer100g = 31.0, carbsPer100g = 0.0, fatPer100g = 3.6),
        FoodItem(id = 2, name = "Chicken 65", category = "Non-Veg", caloriesPer100g = 260.0, proteinPer100g = 22.0, carbsPer100g = 8.0, fatPer100g = 15.0),
        FoodItem(id = 3, name = "Paneer (Cottage Cheese)", category = "Vegetables & Paneer", caloriesPer100g = 265.0, proteinPer100g = 18.0, carbsPer100g = 3.0, fatPer100g = 20.0),
        FoodItem(id = 4, name = "Brown Rice (Cooked)", category = "Rice & Grains", caloriesPer100g = 111.0, proteinPer100g = 2.6, carbsPer100g = 23.0, fatPer100g = 0.9),
        FoodItem(id = 5, name = "Rolled Oats", category = "Rice & Grains", caloriesPer100g = 389.0, proteinPer100g = 16.9, carbsPer100g = 66.3, fatPer100g = 6.9),
        FoodItem(id = 6, name = "Whey Protein", category = "Protein Supplements", caloriesPer100g = 400.0, proteinPer100g = 80.0, carbsPer100g = 8.0, fatPer100g = 4.0),
        FoodItem(id = 7, name = "Chapati / Roti", category = "Indian Breads", caloriesPer100g = 264.0, proteinPer100g = 9.0, carbsPer100g = 55.0, fatPer100g = 3.0),
        FoodItem(id = 8, name = "Salmon Fillet", category = "Non-Veg", caloriesPer100g = 208.0, proteinPer100g = 20.0, carbsPer100g = 0.0, fatPer100g = 13.0)
    )

    @Test
    fun testDamerauLevenshteinDistance() {
        assertEquals(0, FuzzySearchEngine.damerauLevenshteinDistance("chicken", "chicken"))
        assertEquals(1, FuzzySearchEngine.damerauLevenshteinDistance("paner", "paneer")) // insertion
        assertEquals(1, FuzzySearchEngine.damerauLevenshteinDistance("whye", "whey"))   // transposition
        assertEquals(3, FuzzySearchEngine.damerauLevenshteinDistance("chkikn", "chicken"))
    }

    @Test
    fun testTypoMatches_Chicken() {
        val results = FuzzySearchEngine.search("chkikn", sampleFoods)
        assertTrue("Searching 'chkikn' should find Chicken items", results.isNotEmpty())
        assertTrue("Top result should contain chicken", results.first().name.contains("Chicken"))
    }

    @Test
    fun testTypoMatches_Paneer() {
        val results = FuzzySearchEngine.search("paner", sampleFoods)
        assertTrue("Searching 'paner' should find Paneer", results.isNotEmpty())
        assertEquals("Paneer (Cottage Cheese)", results.first().name)
    }

    @Test
    fun testTypoMatches_BrownRice() {
        val results = FuzzySearchEngine.search("brwn rice", sampleFoods)
        assertTrue("Searching 'brwn rice' should find Brown Rice", results.isNotEmpty())
        assertEquals("Brown Rice (Cooked)", results.first().name)
    }

    @Test
    fun testTypoMatches_Oats() {
        val results = FuzzySearchEngine.search("oates", sampleFoods)
        assertTrue("Searching 'oates' should find Rolled Oats", results.isNotEmpty())
        assertEquals("Rolled Oats", results.first().name)
    }

    @Test
    fun testTypoMatches_ChapatiTransliteration() {
        val results = FuzzySearchEngine.search("chappathi", sampleFoods)
        assertTrue("Searching 'chappathi' should find Chapati / Roti", results.isNotEmpty())
        assertEquals("Chapati / Roti", results.first().name)
    }

    @Test
    fun testDidYouMeanSuggestion() {
        val suggestion = FuzzySearchEngine.getDidYouMeanSuggestion("paner", sampleFoods)
        assertNotNull("Should provide suggestion for 'paner'", suggestion)
        assertTrue("Suggestion should be Paneer", suggestion!!.contains("Paneer"))

        // Exact query shouldn't have "did you mean"
        val exactSuggestion = FuzzySearchEngine.getDidYouMeanSuggestion("chicken", sampleFoods)
        assertNull("Exact match should not show did-you-mean", exactSuggestion)
    }
}
