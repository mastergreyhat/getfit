package com.getfit.data.remote

import android.content.Context
import android.util.Log
import com.getfit.BuildConfig
import com.getfit.data.entity.FoodItem
import com.getfit.ui.util.AiPreferences
import com.getfit.ui.util.AuthPreferences
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Intelligent AI & Cloud Nutrition Engine.
 * 1. Leverages Google Gemini 1.5 Flash for deep dish understanding, typo correction, and lab-accurate macros.
 * 2. Cascades automatically and concurrently to USDA FoodData Central & OpenFoodFacts when offline or keyless.
 * 3. Enforces strict connection cleanup, parallel lookups, and timeouts to guarantee the UI never stalls or freezes.
 */
object GeminiNutritionService {

    data class GeneratedMeal(
        val mealType: String,
        val title: String,
        val calories: Int,
        val protein: Int,
        val carbs: Int,
        val fat: Int,
        val items: List<String>
    )

    data class GeneratedMealPlan(
        val summary: String,
        val totalCalories: Int,
        val totalProtein: Int,
        val totalCarbs: Int,
        val totalFat: Int,
        val meals: List<GeneratedMeal>
    )

    private const val TAG = "GeminiNutritionService"
    private const val CLOUDFLARE_WORKER_ENDPOINT = "https://getfit-nutrition-proxy.aswinvijay4686.workers.dev/api/nutrition"
    private const val CLOUDFLARE_MEAL_PLAN_ENDPOINT = "https://getfit-nutrition-proxy.aswinvijay4686.workers.dev/api/meal-plan"
    private const val GEMINI_API_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
    private const val USDA_API_ENDPOINT = "https://api.nal.usda.gov/fdc/v1/foods/search"

    private const val CONNECT_TIMEOUT_MS = 10000
    private const val READ_TIMEOUT_MS = 30000
    private const val OVERALL_TIMEOUT_MS = 35000L

    suspend fun generateMealPlan(
        context: Context,
        pantryItems: String,
        targetCalories: Int,
        targetProtein: Int,
        targetCarbs: Int,
        targetFat: Int,
        goal: String
    ): Result<GeneratedMealPlan> = withContext(Dispatchers.IO) {
        val token = AuthPreferences.getToken(context)
        if (token.isNullOrBlank()) {
            return@withContext Result.failure(Exception("Please sign in to generate AI meal plans."))
        }
        val trimmed = pantryItems.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(Exception("Please enter at least one ingredient."))
        }

        var conn: HttpURLConnection? = null
        try {
            val url = URL(CLOUDFLARE_MEAL_PLAN_ENDPOINT)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 45000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $token")
            }

