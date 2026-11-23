package com.getfit.data

import com.getfit.data.entity.UserProfile
import kotlin.math.roundToInt

data class MealSuggestion(
    val mealType: String,
    val time: String,
    val foods: List<String>,
    val calories: Int,
    val protein: Int,
    val carbs: Int = 0,
    val fat: Int = 0
)

data class DietPlan(
    val name: String,
    val totalCalories: Int,
    val totalProtein: Int,
    val description: String,
    val meals: List<MealSuggestion>,
    val carbs: Int = 0,
    val fat: Int = 0
)

object MealPlanSuggestions {
    fun getAllPlans(userProfile: UserProfile? = null): List<DietPlan> = listOf(
        getPersonalPlan(userProfile),
        get2000CalPlan(),
        get2500CalPlan(),
        get3000CalPlan(),
        get3500CalPlan()
    )

    /**
     * Dedicated Personal Meal Plan dynamically calibrated to the user's Profile
     * (Weight, Goal, Calorie & Macro targets):
     * - Morning Shake: Oats, Milk, Banana, Whey, Almonds, Cashews, Peanuts
     * - Lunch: Cooked Basmati Rice, Chicken Breast, Cucumber & Carrot Salad
     * - Evening Fuel: Banana + Peanuts
     * - Dinner: Whole Eggs, Cooked Basmati Rice, Cucumber & Carrot
     * - Night: Warm Milk
     */
    fun getPersonalPlan(userProfile: UserProfile? = null): DietPlan {
        val profile = userProfile ?: UserProfile()
        val targetCals = profile.dailyCalorieTarget
        val targetProt = profile.dailyProteinTarget
        val targetCarbs = profile.dailyCarbsTarget
        val targetFat = profile.dailyFatTarget
        val goal = profile.goal.ifBlank { "Hypertrophy" }
        val weight = profile.weight.coerceIn(40.0, 200.0)

        // Scale ratios compared to baseline 2480 kcal plan
        val calRatio = (targetCals.toDouble() / 2480.0).coerceIn(0.5, 2.0)
        val protRatio = (targetProt.toDouble() / 165.0).coerceIn(0.5, 2.2)

        // 1. Breakfast Shake / Fuel
        val oatsGrams = (60.0 * calRatio).roundToInt().coerceIn(35, 120)
        val milkMl = (300.0 * calRatio).roundToInt().coerceIn(180, 450)
        val wheyScoops = if (targetProt >= 140) 1.0 else 0.5
        val wheyProt = (25.0 * wheyScoops).toInt()
        val wheyCals = (120.0 * wheyScoops).toInt()
        val nutsGrams = (20.0 * calRatio).roundToInt().coerceIn(10, 35)

        val bOatsCals = (oatsGrams * 3.8).toInt()
        val bOatsP = (oatsGrams * 0.13).toInt()
        val bOatsC = (oatsGrams * 0.67).toInt()
        val bOatsF = (oatsGrams * 0.06).toInt()

        val bMilkCals = (milkMl * 0.5).toInt()
        val bMilkP = (milkMl * 0.032).toInt()
        val bMilkC = (milkMl * 0.048).toInt()
        val bMilkF = (milkMl * 0.015).toInt()

        val bBananaCals = 105; val bBananaP = 1; val bBananaC = 27; val bBananaF = 0
        val bNutsCals = (nutsGrams * 5.8).toInt()
        val bNutsP = (nutsGrams * 0.2).toInt()
        val bNutsC = (nutsGrams * 0.2).toInt()
        val bNutsF = (nutsGrams * 0.5).toInt()

        val bCals = bOatsCals + bMilkCals + bBananaCals + wheyCals + bNutsCals
        val bProt = bOatsP + bMilkP + bBananaP + wheyProt + bNutsP
        val bCarb = bOatsC + bMilkC + bBananaC + 2 + bNutsC
        val bFat = bOatsF + bMilkF + bBananaF + 2 + bNutsF

        val breakfastFoods = listOf(
            "Rolled Oats (${oatsGrams}g)",
            "Toned Milk (${milkMl}ml)",
            "1 Medium Banana (100g)",
            if (wheyScoops == 1.0) "Whey Protein (1 scoop, 25g protein)" else "Whey Protein (1/2 scoop, 13g protein)",
            "Raw Almonds & Cashews (${nutsGrams}g)"
        )

        // 2. Lunch Power Bowl
        val riceGrams = (250.0 * calRatio).roundToInt().coerceIn(120, 450)
        val chickenGrams = (200.0 * protRatio).roundToInt().coerceIn(120, 320)

        val lRiceCals = (riceGrams * 1.3).toInt()
        val lRiceP = (riceGrams * 0.026).toInt()
        val lRiceC = (riceGrams * 0.28).toInt()
        val lRiceF = (riceGrams * 0.003).toInt()

        val lChickenCals = (chickenGrams * 1.65).toInt()
        val lChickenP = (chickenGrams * 0.31).toInt()
        val lChickenC = 0
        val lChickenF = (chickenGrams * 0.036).toInt()

        val lSaladCals = 40; val lSaladP = 2; val lSaladC = 8; val lSaladF = 0

        val lCals = lRiceCals + lChickenCals + lSaladCals
        val lProt = lRiceP + lChickenP + lSaladP
        val lCarb = lRiceC + lChickenC + lSaladC
        val lFat = lRiceF + lChickenF + lSaladF

        val lunchFoods = listOf(
            "Cooked Basmati Rice (${riceGrams}g)",
            "Lean Chicken Breast (${chickenGrams}g cooked)",
            "Cucumber & Carrot Salad (150g)"
        )

        // 3. Snacks / Pre-Workout
        val snackPeanutsGrams = (15.0 * calRatio).roundToInt().coerceIn(10, 30)
        val sPeanutCals = (snackPeanutsGrams * 5.7).toInt()
        val sPeanutP = (snackPeanutsGrams * 0.25).toInt()
        val sPeanutC = (snackPeanutsGrams * 0.16).toInt()
        val sPeanutF = (snackPeanutsGrams * 0.50).toInt()

        val sCals = 105 + sPeanutCals
        val sProt = 1 + sPeanutP
        val sCarb = 27 + sPeanutC
        val sFat = 0 + sPeanutF

        val snackFoods = listOf(
            "1 Medium Banana (100g)",
            "Roasted Peanuts (${snackPeanutsGrams}g)"
        )

        // 4. Dinner Egg Power Bowl
        val eggCount = if (targetCals < 1800) 2 else 3
        val dinnerRiceGrams = (180.0 * calRatio).roundToInt().coerceIn(100, 350)

        val dEggCals = eggCount * 78
        val dEggP = eggCount * 6
        val dEggC = eggCount * 1
        val dEggF = eggCount * 5

        val dRiceCals = (dinnerRiceGrams * 1.3).toInt()
        val dRiceP = (dinnerRiceGrams * 0.026).toInt()
        val dRiceC = (dinnerRiceGrams * 0.28).toInt()
        val dRiceF = (dinnerRiceGrams * 0.003).toInt()

        val dCals = dEggCals + dRiceCals + 25
        val dProt = dEggP + dRiceP + 1
        val dCarb = dEggC + dRiceC + 5
        val dFat = dEggF + dRiceF + 0

        val dinnerFoods = listOf(
            "$eggCount Whole Eggs (Boiled or Scrambled)",
            "Cooked Basmati Rice (${dinnerRiceGrams}g)",
            "Fresh Cucumber & Carrot Salad (100g)"
        )

        // 5. Night Recovery Milk
        val nightMilkMl = (200.0 * calRatio).roundToInt().coerceIn(120, 300)
        val nCals = (nightMilkMl * 0.5).toInt()
        val nProt = (nightMilkMl * 0.032).toInt()
        val nCarb = (nightMilkMl * 0.048).toInt()
        val nFat = (nightMilkMl * 0.015).toInt()

        val nightFoods = listOf("Warm Toned Milk (${nightMilkMl}ml)")

        val rawTotalCals = bCals + lCals + sCals + dCals + nCals
        val diff = targetCals - rawTotalCals
        val adjLCals = lCals + (diff / 2)
        val adjDCals = dCals + (diff - (diff / 2))

        return DietPlan(
            name = "Personal",
            totalCalories = targetCals,
            totalProtein = targetProt,
            carbs = targetCarbs,
            fat = targetFat,
            description = "Customized to your profile (${weight.toInt()}kg, ${goal}): Daily target ${targetCals} kcal (${targetProt}g P • ${targetCarbs}g C • ${targetFat}g F) calibrated to your nutritional targets.",
            meals = listOf(
                MealSuggestion("Breakfast (Hypertrophy Shake)", "07:30", breakfastFoods, bCals, bProt, bCarb, bFat),
                MealSuggestion("Lunch (Chicken & Rice Power Bowl)", "13:00", lunchFoods, adjLCals, lProt, lCarb, lFat),
                MealSuggestion("Snacks / Pre-Workout", "17:00", snackFoods, sCals, sProt, sCarb, sFat),
                MealSuggestion("Dinner ($eggCount-Egg Power Bowl)", "20:30", dinnerFoods, adjDCals, dProt, dCarb, dFat),
                MealSuggestion("Night Recovery", "22:15", nightFoods, nCals, nProt, nCarb, nFat)
            )
        )
    }

