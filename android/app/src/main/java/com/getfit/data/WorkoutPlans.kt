package com.getfit.data

data class PlannedExercise(
    val exerciseName: String,
    val sets: Int,
    val reps: String,
    val restSeconds: Int = 90,
    val notes: String = ""
)

data class WorkoutDay(
    val dayName: String,
    val muscleGroups: String,
    val exercises: List<PlannedExercise>
)

data class WorkoutPlan(
    val name: String,
    val description: String,
    val daysPerWeek: Int,
    val days: List<WorkoutDay>
)

object WorkoutPlans {
    fun getAllPlans(): List<WorkoutPlan> = listOf(
        getPPLPlan(),
        getUpperLowerPlan(),
        getFullBodyPlan(),
        getBroSplitPlan()
    )

    fun getPPLPlan(): WorkoutPlan = WorkoutPlan(
        name = "PPL",
        description = "Push/Pull/Legs - 6 day split for intermediate to advanced lifters",
        daysPerWeek = 6,
        days = listOf(
            WorkoutDay("Push A", "Chest, Shoulders, Triceps", listOf(
                PlannedExercise("Flat Barbell Bench Press", 3, "6-8", 120, "Focus on progressive overload"),
                PlannedExercise("Incline Dumbbell Press", 3, "8-10", 90),
                PlannedExercise("Dumbbell Shoulder Press", 3, "8-10", 90),
                PlannedExercise("Lateral Raises", 3, "12-15", 60),
                PlannedExercise("Tricep Pushdown", 3, "10-12", 60),
                PlannedExercise("Overhead Tricep Extension", 3, "10-12", 60)
            )),
            WorkoutDay("Pull A", "Back, Biceps", listOf(
                PlannedExercise("Deadlift", 3, "5-6", 180, "Main compound lift"),
                PlannedExercise("Barbell Row", 3, "8-10", 90),
                PlannedExercise("Lat Pulldown", 3, "10-12", 90),
                PlannedExercise("Face Pulls", 3, "12-15", 60),
                PlannedExercise("Barbell Curl", 3, "8-10", 60),
                PlannedExercise("Hammer Curl", 3, "10-12", 60)
            )),
            WorkoutDay("Legs A", "Quads, Hamstrings, Calves", listOf(
                PlannedExercise("Barbell Back Squat", 3, "6-8", 150, "King of leg exercises"),
                PlannedExercise("Romanian Deadlift", 3, "8-10", 90),
                PlannedExercise("Leg Press", 3, "10-12", 90),
                PlannedExercise("Leg Extension", 3, "12-15", 60),
                PlannedExercise("Leg Curl", 3, "10-12", 60),
                PlannedExercise("Calf Raises (Standing)", 3, "12-15", 60)
            )),
            WorkoutDay("Push B", "Chest, Shoulders, Triceps", listOf(
                PlannedExercise("Overhead Press (Barbell)", 3, "6-8", 120, "Main shoulder press"),
                PlannedExercise("Incline Barbell Bench Press", 3, "8-10", 90),
                PlannedExercise("Cable Crossover", 3, "12-15", 60),
                PlannedExercise("Lateral Raises", 3, "12-15", 60),
                PlannedExercise("Skull Crushers", 3, "8-10", 60),
                PlannedExercise("Tricep Kickbacks", 3, "12-15", 60)
            )),
            WorkoutDay("Pull B", "Back, Biceps", listOf(
                PlannedExercise("Pull-Ups", 3, "6-10", 120),
                PlannedExercise("Dumbbell Row", 3, "8-10", 90),
                PlannedExercise("Seated Cable Row", 3, "10-12", 90),
                PlannedExercise("Reverse Flyes", 3, "12-15", 60),
                PlannedExercise("Incline Dumbbell Curl", 3, "10-12", 60),
                PlannedExercise("Concentration Curl", 3, "10-12", 60)
            )),
            WorkoutDay("Legs B", "Quads, Hamstrings, Glutes, Calves", listOf(
                PlannedExercise("Front Squat", 3, "6-8", 150),
                PlannedExercise("Bulgarian Split Squat", 3, "8-10", 90),
                PlannedExercise("Hip Thrust", 3, "8-10", 90),
                PlannedExercise("Walking Lunges", 3, "12-15", 60),
                PlannedExercise("Leg Curl", 3, "10-12", 60),
                PlannedExercise("Calf Raises (Seated)", 3, "15-20", 60)
            ))
        )
    )

