# Add project specific ProGuard rules here.

# Keep WiFi P2P framework classes
-keep class android.net.wifi.p2p.** { *; }

# Keep our data models (used with kotlinx.serialization reflection)
-keep class com.thazh.share.data.** { *; }
-keepclassmembers class com.thazh.share.data.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.thazh.share.**$$serializer { *; }
-keepclassmembers class com.thazh.share.** {
    *** Companion;
}
-keepclasseswithmembers class com.thazh.share.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# Keep Glasmio & geometry classes
-keep class io.github.neoruaa.glasmio.** { *; }
-keep class com.kyant.capsule.** { *; }
-keep class com.kyant.shapes.** { *; }

-dontoptimize
-dontwarn org.jetbrains.annotations.**
