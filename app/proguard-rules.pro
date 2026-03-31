# Sonorita ProGuard Rules

# Keep Room database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.sonorita.assistant.data.** { *; }

# Keep ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Keep OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Keep accessibility service
-keep class com.sonorita.assistant.services.SonoritaAccessibilityService { *; }
-keep class com.sonorita.assistant.services.SonoritaNotificationListener { *; }

# Keep receivers
-keep class com.sonorita.assistant.services.BootReceiver { *; }
-keep class com.sonorita.assistant.services.CallReceiver { *; }
-keep class com.sonorita.assistant.services.SmsReceiver { *; }

# Keep service
-keep class com.sonorita.assistant.services.SonoritaService { *; }
-keep class com.sonorita.assistant.ui.FloatingBubbleService { *; }