    fun getUpperLowerPlan(): WorkoutPlan = WorkoutPlan(
        name = "Upper/Lower",
        description = "Upper/Lower split - 4 day program for beginners to intermediate",
        daysPerWeek = 4,
        days = listOf(
            WorkoutDay("Upper A", "Chest, Back, Shoulders, Arms", listOf(
                PlannedExercise("Flat Barbell Bench Press", 3, "6-8", 120),
                PlannedExercise("Barbell Row", 3, "6-8", 120),
                PlannedExercise("Dumbbell Shoulder Press", 3, "8-10", 90),
                PlannedExercise("Lat Pulldown", 3, "10-12", 90),
                PlannedExercise("Dumbbell Curl", 3, "10-12", 60),
                PlannedExercise("Tricep Pushdown", 3, "10-12", 60)
            )),
            WorkoutDay("Lower A", "Quads, Hamstrings, Calves, Core", listOf(
                PlannedExercise("Barbell Back Squat", 3, "6-8", 150),
                PlannedExercise("Romanian Deadlift", 3, "8-10", 120),
                PlannedExercise("Leg Press", 3, "10-12", 90),
                PlannedExercise("Leg Curl", 3, "10-12", 60),
                PlannedExercise("Calf Raises (Standing)", 3, "12-15", 60),
                PlannedExercise("Plank", 3, "60s", 60)
            )),
            WorkoutDay("Upper B", "Chest, Back, Shoulders, Arms", listOf(
                PlannedExercise("Overhead Press (Barbell)", 3, "6-8", 120),
                PlannedExercise("Pull-Ups", 3, "6-10", 120),
                PlannedExercise("Incline Dumbbell Press", 3, "8-10", 90),
                PlannedExercise("Seated Cable Row", 3, "10-12", 90),
                PlannedExercise("Hammer Curl", 3, "10-12", 60),
                PlannedExercise("Skull Crushers", 3, "10-12", 60)
            )),
            WorkoutDay("Lower B", "Quads, Hamstrings, Glutes, Core", listOf(
                PlannedExercise("Deadlift", 3, "5-6", 180),
                PlannedExercise("Front Squat", 3, "8-10", 120),
                PlannedExercise("Bulgarian Split Squat", 3, "10-12", 90),
                PlannedExercise("Hip Thrust", 3, "10-12", 90),
                PlannedExercise("Calf Raises (Seated)", 3, "15-20", 60),
                PlannedExercise("Hanging Leg Raise", 3, "10-15", 60)
            ))
        )
    )

    fun getFullBodyPlan(): WorkoutPlan = WorkoutPlan(
        name = "Full Body",
        description = "Full Body - 3 day program ideal for beginners",
        daysPerWeek = 3,
        days = listOf(
            WorkoutDay("Day A", "Full Body (Push Focus)", listOf(
                PlannedExercise("Barbell Back Squat", 3, "6-8", 150),
                PlannedExercise("Flat Barbell Bench Press", 3, "6-8", 120),
                PlannedExercise("Barbell Row", 3, "8-10", 90),
                PlannedExercise("Dumbbell Shoulder Press", 3, "8-10", 90),
                PlannedExercise("Dumbbell Curl", 2, "10-12", 60),
                PlannedExercise("Calf Raises (Standing)", 3, "12-15", 60)
            )),
            WorkoutDay("Day B", "Full Body (Pull Focus)", listOf(
                PlannedExercise("Deadlift", 3, "5-6", 180),
                PlannedExercise("Overhead Press (Barbell)", 3, "6-8", 120),
                PlannedExercise("Pull-Ups", 3, "6-10", 90),
                PlannedExercise("Leg Press", 3, "10-12", 90),
                PlannedExercise("Tricep Pushdown", 2, "10-12", 60),
                PlannedExercise("Plank", 3, "60s", 60)
            )),
            WorkoutDay("Day C", "Full Body (Legs Focus)", listOf(
                PlannedExercise("Front Squat", 3, "6-8", 150),
                PlannedExercise("Incline Dumbbell Press", 3, "8-10", 90),
                PlannedExercise("Seated Cable Row", 3, "8-10", 90),
                PlannedExercise("Romanian Deadlift", 3, "8-10", 90),
                PlannedExercise("Lateral Raises", 3, "12-15", 60),
                PlannedExercise("Hammer Curl", 2, "10-12", 60)
            ))
        )
    )

