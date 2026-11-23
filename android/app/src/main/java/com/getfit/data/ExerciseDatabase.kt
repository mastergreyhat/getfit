package com.getfit.data

import com.getfit.data.entity.Exercise

object ExerciseDatabase {
    fun getDefaultExercises(): List<Exercise> = listOf(
        // ===== CHEST =====
        Exercise(name = "Flat Barbell Bench Press", muscleGroup = "Chest", equipment = "Barbell, Bench", description = "Lie on flat bench, grip barbell shoulder-width, lower to chest, press up.", isCompound = true),
        Exercise(name = "Incline Barbell Bench Press", muscleGroup = "Chest", equipment = "Barbell, Incline Bench", description = "Set bench to 30-45°, press barbell from upper chest.", isCompound = true),
        Exercise(name = "Decline Barbell Bench Press", muscleGroup = "Chest", equipment = "Barbell, Decline Bench", description = "Set bench to decline, press barbell from lower chest.", isCompound = true),
        Exercise(name = "Flat Dumbbell Press", muscleGroup = "Chest", equipment = "Dumbbells, Bench", description = "Lie flat, press dumbbells up from chest level.", isCompound = true),
        Exercise(name = "Incline Dumbbell Press", muscleGroup = "Chest", equipment = "Dumbbells, Incline Bench", description = "Set bench to 30-45°, press dumbbells up.", isCompound = true),
        Exercise(name = "Dumbbell Flyes", muscleGroup = "Chest", equipment = "Dumbbells, Bench", description = "Lie flat, arms extended, lower dumbbells in arc to sides.", isCompound = false),
        Exercise(name = "Cable Crossover", muscleGroup = "Chest", equipment = "Cable Machine", description = "Stand between cables, bring handles together in front of chest.", isCompound = false),
        Exercise(name = "Push-Ups", muscleGroup = "Chest", equipment = "Bodyweight", description = "Hands shoulder-width on floor, lower body, push up.", isCompound = true),
        Exercise(name = "Chest Dips", muscleGroup = "Chest", equipment = "Dip Station", description = "Lean forward on dip bars, lower body, push up.", isCompound = true),
        Exercise(name = "Pec Deck Machine", muscleGroup = "Chest", equipment = "Machine", description = "Sit at machine, bring padded arms together in front.", isCompound = false),

        // ===== BACK =====
        Exercise(name = "Deadlift", muscleGroup = "Back", equipment = "Barbell", description = "Stand behind barbell, hinge at hips, grip bar, lift by extending hips and knees.", isCompound = true),
        Exercise(name = "Barbell Row", muscleGroup = "Back", equipment = "Barbell", description = "Bend at hips, pull barbell to lower chest/upper abs.", isCompound = true),
        Exercise(name = "Dumbbell Row", muscleGroup = "Back", equipment = "Dumbbell, Bench", description = "One hand and knee on bench, row dumbbell to hip.", isCompound = true),
        Exercise(name = "Pull-Ups", muscleGroup = "Back", equipment = "Pull-Up Bar", description = "Hang from bar, pull body up until chin over bar.", isCompound = true),
        Exercise(name = "Chin-Ups", muscleGroup = "Back", equipment = "Pull-Up Bar", description = "Underhand grip on bar, pull body up.", isCompound = true),
        Exercise(name = "Lat Pulldown", muscleGroup = "Back", equipment = "Cable Machine", description = "Sit at lat pulldown, pull bar to upper chest.", isCompound = true),
        Exercise(name = "Seated Cable Row", muscleGroup = "Back", equipment = "Cable Machine", description = "Sit at cable row, pull handle to midsection.", isCompound = true),
        Exercise(name = "T-Bar Row", muscleGroup = "Back", equipment = "T-Bar", description = "Straddle T-bar, pull weight to chest.", isCompound = true),
        Exercise(name = "Face Pulls", muscleGroup = "Back", equipment = "Cable Machine", description = "Pull rope attachment toward face, elbows high.", isCompound = false),

        // ===== SHOULDERS =====
        Exercise(name = "Overhead Press (Barbell)", muscleGroup = "Shoulders", equipment = "Barbell", description = "Press barbell overhead from front of shoulders.", isCompound = true),
        Exercise(name = "Dumbbell Shoulder Press", muscleGroup = "Shoulders", equipment = "Dumbbells", description = "Seated or standing, press dumbbells overhead.", isCompound = true),
        Exercise(name = "Arnold Press", muscleGroup = "Shoulders", equipment = "Dumbbells", description = "Start palms facing you, rotate and press overhead.", isCompound = true),
        Exercise(name = "Lateral Raises", muscleGroup = "Shoulders", equipment = "Dumbbells", description = "Stand, raise dumbbells to sides until arms parallel to floor.", isCompound = false),
        Exercise(name = "Front Raises", muscleGroup = "Shoulders", equipment = "Dumbbells", description = "Stand, raise dumbbells in front to shoulder height.", isCompound = false),
        Exercise(name = "Reverse Flyes", muscleGroup = "Shoulders", equipment = "Dumbbells", description = "Bend forward, raise dumbbells to sides targeting rear delts.", isCompound = false),
        Exercise(name = "Upright Row", muscleGroup = "Shoulders", equipment = "Barbell", description = "Pull barbell straight up to chin level, elbows high.", isCompound = true),
        Exercise(name = "Shrugs", muscleGroup = "Shoulders", equipment = "Dumbbells", description = "Hold heavy dumbbells at sides, shrug shoulders up.", isCompound = false),

        // ===== BICEPS =====
        Exercise(name = "Barbell Curl", muscleGroup = "Biceps", equipment = "Barbell", description = "Stand with barbell, curl weight up by bending elbows.", isCompound = false),
        Exercise(name = "Dumbbell Curl", muscleGroup = "Biceps", equipment = "Dumbbells", description = "Stand with dumbbells, curl alternating or together.", isCompound = false),
        Exercise(name = "Hammer Curl", muscleGroup = "Biceps", equipment = "Dumbbells", description = "Neutral grip dumbbells, curl up without rotating.", isCompound = false),
        Exercise(name = "Preacher Curl", muscleGroup = "Biceps", equipment = "EZ Bar, Preacher Bench", description = "Rest arms on preacher pad, curl EZ bar up.", isCompound = false),
        Exercise(name = "Incline Dumbbell Curl", muscleGroup = "Biceps", equipment = "Dumbbells, Incline Bench", description = "Lie on incline bench, let arms hang, curl up.", isCompound = false),
        Exercise(name = "Concentration Curl", muscleGroup = "Biceps", equipment = "Dumbbell", description = "Sit, elbow on inner thigh, curl dumbbell up.", isCompound = false),
        Exercise(name = "Cable Curl", muscleGroup = "Biceps", equipment = "Cable Machine", description = "Stand at cable machine, curl bar or rope attachment.", isCompound = false),

        // ===== TRICEPS =====
        Exercise(name = "Close-Grip Bench Press", muscleGroup = "Triceps", equipment = "Barbell, Bench", description = "Bench press with hands closer than shoulder-width.", isCompound = true),
        Exercise(name = "Tricep Dips", muscleGroup = "Triceps", equipment = "Dip Station", description = "Upright body on dip bars, lower and press up.", isCompound = true),
        Exercise(name = "Skull Crushers", muscleGroup = "Triceps", equipment = "EZ Bar, Bench", description = "Lie flat, lower EZ bar to forehead, extend arms.", isCompound = false),
        Exercise(name = "Tricep Pushdown", muscleGroup = "Triceps", equipment = "Cable Machine", description = "Stand at cable, push bar/rope down extending elbows.", isCompound = false),
        Exercise(name = "Overhead Tricep Extension", muscleGroup = "Triceps", equipment = "Dumbbell", description = "Hold dumbbell overhead with both hands, lower behind head, extend.", isCompound = false),
        Exercise(name = "Tricep Kickbacks", muscleGroup = "Triceps", equipment = "Dumbbell", description = "Bend over, extend arm back squeezing tricep.", isCompound = false),

        // ===== LEGS =====
        Exercise(name = "Barbell Back Squat", muscleGroup = "Legs", equipment = "Barbell, Squat Rack", description = "Bar on upper back, squat down until thighs parallel, stand up.", isCompound = true),
        Exercise(name = "Front Squat", muscleGroup = "Legs", equipment = "Barbell, Squat Rack", description = "Bar on front delts, squat down, stand up.", isCompound = true),
        Exercise(name = "Leg Press", muscleGroup = "Legs", equipment = "Leg Press Machine", description = "Sit in machine, press platform away by extending legs.", isCompound = true),
        Exercise(name = "Romanian Deadlift", muscleGroup = "Legs", equipment = "Barbell", description = "Hold barbell, hinge at hips keeping legs slightly bent, lower bar.", isCompound = true),
        Exercise(name = "Bulgarian Split Squat", muscleGroup = "Legs", equipment = "Dumbbells, Bench", description = "Rear foot on bench, squat on front leg.", isCompound = true),
        Exercise(name = "Walking Lunges", muscleGroup = "Legs", equipment = "Dumbbells", description = "Step forward into lunge, alternate legs while walking.", isCompound = true),
        Exercise(name = "Leg Extension", muscleGroup = "Legs", equipment = "Machine", description = "Sit at machine, extend legs by straightening knees.", isCompound = false),
        Exercise(name = "Leg Curl", muscleGroup = "Legs", equipment = "Machine", description = "Lie face down, curl legs up bending knees.", isCompound = false),
        Exercise(name = "Calf Raises (Standing)", muscleGroup = "Legs", equipment = "Machine/Smith Machine", description = "Stand on edge, raise heels by extending ankles.", isCompound = false),
        Exercise(name = "Calf Raises (Seated)", muscleGroup = "Legs", equipment = "Seated Calf Machine", description = "Sit at machine, raise heels by extending ankles.", isCompound = false),
        Exercise(name = "Hip Thrust", muscleGroup = "Legs", equipment = "Barbell, Bench", description = "Upper back on bench, barbell on hips, thrust hips up.", isCompound = true),
        Exercise(name = "Goblet Squat", muscleGroup = "Legs", equipment = "Dumbbell/Kettlebell", description = "Hold weight at chest, squat down, stand up.", isCompound = true),

        // ===== CORE =====
        Exercise(name = "Plank", muscleGroup = "Core", equipment = "Bodyweight", description = "Hold push-up position on forearms, keep body straight.", isCompound = false),
        Exercise(name = "Crunches", muscleGroup = "Core", equipment = "Bodyweight", description = "Lie on back, knees bent, curl shoulders off floor.", isCompound = false),
        Exercise(name = "Hanging Leg Raise", muscleGroup = "Core", equipment = "Pull-Up Bar", description = "Hang from bar, raise legs to 90° or higher.", isCompound = false),
        Exercise(name = "Cable Woodchop", muscleGroup = "Core", equipment = "Cable Machine", description = "Rotate torso pulling cable diagonally across body.", isCompound = false),
        Exercise(name = "Ab Wheel Rollout", muscleGroup = "Core", equipment = "Ab Wheel", description = "Kneel, roll wheel forward extending body, roll back.", isCompound = false),
        Exercise(name = "Russian Twist", muscleGroup = "Core", equipment = "Bodyweight/Plate", description = "Sit with legs up, rotate torso side to side.", isCompound = false),
        Exercise(name = "Mountain Climbers", muscleGroup = "Core", equipment = "Bodyweight", description = "Push-up position, alternate driving knees to chest.", isCompound = true),
        Exercise(name = "Dead Bug", muscleGroup = "Core", equipment = "Bodyweight", description = "Lie on back, arms up, extend opposite arm and leg.", isCompound = false)
    )
}
