# Navigation 3 saves the back stack with kotlinx.serialization. Route keys are looked up
# reflectively when the stack is restored after process death, so keep them and their serializers.
-keep class com.alphito.kcal.core.navigation.** implements androidx.navigation3.runtime.NavKey { *; }
-keepclassmembers class com.alphito.kcal.core.navigation.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.alphito.kcal.core.navigation.**$$serializer { *; }
