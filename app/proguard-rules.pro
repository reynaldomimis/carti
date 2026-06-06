# CARTI ELITE SECURITY: ProGuard/R8 Obfuscation Rules

# Preserve Line Numbers for Crash Reporting (Optional but recommended)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# GSON & JSON Rules (Crucial for AI Action Parsing)
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.upreyvan.carti.model.** { *; }
-keep class com.upreyvan.carti.data.ai.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Appwrite
-keep class io.appwrite.** { *; }

# Glide
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public class * extends com.bumptech.glide.module.LibraryGlideModule
-keep class com.bumptech.glide.** { *; }

# Security Guard - Keep it but let it be obfuscated except for check method
-keep class com.upreyvan.carti.util.SecurityGuard {
    public static void checkIntegrity(android.content.Context);
}

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# Navigation
-keep class androidx.navigation.** { *; }
