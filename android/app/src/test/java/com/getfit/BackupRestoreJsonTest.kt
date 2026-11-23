package com.getfit

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class BackupRestoreJsonTest {

    @Test
    fun testParseBackupJsonFile() {
        val file = File("../../GetFit_Full_Backup.json")
        assertTrue("Backup file must exist at ${file.absolutePath}", file.exists())
        val jsonString = file.readText()
        assertTrue("Backup content must not be blank", jsonString.isNotBlank())

        val root = JSONObject(jsonString)
        assertEquals(1, root.getInt("version"))
        assertEquals("GetFit", root.getString("app"))
        assertTrue(root.has("profile"))
        assertTrue(root.has("exercises"))
        assertTrue(root.has("workoutLogs"))
        assertTrue(root.has("foodLogs"))
        assertTrue(root.has("waterLogs"))
        assertTrue(root.has("weightLogs"))
        assertTrue(root.has("customFoods"))

        val pObj = root.getJSONObject("profile")
        assertEquals("Aswin", pObj.getString("name"))
        assertEquals(25, pObj.getInt("age"))
        assertEquals(65.0, pObj.getDouble("weight"), 0.01)
        assertEquals("Bro Split", pObj.getString("workoutPlan"))
        assertEquals(2799, pObj.getInt("dailyCalorieTarget"))

        val exArr = root.getJSONArray("exercises")
        assertEquals(60, exArr.length())
        for (i in 0 until exArr.length()) {
            val ex = exArr.getJSONObject(i)
            assertTrue(ex.getString("name").isNotBlank())
            assertTrue(ex.getString("muscleGroup").isNotBlank())
        }

        val wArr = root.getJSONArray("workoutLogs")
        assertEquals(15, wArr.length())
        for (i in 0 until wArr.length()) {
            val w = wArr.getJSONObject(i)
            assertTrue(w.getString("exerciseName").isNotBlank())
            assertTrue(w.getString("date").isNotBlank())
            assertTrue(w.getInt("reps") > 0)
        }

        val fArr = root.getJSONArray("foodLogs")
        assertEquals(15, fArr.length())
        for (i in 0 until fArr.length()) {
            val f = fArr.getJSONObject(i)
            assertTrue(f.getString("foodName").isNotBlank())
            assertTrue(f.getString("date").isNotBlank())
            assertTrue(f.getDouble("totalCalories") > 0.0)
        }

        val waterArr = root.getJSONArray("waterLogs")
        assertEquals(3, waterArr.length())
        for (i in 0 until waterArr.length()) {
            val water = waterArr.getJSONObject(i)
            assertTrue(water.getString("date").isNotBlank())
            assertTrue(water.getInt("amountMl") > 0)
        }

        val cfArr = root.getJSONArray("customFoods")
        assertEquals(9, cfArr.length())
        for (i in 0 until cfArr.length()) {
            val cf = cfArr.getJSONObject(i)
            assertTrue(cf.getString("name").isNotBlank())
            assertTrue(cf.getBoolean("isCustom"))
        }
    }
}
