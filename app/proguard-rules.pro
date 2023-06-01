# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keep @interface kotlin.coroutines.jvm.internal.DebugMetadata { *; }

-keep class **$$TypeAdapter { *; }
-keep class knf.kuma.pojos.AnimeObject.WebInfo.AnimeChapter {*;}
-keep class knf.kuma.pojos.AnimeObject.WebInfo.AnimeRelated {*;}
-keep class androidx.startup.InitializationProvider {*;}

-keepclasseswithmembernames class * {
    @com.tickaroo.tikxml.* <fields>;
}

-keepclasseswithmembernames class * {
    @com.tickaroo.tikxml.* <methods>;
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclassmembers class * implements pl.droidsonroids.jspoon.ElementConverter
-keep class pl.droidsonroids.jspoon.** { *; }
-keepattributes Signature
-keepattributes Exceptions

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

-dontwarn knf.kuma.**
-dontwarn kotlinx.coroutines.**
-keep public enum knf.kuma.**{*;}
-keep class es.munix.multidisplaycast.**{*;}
-keep class com.connectsdk.**{* ;}
-keepclassmembers public class * implements pl.droidsonroids.jspoon.ElementConverter {
   public <init>(...);
}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keep public class * extends java.lang.Exception
-keep class org.jsoup.**{*;}
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type
-keeppackagenames org.jsoup.nodes
-dontwarn com.amazon.client.metrics.**
-dontwarn com.beloo.widget.chipslayoutmanager.**
-dontwarn com.squareup.okhttp.**
-dontwarn dagger.android.**
-dontwarn dagger.android.support.**
-dontwarn okhttp3.**
-dontwarn okhttp3.internal.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn com.dropbox.core.**
-dontwarn io.branch.**
-dontwarn com.bumptech.glide.**
-dontwarn com.smaato.soma.SomaUnityPlugin*
-dontwarn com.millennialmedia**
-dontwarn com.facebook.**
-dontwarn com.google.common.**
-dontwarn com.google.android.**
-dontwarn com.google.firebase.**
-dontwarn org.jetbrains.anko.db.**
-dontwarn com.afollestad.materialdialogs.**
-dontwarn com.amazon.**
-dontwarn xdroid.toaster.**
-dontwarn com.pavelsikun.seekbarpreference.**
-dontwarn com.jakewharton.picasso.**
-dontwarn io.grpc.**
-dontwarn kotlin.internal.**
-dontwarn at.blogc.android.**
-dontwarn com.afollestad.**
-dontwarn com.connectsdk.**
-dontwarn com.danielstone.materialaboutlibrary.**
-dontwarn com.github.rubensousa.previewseekbar.**
-dontwarn com.mikhaellopez.circularprogressbar.**
-dontwarn com.simplecityapps.**
-dontwarn com.tbuonomo.viewpagerdotsindicator.**
-dontwarn es.munix.multidisplaycast.**
-dontwarn fr.bmartel.speedtest.**
-dontwarn me.zhanghai.android.**
-dontwarn moe.feng.common.**
-dontwarn com.github.stephenvinouze.materialnumberpickercore.**
-dontwarn org.cryse.widget.persistentsearch.**
-dontwarn pl.droidsonroids.jspoon.**
-dontwarn org.jetbrains.anko.**
-dontwarn nl.dionsegijn.konfetti.**
-dontwarn kotlinx.android.**
-dontwarn kotlin.**
-dontwarn io.opencensus.**
-dontwarn dagger.**
-dontwarn com.tonyodev.**