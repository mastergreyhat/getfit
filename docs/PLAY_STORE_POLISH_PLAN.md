# GETFIT: PRODUCTION REMEDIATION & PLAY STORE POLISH PLAN
**Document Version:** 2.0.0  
**Target Platform:** Android (Native Jetpack Compose / Material 3) & Web Simulator (Clean Design System)  
**Status:** Approved for Implementation  
**Audience:** Next AI Model / Lead Mobile Engineer  

---

## 1. Executive Summary & Quality Directive

### 1.1 Objective
Transform the GetFit codebase into a **flawless, publication-ready fitness & nutrition tracking app** tailored for release on the Google Play Store, adhering to the "Clean" SaaS design standard (Linear/Stripe aesthetic: pure whites, deep OLED blacks, subtle borders, crisp typography, and zero emoji clutter).

### 1.2 Core Mandates from Product Owner
1. **4 Tabs Only:** Simplify bottom navigation strictly to:
   - **Home** (`dashboard`)
   - **Nutrition** (`food`)
   - **Training** (`workout`)
   - **Profile** (`profile`)
2. **Remove Hydration/Water Section Completely:** Strip all water tracking elements across Android and Web. No water tab, no water cards in dashboard, no water section in diary.
3. **Remove Natural Language / Voice Smart Log Completely:** Strip the NLP and voice speech input because it causes freezing, state locks, and parsing failures. Replace with instant, rock-solid search, category filters, and quick-add modals.
4. **Eliminate All Dummy / Placeholder Data:**
   - Remove hardcoded clock `09:41` (replace with dynamic device time).
   - Remove "simulated scan" placeholder warnings.
   - Remove hardcoded dates ("Today, 11 Nov").
   - Remove fake analytics charts that don't bind to live user data.
   - Remove all TODOs, dead functions, and unused code paths.
5. **Fix All Functional Bugs:**
   - When clicking "Add Food" under Breakfast, Lunch, Dinner, or Snacks, the food **must** log to that exact meal (not default to Lunch).
   - When diet presets (2000, 2500, 3000, 3500 kcal) are changed, all macros (Protein, Carbs, Fats) must calculate and update proportionally.
   - Ensure the user's **Personal Meal Plan** (featuring their exact pantry: Oats 60g, Milk 300ml, Banana, Whey 25g, Almonds 10g, Cashews 10g, Peanuts 15g, Basmati Rice 250g, Chicken Breast 200g, Cucumber, Carrot, Eggs max 3) is 100% bug-free and can be logged individually or as a complete day with a single clean toast.
   - Fix timer flicker in Workout screen.
6. **Codebase Synchronization:** All updates must be made in tandem across:
   - Android Native: `app/src/main/java/com/getfit/...`
   - Web App: `web/index.html`
   - IDE Artifact: `getfit_app.html`

---

## 2. Inventory of Changes & Structural Surgery

### 2.1 File Operations Summary

| Target File | Action | Description |
|---|---|---|
| `app/src/main/java/com/getfit/ui/navigation/NavHost.kt` | **Modify** | Remove `Screen.Water`, reduce `bottomNavItems` from 5 to 4, remove `WaterScreen` route. |
| `app/src/main/java/com/getfit/ui/screens/WaterScreen.kt` | **Delete** | Entire screen removed. |
| `app/src/main/java/com/getfit/ui/viewmodel/WaterViewModel.kt` | **Delete** | Entire viewmodel removed. |
| `app/src/main/java/com/getfit/data/entity/WaterLog.kt` | **Delete / Deprecate** | Remove entity or leave unused without queries. |
| `app/src/main/java/com/getfit/data/dao/WaterLogDao.kt` | **Delete / Deprecate** | Remove water DAO. |
| `app/src/main/java/com/getfit/notification/HydrationReminderWorker.kt` | **Delete** | Remove background hydration worker. |
| `app/src/main/java/com/getfit/ui/screens/DashboardScreen.kt` | **Modify** | Remove `onNavigateToWater`, remove Hydration Card, align grid to 2 items. |
| `app/src/main/java/com/getfit/ui/viewmodel/DashboardViewModel.kt` | **Modify** | Remove water flows and `quickLogWater()`. |
| `app/src/main/java/com/getfit/ui/screens/FoodScreen.kt` | **Modify** | Remove Smart NLP Card, Voice launcher, `SmartLogPreviewDialog`, Water diary section, `onQuickLogWater`. Fix meal targeting when adding food. |
| `app/src/main/java/com/getfit/ui/viewmodel/FoodViewModel.kt` | **Modify** | Remove `SmartFoodParser` calls, `smartLogPreview`, `isSmartLogging`, `todayWaterMl`, `quickLogWater`. Add `targetMealForAdd` state. |
| `app/src/main/java/com/getfit/data/util/SmartFoodParser.kt` | **Delete** | Unused parser class. |
| `app/src/main/java/com/getfit/ui/util/VoiceInputHelper.kt` | **Delete** | Unused voice helper. |
| `app/src/main/java/com/getfit/data/repository/GetFitRepository.kt` | **Modify** | Remove water query calls. |
| `app/src/main/java/com/getfit/data/AppDatabase.kt` | **Modify** | Remove `WaterLog` from entities list and remove `waterLogDao()`. |
| `web/index.html` | **Modify** | Remove `#nav-water`, remove `renderWater()`, remove `addWater()`, remove `doSmartLog()` and `startVoiceInput()`, remove smart NLP card, remove water card from dashboard and diary. Fix `openAddFoodFor()` meal targeting, fix preset macros, fix workout timer DOM update. |
| `getfit_app.html` (Artifact) | **Modify** | Keep 100% synchronized with `web/index.html`. |
| `scratch_test_agent.py` | **Modify** | Update Playwright test suite to validate 4 tabs, meal logging accuracy, theme toggle, and 0 console errors. |

