package com.getfit.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.getfit.data.dao.*
import com.getfit.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Database(
    entities = [
        FoodItem::class,
        FoodLog::class,
        Exercise::class,
        WorkoutLog::class,
        UserProfile::class,
        MealSchedule::class,
        WeightLog::class,
        TrashItem::class,
        WaterLog::class,
        StopwatchRecord::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun foodLogDao(): FoodLogDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutLogDao(): WorkoutLogDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun mealScheduleDao(): MealScheduleDao
    abstract fun weightLogDao(): WeightLogDao
    abstract fun trashDao(): TrashDao
    abstract fun waterLogDao(): WaterLogDao
    abstract fun stopwatchRecordDao(): StopwatchRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE food_items ADD COLUMN source TEXT NOT NULL DEFAULT 'ICMR-NIN Verified'")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `trash_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `itemType` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `subtitle` TEXT NOT NULL,
                        `dataJson` TEXT NOT NULL,
                        `deletedAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `water_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` TEXT NOT NULL,
                        `amountMl` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_water_logs_date` ON `water_logs` (`date`)")
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `stopwatch_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `elapsedMillis` INTEGER NOT NULL,
                        `formattedTime` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "getfit_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    // Prepopulate foods
                                    database.foodDao().insertAllFoods(FoodDatabase.getDefaultFoods())
                                    // Prepopulate exercises
                                    database.exerciseDao().insertAllExercises(ExerciseDatabase.getDefaultExercises())
                                    // Create default user profile
                                    database.userProfileDao().insertOrUpdate(UserProfile())
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
