# R8 rules for Heartrace App

# Preserve source locations for crash deobfuscation.
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Remove Android Log calls in release builds
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