---

## 3. Detailed Component-by-Component Specifications

### 3.1 Bottom Navigation Bar (Android & Web)
* **Tabs:** Exactly 4 items in this order:
  1. **Home** (`dashboard`): Icon `Icons.Default.Home`, title `"Home"`.
  2. **Nutrition** (`food`): Icon `Icons.Default.Restaurant`, title `"Nutrition"`.
  3. **Training** (`workout`): Icon `Icons.Default.FitnessCenter`, title `"Training"`.
  4. **Profile** (`profile`): Icon `Icons.Default.Person`, title `"Profile"`.
* **Theme Capsule Toggle Location:**
  - **Exclusively** in the Profile screen (`ProfileScreen.kt` / `renderProfile()`).
  - **Never** render theme toggles in top app bar, dashboard, nutrition, or training screens.

---

### 3.2 Home / Dashboard Screen

#### Android: `app/src/main/java/com/getfit/ui/screens/DashboardScreen.kt`
* **Remove:**
  - Callback `onNavigateToWater: () -> Unit` in function signature.
  - "Hydration Card" (previously lines 191–251).
* **Retain & Polish:**
  1. **Energy Balance Card:**
     - Calories Consumed vs. Target (e.g. `1,250 / 2,500 kcal`).
     - Percentage met capsule pill (`50% Met`).
     - Progress bar with thin clean border.
     - Macro Adherence rows: Protein, Carbohydrates, Fats with respective targets and grams consumed.
  2. **Next Scheduled Meal Card:**
     - Calculate dynamically based on current hour:
       - 05:00 - 11:00: Breakfast ("High Protein Oatmeal / Shake")
       - 11:00 - 15:00: Lunch ("Basmati Rice & Chicken Power Bowl")
       - 15:00 - 18:30: Snack ("Fruit & Roasted Nuts")
       - 18:30 - 23:00: Dinner ("Whole Eggs & Veggie Bowl")
       - 23:00 - 05:00: Fasting / Rest ("Recovery Window")
     - Click navigates directly to `onNavigateToFood()`.
  3. **Quick Actions Grid (2 clean cards):**
     - Card 1: `LOG FOOD` → `onNavigateToFood()`
     - Card 2: `TRAINING` → `onNavigateToWorkout()`
     - Card 3 (Workout of the day / Split preview): Shows current workout split (e.g. "Push Day: Bench & Shoulders").

#### Web: `web/index.html` (`renderDashboard()`)
* Remove the Hydration card (`<div class="card-clean p-3 flex ...">`).
* Ensure dynamic time and clean layout with no water progress metrics.

---

### 3.3 Nutrition Screen & Meal Logging (MyFitnessPal Standard)

