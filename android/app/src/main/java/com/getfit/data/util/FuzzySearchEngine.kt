package com.getfit.data.util

import com.getfit.data.entity.FoodItem
import java.util.Locale

/**
 * High-performance, offline-first fuzzy search and typo tolerance engine.
 * Implements Damerau-Levenshtein distance, multi-token similarity matching,
 * and Indian/international food phonetic alias resolution.
 */
object FuzzySearchEngine {

    // Common spelling mistakes, phonetic variations, and regional language transliterations
    private val ALIAS_MAP = mapOf(
        "chkikn" to "chicken",
        "chikn" to "chicken",
        "chkn" to "chicken",
        "chiken" to "chicken",
        "paner" to "paneer",
        "panner" to "paneer",
        "panir" to "paneer",
        "brwn" to "brown",
        "oates" to "oats",
        "otmeal" to "oats",
        "oat" to "oats",
        "whye" to "whey",
        "whay" to "whey",
        "whe" to "whey",
        "protin" to "protein",
        "proten" to "protein",
        "proteub" to "protein",
        "carot" to "carrot",
        "carrots" to "carrot",
        "gajar" to "carrot",
        "tamatar" to "tomato",
        "tomatos" to "tomato",
        "pyaz" to "onion",
        "onions" to "onion",
        "aloo" to "potato",
        "potatos" to "potato",
        "anda" to "egg",
        "eggs" to "egg",
        "chapathi" to "chapati",
        "chappathi" to "chapati",
        "chappati" to "chapati",
        "roti" to "chapati",
        "phulka" to "chapati",
        "biriyani" to "biryani",
        "briyani" to "biryani",
        "dosai" to "dosa",
        "dhosa" to "dosa",
        "idly" to "idli",
        "iddli" to "idli",
        "porotta" to "parotta",
        "parota" to "parotta",
        "prata" to "parotta",
        "salmn" to "salmon",
        "samon" to "salmon",
        "yogert" to "curd",
        "yoghurt" to "curd",
        "dahi" to "curd",
        "badam" to "almond",
        "almonds" to "almond",
        "kaju" to "cashew",
        "cashews" to "cashew",
        "meen" to "fish",
        "machli" to "fish",
        "chawal" to "rice",
        "sadam" to "rice",
        "daal" to "dal",
        "dhal" to "dal",
        "paruppu" to "dal",
        "chilly" to "chilli",
        "mayo" to "mayonnaise",
        "mayonaise" to "mayonnaise",
        "mayonise" to "mayonnaise",
        "myo" to "mayonnaise",
        "halva" to "halwa",
        "helwa" to "halwa",
        "prigles" to "pringles",
        "pringels" to "pringles",
        "pringls" to "pringles",
        "kechup" to "ketchup",
        "catchup" to "ketchup",
        "ketchp" to "ketchup",
        "maggie" to "maggi",
        "shwarma" to "shawarma",
        "shawrma" to "shawarma",
        "gulabjamun" to "gulab jamun",
        "kajukatli" to "kaju katli",
        "rossogolla" to "rasgulla",
        "rasagola" to "rasgulla",
        "ladoo" to "laddu",
        "laddoo" to "laddu",
        "jelebi" to "jalebi",
        "pavbhaji" to "pav bhaji",
        "paav bhaji" to "pav bhaji",
        "vadapav" to "vada pav",
        "wada pav" to "vada pav",
        "panipuri" to "pani puri",
        "golgappa" to "pani puri",
        "golgappe" to "pani puri"
    )

    /**
     * Computes the Damerau-Levenshtein distance between two strings,
     * accounting for insertions, deletions, substitutions, and adjacent transpositions.
     */
    fun damerauLevenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        if (len1 == 0) return len2
        if (len2 == 0) return len1