    fun get2000CalPlan(): DietPlan = DietPlan(
        name = "Lean Bulk / Cut",
        totalCalories = 2000,
        totalProtein = 130,
        carbs = 230,
        fat = 55,
        description = "For fat loss while preserving lean mass. Caloric deficit with high protein density.",
        meals = listOf(
            MealSuggestion("Early Morning", "06:30", listOf("Warm water with lemon", "5 soaked almonds", "2 walnuts"), 80, 3, 3, 6),
            MealSuggestion("Breakfast", "07:30", listOf("3 Egg White Omelette with veggies", "2 Chapati", "1 glass Toned Milk"), 350, 25, 40, 10),
            MealSuggestion("Mid-Morning Snack", "10:30", listOf("1 Banana", "Roasted Chana (30g)", "Green Tea"), 220, 8, 35, 4),
            MealSuggestion("Lunch", "13:00", listOf("White Rice (150g cooked)", "Toor Dal (150g)", "Mixed Veg Curry (100g)", "Curd (100g)", "Salad"), 450, 22, 65, 10),
            MealSuggestion("Evening Snack", "16:30", listOf("Sprouts Chaat (150g)", "1 Apple", "Black Coffee"), 210, 10, 30, 5),
            MealSuggestion("Pre-Workout", "17:30", listOf("1 Banana", "Black Coffee"), 95, 1, 22, 0),
            MealSuggestion("Dinner", "20:30", listOf("Chicken Breast grilled (100g)", "2 Chapati", "Palak Sabzi (100g)", "Salad"), 395, 35, 35, 12),
            MealSuggestion("Before Bed", "22:00", listOf("1 glass warm Toned Milk", "5 soaked almonds"), 200, 10, 15, 8)
        )
    )

