# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

##──────────────────────────────────────────────────────────────────────────────
## 1. PoToken / WebView JS Interface
##──────────────────────────────────────────────────────────────────────────────
-keepclassmembers class com.music.vivi.utils.potoken.PoTokenWebView {
    @android.webkit.JavascriptInterface public *;
}
-keep class com.music.vivi.utils.potoken.** { *; }

# Keep coroutine continuation for WebView callbacks
-keepclassmembers class * {
    void resume(...);
    void resumeWithException(...);
}


##──────────────────────────────────────────────────────────────────────────────
## 3. Kotlin Serialization
##──────────────────────────────────────────────────────────────────────────────
# Keep `Companion` object fields of serializable classes.
-if @kotlinx.serialization.Serializable class **
-keepclasseswithmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclasseswithmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclasseswithmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepattributes *Annotation*

##──────────────────────────────────────────────────────────────────────────────
## 4. Kotlin Reflection
##──────────────────────────────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**

##──────────────────────────────────────────────────────────────────────────────
## 5. OkHttp / SSL / Logging
##──────────────────────────────────────────────────────────────────────────────
-dontwarn javax.servlet.ServletContainerInitializer
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
-dontwarn org.slf4j.impl.StaticLoggerBinder

##──────────────────────────────────────────────────────────────────────────────
## 6. NewPipe Extractor
##──────────────────────────────────────────────────────────────────────────────
-keep class org.schabi.newpipe.extractor.services.youtube.protos.** { *; }
-keep class org.schabi.newpipe.extractor.timeago.patterns.** { *; }

##──────────────────────────────────────────────────────────────────────────────
## 7. Logging — strip verbose logs in release
##──────────────────────────────────────────────────────────────────────────────
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

##──────────────────────────────────────────────────────────────────────────────
## 8. Java Beans (auto-generated dontwarn)
##──────────────────────────────────────────────────────────────────────────────
-dontwarn java.beans.BeanDescriptor
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor

##──────────────────────────────────────────────────────────────────────────────
## 9. Kuromoji (Japanese tokenizer)
##──────────────────────────────────────────────────────────────────────────────
-keep class com.atilika.kuromoji.** { *; }

##──────────────────────────────────────────────────────────────────────────────
## 10. Queue / Playback Persistence
##──────────────────────────────────────────────────────────────────────────────
-keep class com.music.vivi.models.PersistQueue { *; }
-keep class com.music.vivi.models.PersistPlayerState { *; }
-keep class com.music.vivi.models.QueueData { *; }
-keep class com.music.vivi.models.QueueType { *; }
-keep class com.music.vivi.playback.queues.** { *; }
-keepclassmembers class * implements java.io.Serializable {
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
}

##──────────────────────────────────────────────────────────────────────────────
## 11. UCrop
##──────────────────────────────────────────────────────────────────────────────
-dontwarn com.yalantis.ucrop**
-keep class com.yalantis.ucrop** { *; }
-keep interface com.yalantis.ucrop** { *; }

##──────────────────────────────────────────────────────────────────────────────
## 12. Google Cast
##──────────────────────────────────────────────────────────────────────────────
-keep class com.music.vivi.cast.** { *; }
-keep class com.google.android.gms.cast.** { *; }
-keep class androidx.mediarouter.** { *; }
-dontwarn com.google.re2j.**

##──────────────────────────────────────────────────────────────────────────────
## 13. Vibra Fingerprint (native JNI)
##──────────────────────────────────────────────────────────────────────────────
-keep class com.music.vivi.recognition.VibraSignature { *; }
-keepclassmembers class com.music.vivi.recognition.VibraSignature {
    native <methods>;
}

##──────────────────────────────────────────────────────────────────────────────
## 14. Ktor
##──────────────────────────────────────────────────────────────────────────────
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }
-dontwarn io.ktor.**

##──────────────────────────────────────────────────────────────────────────────
## 15. Shazam Models
##──────────────────────────────────────────────────────────────────────────────
-keep class com.music.shazamkit.models.** { *; }
-keepclassmembers class com.music.shazamkit.models.** { *; }
-keepclassmembers class com.music.shazamkit.models.** {
    *** Companion;
}
-keepclasseswithmembers class com.music.shazamkit.models.** {
    kotlinx.serialization.KSerializer serializer(...);
}

##──────────────────────────────────────────────────────────────────────────────
## 16. Listen Together
##──────────────────────────────────────────────────────────────────────────────
-keep class com.music.vivi.listentogether.** { *; }
-keepclassmembers class com.music.vivi.listentogether.** { *; }
-keepclassmembers class com.music.vivi.listentogether.** {
    *** Companion;
}
-keepclasseswithmembers class com.music.vivi.listentogether.** {
    kotlinx.serialization.KSerializer serializer(...);
}



##──────────────────────────────────────────────────────────────────────────────
## 18. Room Entities & DAOs
##──────────────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Embedded class * { *; }
-dontwarn androidx.room.**

##──────────────────────────────────────────────────────────────────────────────
## 19. InnerTube module (music API models)
##──────────────────────────────────────────────────────────────────────────────
-keep class com.music.innertube.** { *; }
-keepclassmembers class com.music.innertube.** { *; }

##──────────────────────────────────────────────────────────────────────────────
## 20. InnerTubeX Network Engine
##──────────────────────────────────────────────────────────────────────────────
-keep class com.metrolist.innertubex.** { *; }
-keepclassmembers class com.metrolist.innertubex.** { *; }
-dontwarn com.metrolist.innertubex.**

##──────────────────────────────────────────────────────────────────────────────
## 21. Rhino / Java Scripting API
##──────────────────────────────────────────────────────────────────────────────
-dontwarn javax.script.**
