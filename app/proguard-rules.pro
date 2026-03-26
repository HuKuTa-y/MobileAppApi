# ============================================================================
# OPTIMIZED PROGUARD RULES FOR LAWAPP
# ============================================================================

# 🔥 Базовые настройки
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# 🔥 Сохраняем информацию для отладки (можно закомментировать для минимального размера)
-keepattributes SourceFile,LineNumberTable

# ============================================================================
# GSON RULES (критично для парсинга JSON)
# ============================================================================
-keepattributes Signature
-keepattributes *Annotation*

# ✅ Сохраняем ваши модели (замените пакет на ваш, если отличается)
-keep class com.example.lawapp.models.** { *; }
-keepclassmembers class com.example.lawapp.models.** { *; }

# ✅ Сохраняем поля с @SerializedName
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ✅ Разрешаем обфускацию полей, но сохраняем имена для Gson
-keepclassmembernames class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ============================================================================
# RETROFIT RULES
# ============================================================================
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# ✅ Сохраняем интерфейсы API
-keep interface com.example.lawapp.api.** { *; }
-keep class com.example.lawapp.api.** { *; }

# ============================================================================
# OKHTTP RULES
# ============================================================================
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# ============================================================================
# ANDROIDX RULES
# ============================================================================
-keep class androidx.** { *; }
-dontwarn androidx.**

# ✅ RecyclerView адаптеры
-keep class com.example.lawapp.adapters.** { *; }
-keepclassmembers class com.example.lawapp.adapters.** { *; }

# ✅ Activities и Fragments (чтобы не сломалась навигация)
-keep class com.example.lawapp.**Activity { *; }
-keep class com.example.lawapp.**Fragment { *; }

# ============================================================================
# JSON CACHE MANAGER (если используете CacheManager)
# ============================================================================
-keep class com.example.lawapp.cache.** { *; }
-keepclassmembers class com.example.lawapp.cache.** { *; }

# ============================================================================
# UTILS И ВСПОМОГАТЕЛЬНЫЕ КЛАССЫ
# ============================================================================
-keep class com.example.lawapp.utils.** { *; }
-keepclassmembers class com.example.lawapp.utils.** { *; }

# ============================================================================
# REFLECTION (если где-то используется)
# ============================================================================
-keepattributes *Annotation*, EnclosingMethod, InnerClasses

# ============================================================================
# ОПТИМИЗАЦИЯ: удаляем лишние логи в release-сборке
# ============================================================================
# ❌ Раскомментируйте, если хотите убрать все Log.d/logcat в релизе:
# -assumenosideeffects class android.util.Log {
#     public static *** d(...);
#     public static *** v(...);
#     public static *** i(...);
# }