    fun get2500CalPlan(): DietPlan = DietPlan(
        name = "Moderate Bulk",
        totalCalories = 2500,
        totalProtein = 160,
        carbs = 290,
        fat = 75,
        description = "For steady lean muscle gains with low fat accumulation. Great for daily lifting.",
        meals = listOf(
            MealSuggestion("Early Morning", "06:30", listOf("Warm water with honey", "6 soaked almonds", "3 walnuts", "2 Dates"), 150, 5, 18, 6),
            MealSuggestion("Breakfast", "07:30", listOf("4 Whole Eggs (2 whole + 2 white) Bhurji", "3 Chapati", "1 glass Full Fat Milk"), 520, 32, 55, 18),
            MealSuggestion("Mid-Morning Snack", "10:30", listOf("1 Banana", "Peanut Butter (1 tbsp) on toast", "Green Tea"), 300, 10, 40, 10),
            MealSuggestion("Lunch", "13:00", listOf("White Rice (200g cooked)", "Rajma (150g)", "Aloo Gobi (100g)", "Curd (100g)", "Salad"), 550, 28, 80, 12),
            MealSuggestion("Evening Snack", "16:30", listOf("Paneer Tikka (80g)", "Makhana (20g)", "Coconut Water (250ml)"), 300, 16, 25, 14),
            MealSuggestion("Pre-Workout", "17:30", listOf("1 Banana", "1 scoop Whey Protein with water"), 210, 24, 28, 1),
            MealSuggestion("Dinner", "20:30", listOf("Chicken Curry (200g)", "2 Chapati", "Mixed Veg (100g)", "Salad"), 470, 35, 50, 14),
            MealSuggestion("Before Bed", "22:00", listOf("1 glass warm Milk with turmeric", "Chia Seeds (10g)"), 150, 8, 14, 6)
        )
    )