#### Current Defects to Fix:
1. **Broken Meal Target in "Add Food":**
   - In `openAddFoodFor(mealName)`, `mealName` was received but ignored; all added foods logged to `'Lunch'`.
   - **Fix:** Maintain `selectedMealForAdd` in state (defaulting to the meal whose button was clicked, or `"Breakfast"`). When a food item is clicked or "Quick Add" is submitted, log it specifically under `selectedMealForAdd`.
   - On the Search & Add tab, display a subtle sticky pill: `Adding to: [ Breakfast ▾ ]` so the user can see and change the target meal before tapping `+`.
2. **Remove Smart NLP Quick-Log Card:**
   - Remove `<div class="card-clean p-4 space-y-3">` containing `#smartText`, `startVoiceInput()`, and `doSmartLog()`.
   - In Android `FoodScreen.kt`, delete `SmartLogPreviewDialog` composable and the voice launcher composable call.
3. **Remove Water Section from Diary:**
   - Delete the water intake card at the bottom of the Diary tab in both Android and Web.
4. **Calorie Equation Banner (MyFitnessPal Style):**
   - Formula: `Remaining = Goal - Food + Exercise`
   - Display cleanly: `[Goal: 2,500] - [Food: 1,420] + [Exercise: 0] = [Remaining: 1,080 kcal]`.
5. **Meal Sections in Diary:**
   - Four distinct sections:
     - `Breakfast`
     - `Lunch`
     - `Dinner`
     - `Snacks`
   - Each section displays:
     - Section Title + Subtotal kcal (e.g., `LUNCH  •  710 kcal`).
     - Logged food items with grams, calories, and macros breakdown (`P: 70g • C: 83g • F: 8g`).
     - Delete icon button (`trash-2`).
     - Action buttons: `+ Add Food` (switches to Search with this meal pre-selected) and `⚡ Quick Add` (opens modal with this meal pre-selected).

#### Sub-Tabs in Nutrition Screen:
1. **Diary:** Date navigator (`◀ Today, [Real Date] ▶`), Calorie Equation, 4 Meal Cards, Daily Macro Totals.
2. **Search & Add:**
   - Clean search bar with instant filter across local database.
   - Barcode Scanner button (opens barcode modal).
   - "Search Online" button linking to OpenFoodFacts for packaged goods.
   - Comprehensive offline Indian & staple food database (50 items covering rice, roti, dal, chicken, eggs, milk, oats, paneer, fish, fruits, nuts).
3. **Pantry & Plans:**
   - **Personal Meal Plan (Top Priority):**
     - Crafted strictly with the user's available foods:
       - **Meal 1 - Breakfast Shake (845 kcal, 52g P, 92g C, 32g F):** Rolled Oats (60g), Milk (300ml), Medium Banana (1), Whey Protein (25g), Almonds (10g), Cashews (10g), Peanuts (15g).
       - **Meal 2 - Lunch Power Bowl (710 kcal, 70g P, 83g C, 8g F):** Basmati Rice (250g cooked), Chicken Breast (200g cooked), Cucumber (100g), Carrot (100g).
       - **Meal 3 - Evening Fuel (190 kcal, 5g P, 30g C, 8g F):** Medium Banana (1), Roasted Peanuts (15g).
       - **Meal 4 - Dinner (515 kcal, 25g P, 62g C, 16g F):** 3 Whole Eggs (Boiled/Scrambled - Max 3 eggs rule strictly observed), Basmati Rice (200g), Cucumber & Carrot Salad (100g).
       - **Meal 5 - Night Recovery (120 kcal, 7g P, 10g C, 4g F):** Warm Milk (200ml).
     - **Buttons:**
       - Individual "Log Meal" button on each card.
       - Master "Log Entire Personal Plan" button: Logs all 5 meals in one single transaction and displays one toast: `"Logged complete 2,480 kcal Personal Plan to Diary"`.
   - **Interactive Kitchen Pantry Generator:**
     - Selectable chips for available items (Oats, Milk, Banana, Whey Protein, Almonds, Cashews, Peanuts, Basmati Rice, Chicken, Cucumber, Carrot, Eggs, Paneer, Dal, Curd, Soya Chunks, Fish, Spinach).
     - Text field to type and add any custom pantry item (e.g. "Sattu", "Tofu").
     - "Generate Meal Plan From My Pantry" button compiles a structured 3-4 meal plan utilizing selected items.
   - **Preset Calorie Plans:**
     - 2000 kcal (Lean Cut)
     - 2500 kcal (Moderate Bulk)
     - 3000 kcal (Mass Building)
     - 3500 kcal (Hard Gainer)

---

