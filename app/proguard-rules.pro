# Add project specific ProGuard rules here.

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Media3 / ExoPlayer (consumer rules in AAR handle the rest)
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.ui.** { *; }
-keep class androidx.media3.session.** { *; }
-dontwarn androidx.media3.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep data classes for serialization
-keep class com.example.vista.domain.model.** { *; }
-keep class com.example.vista.data.local.entity.** { *; }

# Retrofit + Gson DTOs - parsed reflectively by Gson; R8 renaming these
# breaks JSON parsing (empty catalog / no movies).
-keep class com.example.vista.data.remote.** { *; }
-keep class com.example.vista.data.remote.** { <fields>; }
-keepclasseswithmembers class com.example.vista.data.remote.** {
    <init>(...);
    <fields>;
}
-keepclassmembers class com.example.vista.data.remote.** { <fields>; }

# Google Play App Open Ads
-keep class com.google.android.gms.ads.appopen.** { *; }

# Gson - keep models with SerializedName
-keep class com.google.gson.annotations.** { *; }
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,Signature
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
