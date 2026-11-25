package com.getfit.ui.util

import android.content.Context
import com.getfit.data.PlannedExercise
import org.json.JSONArray
import org.json.JSONObject

object CustomRoutinePreferences {
    private const val PREFS_NAME = "getfit_custom_routines"

    private fun getKey(planName: String, dayName: String, date: String): String =
        "routine_${planName.trim().lowercase()}_${dayName.trim().lowercase()}_$date"

    fun getCustomExercises(
        context: Context,
        planName: String,
        dayName: String,
        date: String = AppDateManager.getTodayDate()
    ): List<PlannedExercise>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(getKey(planName, dayName, date), null) ?: return null
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<PlannedExercise>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    PlannedExercise(
                        exerciseName = obj.optString("exerciseName", ""),
                        sets = obj.optInt("sets", 3),
                        reps = obj.optString("reps", "8-10"),
                        restSeconds = obj.optInt("restSeconds", 90),
                        notes = obj.optString("notes", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    fun saveCustomExercises(
        context: Context,
        planName: String,
        dayName: String,
        exercises: List<PlannedExercise>,
        date: String = AppDateManager.getTodayDate()
    ) {
        val jsonArray = JSONArray()
        exercises.forEach { ex ->
            val obj = JSONObject().apply {
                put("exerciseName", ex.exerciseName)
                put("sets", ex.sets)
                put("reps", ex.reps)
                put("restSeconds", ex.restSeconds)
                put("notes", ex.notes)
            }
            jsonArray.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(getKey(planName, dayName, date), jsonArray.toString())
            .apply()
    }

    fun addExerciseToDay(
        context: Context,
        planName: String,
        dayName: String,
        defaultExercises: List<PlannedExercise>,
        newExercise: PlannedExercise,
        date: String = AppDateManager.getTodayDate()
    ): List<PlannedExercise> {
        val currentList = (getCustomExercises(context, planName, dayName, date) ?: defaultExercises).toMutableList()
        val existingIndex = currentList.indexOfFirst { it.exerciseName.equals(newExercise.exerciseName, ignoreCase = true) }
        if (existingIndex >= 0) {
            currentList[existingIndex] = newExercise
        } else {
            currentList.add(newExercise)
        }
        saveCustomExercises(context, planName, dayName, currentList, date)
        return currentList
    }

    fun removeExerciseFromDay(
        context: Context,
        planName: String,
        dayName: String,
        defaultExercises: List<PlannedExercise>,
        exerciseName: String,
        date: String = AppDateManager.getTodayDate()
    ): List<PlannedExercise> {
        val currentList = (getCustomExercises(context, planName, dayName, date) ?: defaultExercises).toMutableList()
        currentList.removeAll { it.exerciseName.equals(exerciseName, ignoreCase = true) }
        saveCustomExercises(context, planName, dayName, currentList, date)
        return currentList
    }

    fun updateExerciseInDay(
        context: Context,
        planName: String,
        dayName: String,
        defaultExercises: List<PlannedExercise>,
        updatedExercise: PlannedExercise,
        date: String = AppDateManager.getTodayDate()
    ): List<PlannedExercise> {
        val currentList = (getCustomExercises(context, planName, dayName, date) ?: defaultExercises).toMutableList()
        val idx = currentList.indexOfFirst { it.exerciseName.equals(updatedExercise.exerciseName, ignoreCase = true) }
        if (idx >= 0) {
            currentList[idx] = updatedExercise
        } else {
            currentList.add(updatedExercise)
        }
        saveCustomExercises(context, planName, dayName, currentList, date)
        return currentList
    }

    fun resetDayToDefault(
        context: Context,
        planName: String,
        dayName: String,
        date: String = AppDateManager.getTodayDate()
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(getKey(planName, dayName, date))
            .apply()
    }
}