    fun get3000CalPlan(): DietPlan = DietPlan(
        name = "Mass Building",
        totalCalories = 3000,
        totalProtein = 185,
        carbs = 360,
        fat = 90,
        description = "For intermediate lifters looking for aggressive hypertrophy and sustained recovery.",
        meals = listOf(
            MealSuggestion("Early Morning", "06:00", listOf("Warm water with honey", "8 soaked almonds", "4 walnuts", "3 Dates"), 220, 7, 25, 10),
            MealSuggestion("Breakfast", "07:00", listOf("Oats (200g cooked) with Milk", "2 Whole Eggs + 2 Egg Whites boiled", "1 Banana", "1 tbsp Peanut Butter"), 580, 35, 65, 18),
            MealSuggestion("Mid-Morning Snack", "10:00", listOf("Paneer Bhurji (100g)", "2 Chapati", "Chai"), 460, 22, 50, 18),
            MealSuggestion("Lunch", "13:00", listOf("White Rice (250g cooked)", "Dal Makhani (150g)", "Chicken Curry (150g)", "Curd (100g)", "Salad"), 720, 42, 95, 18),
            MealSuggestion("Evening Snack", "16:30", listOf("Sprouts Chaat (150g)", "1 Apple", "Roasted Makhana (30g)", "Coconut Water"), 280, 12, 40, 7),
            MealSuggestion("Pre-Workout", "17:30", listOf("Banana Shake (300ml)", "1 tbsp Peanut Butter"), 400, 12, 55, 14),
            MealSuggestion("Post-Workout", "19:30", listOf("1 scoop Whey Protein with Milk", "1 Banana"), 280, 30, 35, 2),
            MealSuggestion("Dinner", "21:00", listOf("Chicken Biryani (250g)", "Raita (100g)", "Salad"), 520, 30, 68, 13),
            MealSuggestion("Before Bed", "22:30", listOf("1 glass warm Milk with turmeric", "10 almonds", "Flax Seeds (10g)"), 300, 14, 22, 16)
        )
    )

    fun get3500CalPlan(): DietPlan = DietPlan(
        name = "Hard Gainer",
        totalCalories = 3500,
        totalProtein = 210,
        carbs = 425,
        fat = 105,
        description = "Maximum caloric surplus for hard gainers with high metabolisms and heavy training volume.",
        meals = listOf(
            MealSuggestion("Early Morning", "06:00", listOf("Warm water with honey", "10 soaked almonds", "5 walnuts", "4 Dates", "1 tbsp Ghee in warm water"), 350, 10, 32, 20),
            MealSuggestion("Breakfast", "07:00", listOf("4 Whole Eggs Omelette", "3 Aloo Paratha", "Curd (100g)", "1 glass Full Fat Milk"), 780, 38, 85, 30),
            MealSuggestion("Mid-Morning Snack", "10:00", listOf("Banana Shake (300ml) with Peanut Butter", "Roasted Chana (50g)"), 480, 20, 60, 18),
            MealSuggestion("Lunch", "13:00", listOf("White Rice (300g cooked)", "Chole (200g)", "Paneer Tikka (100g)", "Curd (100g)", "Salad", "1 tsp Ghee on rice"), 820, 48, 105, 22),
            MealSuggestion("Evening Snack", "16:00", listOf("Chicken Sandwich (2 slices bread + chicken)", "Makhana (30g)", "Coconut Water", "1 Apple"), 380, 22, 45, 11),
            MealSuggestion("Pre-Workout", "17:30", listOf("2 Bananas", "1 scoop Mass Gainer with Milk"), 520, 18, 90, 8),
            MealSuggestion("Post-Workout", "19:30", listOf("1 scoop Whey Protein with Milk", "1 Banana", "2 Dates"), 350, 30, 52, 2),
            MealSuggestion("Dinner", "21:00", listOf("Chicken Biryani (300g)", "Butter Chicken (150g)", "2 Naan", "Raita (100g)"), 950, 52, 110, 32),
            MealSuggestion("Before Bed", "22:30", listOf("1 glass warm Milk with Casein Protein", "10 almonds"), 350, 35, 18, 14)
        )
    )

}
