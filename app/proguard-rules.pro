# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# Keep app models
-keep class com.keepereye.app.history.** { *; }
-keep class com.keepereye.app.obstacle.** { *; }
