# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Gson / JSON serialization models
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.getfit.data.entity.** { *; }
-keep class com.getfit.data.remote.** { *; }
-keep class com.getfit.data.sync.** { *; }

# Coroutines & Compose
-dontwarn kotlinx.coroutines.**
-dontwarn java.lang.invoke.**