        val d = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) d[i][0] = i
        for (j in 0..len2) d[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                d[i][j] = minOf(
                    d[i - 1][j] + 1,       // deletion
                    d[i][j - 1] + 1,       // insertion
                    d[i - 1][j - 1] + cost // substitution
                )
                // transposition
                if (i > 1 && j > 1 && s1[i - 1] == s2[j - 2] && s1[i - 2] == s2[j - 1]) {
                    d[i][j] = minOf(d[i][j], d[i - 2][j - 2] + cost)
                }
            }
        }
        return d[len1][len2]
    }

    /**
     * Calculates normalized similarity score between 0.0 (completely different) and 1.0 (identical).
     */
    fun calculateSimilarity(s1: String, s2: String): Double {
        val str1 = s1.lowercase(Locale.ROOT).trim()
        val str2 = s2.lowercase(Locale.ROOT).trim()
        if (str1 == str2) return 1.0

        val maxLen = maxOf(str1.length, str2.length)
        if (maxLen == 0) return 1.0

        val distance = damerauLevenshteinDistance(str1, str2)
        return (1.0 - (distance.toDouble() / maxLen)).coerceIn(0.0, 1.0)
    }

    /**
     * Normalizes a search query token with phonetic and regional aliases.
     */
    fun resolveAlias(token: String): String {
        val lower = token.lowercase(Locale.ROOT).trim()
        return ALIAS_MAP[lower] ?: lower
    }

    private data class ScoredFoodItem(
        val food: FoodItem,
        val score: Double
    )

    /**
     * Evaluates the match quality of a candidate food item against a user query.
     * Returns a score >= 0.0, where higher is more relevant.
     */
    fun scoreFood(query: String, food: FoodItem): Double {
        val qClean = query.lowercase(Locale.ROOT).trim()
        if (qClean.isEmpty()) return 0.0

        val nameClean = food.name.lowercase(Locale.ROOT).trim()
        val catClean = food.category.lowercase(Locale.ROOT).trim()

        // 1. Exact match
        if (nameClean == qClean) return 1000.0

        // 2. Starts with full query
        if (nameClean.startsWith(qClean)) return 800.0

        // 3. Word boundary exact match (e.g. "Chicken" in "Grilled Chicken Breast")
        val nameWords = nameClean.split(Regex("[\\s,()/-]+")).filter { it.isNotBlank() }
        if (nameWords.any { it == qClean }) return 700.0

        // 4. Substring match
        if (nameClean.contains(qClean)) return 500.0

        // 5. Category match
        if (catClean.contains(qClean)) return 400.0

        // 6. Multi-token fuzzy match
        val rawQueryTokens = qClean.split(Regex("[\\s,()/-]+")).filter { it.isNotBlank() }
        if (rawQueryTokens.isEmpty()) return 0.0

        // Expand query tokens with aliases
        val resolvedQueryTokens = rawQueryTokens.map { resolveAlias(it) }

        var totalTokenScore = 0.0
        var matchedTokensCount = 0

        for (qToken in resolvedQueryTokens) {
            var bestTokenSim = 0.0

            for (nWord in nameWords) {
                val nResolved = resolveAlias(nWord)

                // Exact word equality
                if (nWord == qToken || nResolved == qToken) {
                    bestTokenSim = 1.0
                    break
                }

                // Word prefix match (e.g., "oat" matches "oats")
                if (nWord.startsWith(qToken) || qToken.startsWith(nWord)) {
                    val sim = 0.9
                    if (sim > bestTokenSim) bestTokenSim = sim
                }

                // Fuzzy Damerau-Levenshtein comparison
                val sim = calculateSimilarity(qToken, nWord)
                val dist = damerauLevenshteinDistance(qToken, nWord)

                // Allow 1 typo for words length 3-4, 2 typos for words length 5+
                val isTypoTolerant = (qToken.length <= 4 && dist <= 1) || (qToken.length > 4 && dist <= 2) || sim >= 0.68

                if (isTypoTolerant && sim > bestTokenSim) {
                    bestTokenSim = sim
                }
            }

            if (bestTokenSim >= 0.65) {
                totalTokenScore += bestTokenSim
                matchedTokensCount++
            }
        }

        // Must match all tokens for short queries (1 or 2 tokens), or at least 70% for longer queries
        val requiredMatches = if (resolvedQueryTokens.size <= 2) resolvedQueryTokens.size else (resolvedQueryTokens.size * 0.7).toInt()

        if (matchedTokensCount >= requiredMatches && matchedTokensCount > 0) {
            val avgSim = totalTokenScore / resolvedQueryTokens.size
            return 200.0 + (avgSim * 100.0)
        }

        return 0.0
    }

    /**
     * Searches foods with typo tolerance, ranking exact matches first,
     * followed by fuzzy matches.
     */
    fun search(query: String, allFoods: List<FoodItem>): List<FoodItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return allFoods

        val scored = mutableListOf<ScoredFoodItem>()
        for (food in allFoods) {
            val score = scoreFood(trimmed, food)
            if (score > 0.0) {
                scored.add(ScoredFoodItem(food, score))
            }
        }

        return scored.sortedByDescending { it.score }.map { it.food }
    }

    /**
     * Detects if the user query is a likely misspelling and suggests the closest matching food name.
     * Returns null if the query already has exact substring matches, or if no high-confidence match is found.
     */
    fun getDidYouMeanSuggestion(query: String, allFoods: List<FoodItem>): String? {
        val trimmed = query.trim()
        if (trimmed.length < 3) return null

        val qClean = trimmed.lowercase(Locale.ROOT)
        // If there's already an exact substring match, no "did you mean" is necessary
        val hasExactSubstring = allFoods.any { it.name.lowercase(Locale.ROOT).contains(qClean) }
        if (hasExactSubstring) return null

        // Look for the highest scoring fuzzy match
        var bestFood: FoodItem? = null
        var highestScore = 0.0

        for (food in allFoods) {
            val score = scoreFood(trimmed, food)
            if (score >= 250.0 && score > highestScore) {
                highestScore = score
                bestFood = food
            }
        }

        return bestFood?.name
    }
}