            val payload = JsonObject().apply {
                addProperty("items", trimmed)
                addProperty("targetCalories", targetCalories)
                addProperty("targetProtein", targetProtein)
                addProperty("targetCarbs", targetCarbs)
                addProperty("targetFat", targetFat)
                addProperty("goal", goal)
            }
            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() } ?: "{}"

            if (code in 200..299) {
                val root = JsonParser.parseString(responseText).asJsonObject
                val mealsArray = root.getAsJsonArray("meals") ?: JsonArray()
                val meals = mealsArray.mapNotNull { el ->
                    runCatching {
                        val m = el.asJsonObject
                        val itemsList = m.getAsJsonArray("items")?.map { it.asString } ?: emptyList()
                        GeneratedMeal(
                            mealType = m.get("mealType")?.asString ?: "Meal",
                            title = m.get("title")?.asString ?: "Meal",
                            calories = m.get("calories")?.asInt ?: 0,
                            protein = m.get("protein")?.asInt ?: 0,
                            carbs = m.get("carbs")?.asInt ?: 0,
                            fat = m.get("fat")?.asInt ?: 0,
                            items = itemsList
                        )
                    }.getOrNull()
                }
                if (meals.isEmpty()) {
                    return@withContext Result.failure(Exception("Could not generate meals for those items. Try adding more ingredients."))
                }
                Result.success(
                    GeneratedMealPlan(
                        summary = root.get("summary")?.asString ?: "Daily meal plan tailored to your calorie target.",
                        totalCalories = root.get("totalCalories")?.asInt ?: meals.sumOf { it.calories },
                        totalProtein = root.get("totalProtein")?.asInt ?: meals.sumOf { it.protein },
                        totalCarbs = root.get("totalCarbs")?.asInt ?: meals.sumOf { it.carbs },
                        totalFat = root.get("totalFat")?.asInt ?: meals.sumOf { it.fat },
                        meals = meals
                    )
                )
            } else {
                val errMsg = runCatching {
                    JsonParser.parseString(responseText).asJsonObject.get("error")?.asString
                }.getOrNull() ?: "Failed to generate meal plan ($code)"
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Log.w(TAG, "generateMealPlan exception: ${e.message}")
            Result.failure(Exception(e.localizedMessage ?: "Network error generating meal plan."))
        } finally {
            try {
                conn?.disconnect()
            } catch (_: Exception) {}
        }
    }

    /**
     * Searches for a food item across AI and verified nutrition databases.
     * Uses Google Gemini 2.5 Flash for deep dish understanding, regional recipes, and lab-accurate macros.
     * Cascades gracefully to USDA & OpenFoodFacts when offline.
     */
    suspend fun searchFoods(context: Context, query: String): List<FoodItem> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()

        // 1. Prioritize Cloudflare Serverless AI Proxy (for authenticated users, 24h edge cache)
        if (AiPreferences.isAiSearchEnabled(context) && AuthPreferences.isLoggedIn(context)) {
            try {
                val workerResults = withTimeoutOrNull(OVERALL_TIMEOUT_MS) {
                    queryCloudflareWorker(context, trimmed)
                }
                if (workerResults != null && workerResults.isNotEmpty()) {
                    return@withContext workerResults
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cloudflare Worker proxy query failed: ${e.message}")
            }
        }

        // 2. Offline Smart AI Nutritionist fallback (0ms, zero network needed)
        val smartAiResults = SmartAiNutritionist.analyzeFood(trimmed)
        if (smartAiResults.isNotEmpty()) {
            return@withContext smartAiResults
        }

        // 3. Fast parallel cloud check (USDA + OpenFoodFacts)
        val cloudResults = mutableListOf<FoodItem>()
        try {
            withTimeoutOrNull(4000L) {
                coroutineScope {
                    val usdaDeferred = async(Dispatchers.IO) {
                        try {
                            queryUSDA(trimmed)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }

                    val offDeferred = async(Dispatchers.IO) {
                        try {
                            OpenFoodFactsService.searchFoods(trimmed, pageSize = 8)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }

                    val offList = offDeferred.await()
                    val usdaList = usdaDeferred.await()

                    for (item in offList) {
                        if (cloudResults.none { it.name.equals(item.name, ignoreCase = true) }) {
                            cloudResults.add(item)
                        }
                    }
                    for (item in usdaList) {
                        if (cloudResults.none { it.name.equals(item.name, ignoreCase = true) }) {
                            cloudResults.add(item)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cloud lookups timed out or failed: ${e.message}")
        }

        return@withContext cloudResults
    }

    /**
     * Queries Google Gemini 2.5 Flash with strict structured JSON output instructions.
     */
    private fun queryGemini(foodQuery: String, apiKey: String): List<FoodItem> {
        var conn: HttpURLConnection? = null
        try {
            val url = URL("$GEMINI_API_ENDPOINT?key=$apiKey")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 45000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            val prompt = """
                You are a clinical sports nutritionist. Given the food query, analyze it, correct any spelling mistakes if it closely resembles an actual food or brand, and return a JSON array of up to 4 accurate food items with verified macronutrient values.
                If the query is completely nonsensical gibberish or not a food/dish/ingredient/beverage at all, return an empty JSON array: [].
                For each item, output:
                - "name": Clean, standard food or dish name (e.g. "Chicken Shawarma Roll", "Starbucks Caramel Frappuccino", "Amul Whey Protein", "Kerala Kizhi Porotta", "Mutton Chops (Kerala Style)", "Beef Roast")
                - "category": One of ["Non-Veg", "Vegetables & Paneer", "Rice & Grains", "Lentils & Dal", "South Indian", "Indian Breads", "Dairy", "Fruits", "Snacks", "Beverages", "Protein Supplements", "Custom"]
                - "caloriesPer100g": Calories in kcal per 100g (or per 100ml)
                - "proteinPer100g": Protein in grams per 100g
                - "carbsPer100g": Total carbohydrates in grams per 100g
                - "fatPer100g": Total fat in grams per 100g
                - "fiberPer100g": Dietary fiber in grams per 100g
                - "defaultServingSize": Typical single serving size (e.g. 150.0 for a 150g bowl, 1.0 for 1 piece)
                - "servingUnit": EXACTLY "g" or "piece"
                - "source": "Gemini AI Verified"

                Query: "$foodQuery"
            """.trimIndent()

            val requestBody = JsonObject().apply {
                val contents = JsonArray().apply {
                    val contentObj = JsonObject().apply {
                        val parts = JsonArray().apply {
                            val part = JsonObject().apply {
                                addProperty("text", prompt)
                            }
                            add(part)
                        }
                        add("parts", parts)
                    }
                    add(contentObj)
                }
                add("contents", contents)

                val genConfig = JsonObject().apply {
                    addProperty("temperature", 0.1)
                    addProperty("responseMimeType", "application/json")
                }
                add("generationConfig", genConfig)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(requestBody.toString()) }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val parsedRoot = JsonParser.parseString(responseText).asJsonObject
                val candidates = parsedRoot.getAsJsonArray("candidates")
                if (candidates != null && candidates.size() > 0) {
                    val candidate = candidates.get(0).asJsonObject
                    val content = candidate.getAsJsonObject("content")
                    val parts = content.getAsJsonArray("parts")
                    if (parts != null && parts.size() > 0) {
                        val text = parts.get(0).asJsonObject.get("text").asString
                        return parseGeminiJson(text)
                    }
                }
            } else {
                val errText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.w(TAG, "Gemini API error ${conn.responseCode}: $errText")
            }
        } catch (e: Exception) {
            Log.w(TAG, "queryGemini exception: ${e.message}")
        } finally {
            try {
                conn?.disconnect()
            } catch (_: Exception) {}
        }

        return emptyList()
    }

    private fun queryCloudflareWorker(context: Context, foodQuery: String): List<FoodItem>? {
        val token = AuthPreferences.getToken(context)
        if (token.isNullOrBlank()) {
            return null
        }
        var conn: HttpURLConnection? = null
        try {
            val encodedQuery = URLEncoder.encode(foodQuery, "UTF-8")
            val url = URL("$CLOUDFLARE_WORKER_ENDPOINT?q=$encodedQuery")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $token")
            }

            if (conn.responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                return parseGeminiJson(responseText)
            } else {
                val errText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.w(TAG, "Cloudflare Worker error ${conn.responseCode}: $errText")
            }
        } catch (e: Exception) {
            Log.w(TAG, "queryCloudflareWorker exception: ${e.message}")
        } finally {
            try {
                conn?.disconnect()
            } catch (_: Exception) {}
        }
        return null
    }

    private fun parseGeminiJson(jsonString: String): List<FoodItem> {
        val list = mutableListOf<FoodItem>()
        try {
            val cleanJson = jsonString.trim().removeSurrounding("```json", "```").trim()
            val jsonElement = JsonParser.parseString(cleanJson)
            if (jsonElement.isJsonArray) {
                for (itemElem in jsonElement.asJsonArray) {
                    if (itemElem.isJsonObject) {
                        val obj = itemElem.asJsonObject
                        val name = obj.get("name")?.asString ?: continue
                        val category = obj.get("category")?.asString ?: "Custom"
                        val calories = obj.get("caloriesPer100g")?.asDouble ?: obj.get("calories")?.asDouble ?: 0.0
                        val protein = obj.get("proteinPer100g")?.asDouble ?: obj.get("protein")?.asDouble ?: 0.0
                        val carbs = obj.get("carbsPer100g")?.asDouble ?: obj.get("carbs")?.asDouble ?: 0.0
                        val fat = obj.get("fatPer100g")?.asDouble ?: obj.get("fat")?.asDouble ?: 0.0
                        val fiber = obj.get("fiberPer100g")?.asDouble ?: obj.get("fiber")?.asDouble ?: 0.0
                        val servingSize = obj.get("defaultServingSize")?.asDouble ?: obj.get("servingSize")?.asDouble ?: 100.0
                        val unit = obj.get("servingUnit")?.asString ?: "g"
                        val validUnit = if (unit.equals("piece", ignoreCase = true)) "piece" else "g"

                        list.add(
                            FoodItem(
                                name = name,
                                category = category,
                                caloriesPer100g = calories,
                                proteinPer100g = protein,
                                carbsPer100g = carbs,
                                fatPer100g = fat,
                                fiberPer100g = fiber,
                                defaultServingSize = servingSize,
                                servingUnit = validUnit,
                                isCustom = true,
                                source = "Gemini AI Verified"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Gemini response JSON", e)
        }
        return list
    }

    /**
     * Queries USDA FoodData Central open API (Lab verified nutritional values).
     */
    private fun queryUSDA(foodQuery: String): List<FoodItem> {
        var conn: HttpURLConnection? = null
        val list = mutableListOf<FoodItem>()
        try {
            val encodedQuery = URLEncoder.encode(foodQuery, "UTF-8")
            val urlString = "$USDA_API_ENDPOINT?query=$encodedQuery&pageSize=8&api_key=${BuildConfig.USDA_API_KEY}"
            val url = URL(urlString)

            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JsonParser.parseString(responseText).asJsonObject
                val foodsArray = root.getAsJsonArray("foods") ?: return emptyList()

                for (element in foodsArray) {
                    if (!element.isJsonObject) continue
                    val obj = element.asJsonObject
                    val description = obj.get("description")?.asString ?: continue

                    var calories = 0.0
                    var protein = 0.0
                    var carbs = 0.0
                    var fat = 0.0
                    var fiber = 0.0

                    val nutrients = obj.getAsJsonArray("foodNutrients")
                    if (nutrients != null) {
                        for (n in nutrients) {
                            if (!n.isJsonObject) continue
                            val nObj = n.asJsonObject
                            val nutrientName = nObj.get("nutrientName")?.asString?.lowercase() ?: ""
                            val value = nObj.get("value")?.asDouble ?: 0.0

                            when {
                                nutrientName.contains("energy") && (nutrientName.contains("kcal") || nObj.get("unitName")?.asString?.lowercase() == "kcal") -> calories = value
                                nutrientName.contains("protein") -> protein = value
                                nutrientName.contains("carbohydrate") -> carbs = value
                                nutrientName.contains("total lipid") || nutrientName.contains("fat") -> fat = value
                                nutrientName.contains("fiber") -> fiber = value
                            }
                        }
                    }

                    if (calories > 0.0 || protein > 0.0) {
                        list.add(
                            FoodItem(
                                name = formatUsdaName(description),
                                category = categorizeFood(description),
                                caloriesPer100g = Math.round(calories * 10.0) / 10.0,
                                proteinPer100g = Math.round(protein * 10.0) / 10.0,
                                carbsPer100g = Math.round(carbs * 10.0) / 10.0,
                                fatPer100g = Math.round(fat * 10.0) / 10.0,
                                fiberPer100g = Math.round(fiber * 10.0) / 10.0,
                                defaultServingSize = 100.0,
                                servingUnit = "g",
                                isCustom = true,
                                source = "USDA Verified"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "queryUSDA error: ${e.message}")
        } finally {
            try {
                conn?.disconnect()
            } catch (_: Exception) {}
        }
        return list
    }

    private fun formatUsdaName(raw: String): String {
        return raw.split(",")
            .map { it.trim() }
            .take(3)
            .joinToString(", ")
            .split(" ")
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
    }

    private fun categorizeFood(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.contains("chicken") || lower.contains("beef") || lower.contains("egg") || lower.contains("fish") || lower.contains("turkey") || lower.contains("pork") || lower.contains("mutton") -> "Non-Veg"
            lower.contains("rice") || lower.contains("oat") || lower.contains("wheat") || lower.contains("grain") || lower.contains("pasta") -> "Rice & Grains"
            lower.contains("dal") || lower.contains("lentil") || lower.contains("bean") || lower.contains("chickpea") -> "Lentils & Dal"
            lower.contains("milk") || lower.contains("cheese") || lower.contains("yogurt") || lower.contains("curd") || lower.contains("paneer") -> "Dairy"
            lower.contains("apple") || lower.contains("banana") || lower.contains("berry") || lower.contains("orange") || lower.contains("fruit") -> "Fruits"
            lower.contains("protein") || lower.contains("whey") || lower.contains("isolate") -> "Protein Supplements"
            lower.contains("bread") || lower.contains("roti") || lower.contains("chapati") || lower.contains("naan") -> "Indian Breads"
            lower.contains("snack") || lower.contains("chip") || lower.contains("cookie") || lower.contains("biscuit") -> "Snacks"
            lower.contains("coffee") || lower.contains("tea") || lower.contains("shake") || lower.contains("juice") -> "Beverages"
            else -> "Vegetables & Paneer"
        }
    }
}