### 3.4 Training / Workout Screen
* **Active Routine Card:**
  - Routine Name: "Push Hypertrophy (Chest, Delts, Triceps)"
  - Exercise 1: Barbell Flat Bench Press (4 Sets × 8-10 Reps)
  - Exercise 2: Incline Dumbbell Press (3 Sets × 10-12 Reps)
  - Exercise 3: Standing Overhead Barbell Press (3 Sets × 8-10 Reps)
  - Exercise 4: Triceps Rope Pushdown (4 Sets × 12-15 Reps)
  - Exercise 5: Lateral Dumbbell Raises (4 Sets × 15 Reps)
* **Interactive Set Logger:**
  - Table columns: `SET`, `PREV`, `KG`, `REPS`, `CHECK`.
  - Checking a set marks it complete and launches the Rest Timer.
* **Rest Timer:**
  - 90-second countdown.
  - **Bug Fix for Web:** Instead of re-rendering `main.innerHTML = renderWorkout()` every second (which blinks the screen and breaks user interaction), update the `#restTimerText` span directly via `document.getElementById('restTimerText').innerText = formatTime(remaining)`.
* **Finish Workout Button:**
  - Saves the session to workout history, triggers congratulatory feedback, and resets completed checkmarks.

---

### 3.5 Profile & Settings Screen
* **Capsule Theme Switcher (Single Source of Truth):**
  - Segmented capsule control with `Light Mode` and `Dark Mode`.
  - Light mode: Pure `#FFFFFF` background, `#F7F6F2` secondary, `#111111` typography, `#E8E6E1` borders.
  - Dark mode: Pure `#050505` background, `#121212` secondary, `#EDEDED` typography, `#262626` borders.
  - Smooth 150ms CSS color transition.
  - Preferences saved to `localStorage` (Web) and `ThemePreferences` DataStore (Android).
* **Calorie & Macro Target Presets:**
  - Options:
    - **2,000 kcal:** 140g Protein (28%), 220g Carbs (44%), 62g Fat (28%)
    - **2,500 kcal:** 175g Protein (28%), 275g Carbs (44%), 78g Fat (28%)
    - **3,000 kcal:** 210g Protein (28%), 330g Carbs (44%), 93g Fat (28%)
    - **3,500 kcal:** 245g Protein (28%), 385g Carbs (44%), 109g Fat (28%)
  - **Bug Fix:** When tapping a preset, update **all four values** (calories, protein, carbs, fat) simultaneously in state and DB, not just calories and protein.
* **User Identity Metrics (Clean inputs or display cards):**
  - Bodyweight: `70 kg`
  - Height: `175 cm`
  - Training Split: `Push Pull Legs (6-Day Split)`
  - App Version: `v1.0.0 (Play Store Production Build)`

---

## 4. Elimination of Dummy / Placeholder Data

| Item | Current Issue | Replacement / Clean Solution |
|---|---|---|
| Status bar time | Hardcoded `"09:41"` | Dynamic real-time clock in JS (`new Date().toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})`) updated on load. |
| Barcode modal text | `"Select a packaged staple to simulate scan"` | Replace with clean scanner interface: `"Scan barcode or tap registered product"`, with real product barcodes and nutrition labels. |
| Diary dates | Hardcoded `"Today, 11 Nov"`, `"Yesterday, 10 Nov"` | Dynamic formatted dates based on system calendar (`new Date()`). |
| Analytics charts | Hardcoded static percentage bars | Calculate actual calorie adherence and workout volume from `state.logs` or show clean empty state `"No logs recorded yet"`. |
| Food search placeholder | `"Search 120+ Indian items..."` | Expanded database of 50+ actual Indian staple foods, with accurate placeholder: `"Search foods, brands, or regional staples..."`. |
| Next meal timing | Static `"13:30"` | Dynamically computed next meal based on current clock time. |

---

## 5. Play Store Compliance & Android Production Polish

### 5.1 Android Configuration Checklist
1. **`app/build.gradle.kts`:**
   - `compileSdk = 35`, `targetSdk = 35`, `minSdk = 26`
   - `versionCode = 1`
   - `versionName = "1.0.0"`
   - `applicationId = "com.getfit"`
   - Ensure ProGuard / R8 rules are configured to preserve Room entities and Kotlin serialization models.
