# Project specific ProGuard/R8 rules.
#
# The `preview` build type enables minification, so anything reached only by
# reflection or JNI must survive R8. The entries below are grouped by the reason
# they are required.

# --- Line numbers keep crash reports usable in minified builds. -------------
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes *Annotation*

# --- MapLibre -----------------------------------------------------------------
# MapLibre resolves style layers, sources and expressions by name at runtime, and
# its JNI layer looks these classes up by exact name.
-keep class org.maplibre.geojson.** { *; }
-keep class org.maplibre.android.** { *; }
-keep class org.maplibre.android.turf.** { *; }
-dontwarn org.maplibre.**
# Native callbacks into Java.
-keepclassmembers class * implements org.maplibre.android.maps.MapLibreMap$* {
    public *;
}
# Styles are parsed from JSON, so keep the Gson type adapters it relies on.
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# --- Retrofit / OkHttp / Moshi ------------------------------------------------
# Retrofit builds its service implementations from generic signatures.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <1>
# Moshi reflects over the data classes it serialises.
-keep class com.example.data.** { *; }
-keepclassmembers class com.example.data.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# --- Room ---------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# --- Kotlin coroutines --------------------------------------------------------
-dontwarn kotlinx.coroutines.**

# --- WebView (the mosque map no longer uses one, but Quran pages may) ---------
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# --- Crash readability --------------------------------------------------------
# Keep the enum name of the 21 themes: it is persisted as a preference string and
# resolved by name via AppTheme.fromStored().
-keepclassmembers enum com.example.ui.theme.AppTheme {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public static ** entries;
}