    fun getBroSplitPlan(): WorkoutPlan = WorkoutPlan(
        name = "Bro Split",
        description = "Classic Bro Split - 5 day bodybuilding program, one muscle group per day",
        daysPerWeek = 5,
        days = listOf(
            WorkoutDay("Chest Day", "Chest", listOf(
                PlannedExercise("Flat Barbell Bench Press", 3, "6-8", 120),
                PlannedExercise("Incline Dumbbell Press", 3, "8-10", 90),
                PlannedExercise("Decline Barbell Bench Press", 3, "8-10", 90),
                PlannedExercise("Dumbbell Flyes", 3, "12-15", 60),
                PlannedExercise("Cable Crossover", 3, "12-15", 60),
                PlannedExercise("Push-Ups", 3, "To failure", 60)
            )),
            WorkoutDay("Back Day", "Back", listOf(
                PlannedExercise("Deadlift", 3, "5-6", 180),
                PlannedExercise("Pull-Ups", 3, "6-10", 120),
                PlannedExercise("Barbell Row", 3, "8-10", 90),
                PlannedExercise("Dumbbell Row", 3, "8-10", 90),
                PlannedExercise("Lat Pulldown", 3, "10-12", 90),
                PlannedExercise("Face Pulls", 3, "12-15", 60)
            )),
            WorkoutDay("Shoulder Day", "Shoulders", listOf(
                PlannedExercise("Overhead Press (Barbell)", 3, "6-8", 120),
                PlannedExercise("Arnold Press", 3, "8-10", 90),
                PlannedExercise("Lateral Raises", 3, "12-15", 60),
                PlannedExercise("Front Raises", 3, "12-15", 60),
                PlannedExercise("Reverse Flyes", 3, "12-15", 60),
                PlannedExercise("Shrugs", 3, "10-12", 60)
            )),
            WorkoutDay("Arms Day", "Biceps, Triceps", listOf(
                PlannedExercise("Barbell Curl", 3, "8-10", 90),
                PlannedExercise("Close-Grip Bench Press", 3, "8-10", 90),
                PlannedExercise("Preacher Curl", 3, "10-12", 60),
                PlannedExercise("Skull Crushers", 3, "10-12", 60),
                PlannedExercise("Hammer Curl", 3, "10-12", 60),
                PlannedExercise("Tricep Pushdown", 3, "10-12", 60),
                PlannedExercise("Concentration Curl", 2, "12-15", 60),
                PlannedExercise("Tricep Kickbacks", 2, "12-15", 60)
            )),
            WorkoutDay("Leg Day", "Quads, Hamstrings, Glutes, Calves", listOf(
                PlannedExercise("Barbell Back Squat", 3, "6-8", 150),
                PlannedExercise("Romanian Deadlift", 3, "8-10", 120),
                PlannedExercise("Leg Press", 3, "10-12", 90),
                PlannedExercise("Bulgarian Split Squat", 3, "10-12", 90),
                PlannedExercise("Leg Extension", 3, "12-15", 60),
                PlannedExercise("Leg Curl", 3, "10-12", 60),
                PlannedExercise("Calf Raises (Standing)", 3, "12-15", 60)
            ))
        )
    )
}