2. **`AndroidManifest.xml`:**
   - Permissions: Ensure only required permissions are requested:
     - `INTERNET` (for OpenFoodFacts online search)
     - `POST_NOTIFICATIONS` (Android 13+ meal reminders)
     - `VIBRATE` (for rest timer completion)
   - Remove any microphone / audio permissions since voice smart log is decommissioned.
3. **App Launcher Icon:**
   - Verified present: GetFit heartbeat logo in `app/src/main/res/mipmap-*/` with white background `#FFFFFF`.

### 5.2 Room Database Clean Transition
* Update database version in `AppDatabase.kt` if entities change, or set `fallbackToDestructiveMigration()` in `AppDatabase.kt` builder to prevent crashes on schema upgrade during development.
* Remove `WaterLog` from `entities = [FoodItem::class, FoodLog::class, WorkoutLog::class, UserProfile::class, Exercise::class, WeightLog::class, MealSchedule::class]`.
* Remove `waterLogDao()` method.

---

## 6. Step-by-Step Implementation Sequence for the Next Model

The next model should execute the remediation in this exact 6-phase order:

### Phase 1: Android Architecture & Data Layer Cleanup
1. In `app/src/main/java/com/getfit/data/AppDatabase.kt`:
   - Remove `WaterLog::class` from `@Database` entities.
   - Remove `abstract fun waterLogDao(): WaterLogDao`.
2. Delete unused water files:
   - `app/src/main/java/com/getfit/ui/screens/WaterScreen.kt`
   - `app/src/main/java/com/getfit/ui/viewmodel/WaterViewModel.kt`
   - `app/src/main/java/com/getfit/notification/HydrationReminderWorker.kt`
   - `app/src/main/java/com/getfit/data/entity/WaterLog.kt`
   - `app/src/main/java/com/getfit/data/dao/WaterLogDao.kt`
3. Delete unused smart log files:
   - `app/src/main/java/com/getfit/data/util/SmartFoodParser.kt`
   - `app/src/main/java/com/getfit/ui/util/VoiceInputHelper.kt`
4. In `app/src/main/java/com/getfit/data/repository/GetFitRepository.kt`:
   - Remove all methods referencing `waterLogDao`.
5. In `app/src/main/java/com/getfit/ui/viewmodel/FoodViewModel.kt`:
   - Remove `SmartFoodParser`, `smartLogPreview`, `isSmartLogging`, `parseSmartLogPreview`, `confirmSmartLog`, `clearSmartLogPreview`.
   - Remove `todayWaterMl` and `quickLogWater()`.
   - Add `targetMealForAdd: StateFlow<String>` (values: `"Breakfast"`, `"Lunch"`, `"Dinner"`, `"Snacks"`).
   - Ensure `addFoodToLog` writes to `targetMealForAdd.value`.
6. In `app/src/main/java/com/getfit/ui/viewmodel/DashboardViewModel.kt`:
   - Remove `totalWaterMl` and `quickLogWater()`.

### Phase 2: Android UI & Navigation Polish
1. In `app/src/main/java/com/getfit/ui/navigation/NavHost.kt`:
   - Remove `Screen.Water` from `Screen` sealed class.
   - Remove `Screen.Water` from `bottomNavItems` list (reducing it to 4 items: Dashboard, Food, Workout, Profile).
   - Remove `composable(Screen.Water.route)` block.
   - In `CleanTopHeader`: remove `Screen.Water.route` mapping.
2. In `app/src/main/java/com/getfit/ui/screens/DashboardScreen.kt`:
   - Remove parameter `onNavigateToWater: () -> Unit`.
   - Remove the Hydration Card composable.
   - Wire dynamic meal timing.
3. In `app/src/main/java/com/getfit/ui/screens/FoodScreen.kt`:
   - Remove `VoiceInputHelper` imports.
   - Remove `SmartLogPreviewDialog` composable and its invocation.
   - Remove the `SMART NATURAL LANGUAGE QUICK-LOG` card in `SearchAndAddTabContent`.
   - Remove the `WATER INTAKE` card in `DiaryTabContent`.
   - In `DiaryTabContent`, pass the meal name to `onAddFoodClick(mealName)` so `targetMealForAdd` is set correctly.
   - Add a clean header/badge in `SearchAndAddTabContent` indicating which meal is currently targeted.
4. In `app/src/main/java/com/getfit/ui/screens/ProfileScreen.kt`:
   - Ensure the Capsule Theme Toggle is styled cleanly.
   - Update preset selector so selecting a calorie target recalculates all macros.

