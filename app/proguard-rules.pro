# ─── 话唠棋王 ProGuard 规则 ───
# B+ 方案：全本地化 Android App

# ─── Kotlin ───
-keepattributes *Annotation*
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ─── Kotlin Coroutines ───
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ─── OkHttp ───
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ─── Gson ───
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.hualao.qiwang.model.** { *; }
-keep class com.hualao.qiwang.data.** { *; }

# ─── Compose ───
-keep class androidx.compose.** { *; }

# ─── AndroidX Security (EncryptedSharedPreferences) ───
-keep class androidx.security.crypto.** { *; }

# ─── Room ───
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ─── Native / JNI ───
-keepclasseswithmembernames class * {
    native <methods>;
}

# ─── Data classes (used in Gson/JSON) ───
-keep class com.hualao.qiwang.** { *; }

# ─── Keep enum values ───
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
