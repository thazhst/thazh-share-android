# ProGuard / R8 configuration for Thazh Share (Uptodown & Release Store Build)

# Keep Wi-Fi P2P & Bluetooth & NFC framework classes
-keep class android.net.wifi.p2p.** { *; }
-keep class android.bluetooth.** { *; }
-keep class android.nfc.** { *; }

# Keep our data & network models (used with kotlinx.serialization & Firestore reflection)
-keep class com.thazh.share.data.** { *; }
-keepclassmembers class com.thazh.share.data.** { *; }
-keep class com.thazh.share.network.** { *; }
-keepclassmembers class com.thazh.share.network.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
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

# Firebase Firestore & Auth & Google Credential Manager
-keep class com.google.firebase.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class androidx.credentials.** { *; }

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
-dontwarn com.google.errorprone.annotations.**
