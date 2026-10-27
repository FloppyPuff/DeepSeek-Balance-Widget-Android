# ========================================================
# Jetpack Glance & AppWidget
# ========================================================
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * implements androidx.glance.appwidget.action.ActionCallback { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class com.example.deepseekwidget.ui.DeepSeekBalanceWidget { *; }
-keep class com.example.deepseekwidget.ui.DeepSeekWidgetReceiver { *; }
-keep class com.example.deepseekwidget.ui.RefreshBalanceAction { *; }
-keep class com.example.deepseekwidget.ui.ToggleMaskAction { *; }

# ========================================================
# Quick Settings Tile Service
# ========================================================
-keep class * extends android.service.quicksettings.TileService { *; }
-keep class com.example.deepseekwidget.service.DeepSeekTileService { *; }

# ========================================================
# WorkManager, Room & App Startup
# ========================================================
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.Worker { *; }
-keep class com.example.deepseekwidget.work.BalanceSyncWorker { *; }
-keep class androidx.work.impl.** { *; }
-keep class androidx.work.impl.WorkDatabase_Impl {
    public <init>();
    *;
}
-keep class * extends androidx.room.RoomDatabase {
    public <init>();
    *;
}
-keep class * extends androidx.startup.Initializer { *; }
-keep class androidx.startup.** { *; }

# ========================================================
# OkHttp & Okio
# ========================================================
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ========================================================
# Suppress compile-time annotations warnings (Tink / Guava)
# ========================================================
-dontwarn com.google.errorprone.annotations.**
-dontwarn org.checkerframework.**
-dontwarn com.google.j2objc.annotations.**

# ========================================================
# Data Models & Serialization
# ========================================================
-keep class com.example.deepseekwidget.data.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep serializable fields and serializers
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep class * implements kotlinx.serialization.KSerializer { *; }
