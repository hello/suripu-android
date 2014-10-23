# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Applications/Android Studio.app/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-dontskipnonpubliclibraryclasses
-dontwarn dagger.internal.**
-dontwarn is.hello.sense.**
-dontwarn retrofit.**
-dontwarn rx.**

-keepclassmembers class * {
    static final % *;
    static final java.lang.String *;
}

-keep class com.google.gson.** { *; }
-keep class com.google.inject.** { *; }
-keep class org.apache.http.** { *; }
-keep class org.apache.james.mime4j.** { *; }
-keep class javax.inject.** { *; }
-keep class retrofit.** { *; }

-keepattributes *Annotation*,EnclosingMethod

-keepclassmembers,allowobfuscation class * {
    @javax.inject.* *;
    @dagger.* *;
    <init>();
}

-keep class **$$ModuleAdapter
-keep class **$$InjectAdapter
-keep class **$$StaticInjection
-keep class **$$Lambda$* { *; }

-keepnames !abstract class coffee.*

-keepnames class dagger.Lazy

-keep class org.joda.time.tz.** { *; }
-dontwarn org.joda.time.tz.**

-keepnames class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.databind.**
-keep class * {
    @org.codehaus.jackson.annotate.* *;
}
-keep class is.hello.sense.api.model.* {
    *;
}
-keep class * {
    @retrofit.http.* *;
}