### Phase 3: Web Simulator & In-IDE Artifact Synchronization
1. In `web/index.html`:
   - Remove bottom navigation button `#nav-water`.
   - In `switchTab()`, remove `'water'` from the active tab list and title mappings.
   - Remove `renderWater()` and `addWater()` functions.
   - Remove water card from `renderDashboard()`.
   - Remove water card from `renderDiaryTab()`.
   - Remove smart NLP card from `renderSearchTab()`.
   - Remove `startVoiceInput()` and `doSmartLog()` functions.
   - Fix `openAddFoodFor(mealName)`:
     ```javascript
     function openAddFoodFor(mealName) {
       state.selectedMealForAdd = mealName;
       state.foodSubTab = 'search';
       render();
     }
     ```
   - In `quickLogFood(id)`: Use `state.selectedMealForAdd || 'Lunch'` instead of hardcoded `'Lunch'`.
   - In `renderSearchTab()`: Add a clear meal indicator pill:
     ```html
     <div class="flex items-center justify-between text-xs px-1">
       <span style="color: var(--text-muted);">Logging to:</span>
       <span class="font-bold px-2 py-0.5 rounded" style="background: var(--card-bg); border: 1px solid var(--border); color: var(--text-primary);">${state.selectedMealForAdd || 'Lunch'}</span>
     </div>
     ```
   - Fix `setDiet(calories)`:
     ```javascript
     function setDiet(c) {
       state.profile.targetCalories = c;
       if (c === 2000) { state.profile.targetProtein = 140; state.profile.targetCarbs = 220; state.profile.targetFat = 62; }
       else if (c === 2500) { state.profile.targetProtein = 175; state.profile.targetCarbs = 275; state.profile.targetFat = 78; }
       else if (c === 3000) { state.profile.targetProtein = 210; state.profile.targetCarbs = 330; state.profile.targetFat = 93; }
       else if (c === 3500) { state.profile.targetProtein = 245; state.profile.targetCarbs = 385; state.profile.targetFat = 109; }
       showToast(`Selected ${c} kcal plan`);
       render();
     }
     ```
   - Fix workout rest timer DOM updating: update `#restTimerVal` directly instead of calling full `render()` each second.
   - Update `sysTime` dynamically on load:
     ```javascript
     const now = new Date();
     document.getElementById('sysTime').innerText = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
     ```
   - Remove all dead functions: `toggleTheme`, `getMealTotals`, `state.customPantryItemInput`.
   - Expand `state.foods` with a comprehensive list of 40+ Indian staple foods with accurate nutritional data.
2. Synchronize `getfit_app.html` in artifact directory with the exact content of `web/index.html`.

### Phase 4: Automated Testing & Verification
1. Update `scratch_test_agent.py`:
   - Change `tabs = ['dashboard', 'food', 'workout', 'profile']` (4 tabs).
   - Add test verifying `#nav-water` is **absent** (`page.locator("#nav-water").count() == 0`).
   - Add test verifying NLP card is **absent** (`page.locator("#smartText").count() == 0`).
   - Add test clicking "Add Food" under Breakfast, selecting an item, and verifying it logs into **Breakfast** (not Lunch).
   - Add test clicking "Add Food" under Dinner, selecting an item, and verifying it logs into **Dinner**.
   - Add test selecting 3000 kcal diet preset and verifying targets update to 3000 kcal, 210g Protein, 330g Carbs, 93g Fat.
   - Add test executing "Log Entire Personal Plan" and verifying all 5 meals appear in Diary.
   - Run: `python scratch_test_agent.py` and confirm **100% tests pass, 0 page errors, 0 console errors**.

### Phase 5: Android Build Verification
1. Run `./gradlew.bat assembleDebug` in PowerShell.
2. Confirm `BUILD SUCCESSFUL` with 0 compile errors.
3. Verify output APK generated at:
   `app/build/outputs/apk/debug/app-debug.apk`.

### Phase 6: Final Review & Sign-Off
1. Verify no mojibake, unescaped characters, or broken styles.
2. Verify light and dark mode screenshots look pixel-perfect and match the Stripe/Linear clean design standard.

---

## 7. Sign-Off & Ready State
This remediation plan contains all instructions, file paths, line-level requirements, and algorithmic logic required to bring GetFit to full Google Play Store production quality. Any model or developer can immediately pick up this plan and execute it systematically.

