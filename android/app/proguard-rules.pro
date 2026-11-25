# Room entities & DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# WorkManager workers instantiated via reflection
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Gson / TypeToken / JSON serialization models
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.getfit.data.entity.** { *; }
-keep class com.getfit.data.remote.** { *; }
-keep class com.getfit.data.sync.** { *; }
-keep class com.getfit.ui.viewmodel.MealPlanHistoryEntry { *; }

# Enums parsed by name
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Coroutines & Compose
-dontwarn kotlinx.coroutines.**
-dontwarn java.lang.invoke.**
