# ---------------------------------------------------------------------------------------------
# kCal R8 rules. Libraries (Compose, Room, Koin, kotlinx.serialization, Navigation 3, DataStore)
# ship their own consumer rules; only app-specific needs are listed here.
# ---------------------------------------------------------------------------------------------

# Better crash reports: keep file/line info but hide original file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Navigation 3 back-stack keys are restored with kotlinx.serialization after process death
# (also declared as consumer rules in :core:navigation).
-keep class com.alphito.kcal.core.navigation.Route { *; }
-keep class com.alphito.kcal.core.navigation.Route$** { *; }

# Strip verbose logging from release builds (warnings/errors stay).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# kotlinx.coroutines debug agent is never needed at runtime.
-dontwarn kotlinx.coroutines.debug.**
