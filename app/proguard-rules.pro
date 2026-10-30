# Add project specific ProGuard / R8 rules here.

# Kotpref reflection on KProperty
-keep class * implements kotlin.reflect.KProperty { *; }
-keep class com.chibatching.kotpref.** { *; }

# Vendored content provider entities (CalendarProvider)
-keep class * extends me.everything.providers.core.Entity { *; }
-keepclassmembers class * extends me.everything.providers.core.Entity { *; }
-keepattributes *Annotation*

# EventBus @Subscribe methods
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Gson / Retrofit models & Backup schema
-keepattributes Signature
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.tommasoberlose.anotherwidget.models.** { *; }
-keep class com.tommasoberlose.anotherwidget.helpers.BackupPayload { *; }
-keep class com.kwabenaberko.openweathermaplib.** { *; }

# Joda-Time
-dontwarn org.joda.convert.**

# OkHttp platform warnings
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
