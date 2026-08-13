# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Compose
-keepclasseswithmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.Query <methods>;
    @androidx.room.Insert <methods>;
    @androidx.room.Delete <methods>;
    @androidx.room.Update <methods>;
}

# Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKd
-keep,allowobfuscation,allowshrinking class kotlinx.serialization.internal.**
-keep,allowobfuscation,allowshrinking class * implements kotlinx.serialization.KSerializer {
    <init>();
}
-keep,allowobfuscation,allowshrinking class * {
    @kotlinx.serialization.Serializable <fields>;
}

# Media3 Session
-keep class androidx.media3.session.** { *; }

# JAudioTagger
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn org.jaudiotagger.**

# Queue state is persisted with Gson and must remain compatible across updates.
-keep class com.example.xargoosh.domain.queue.QueueItem { *; }
-keep class com.example.xargoosh.domain.models.MusicTrack { *; }
