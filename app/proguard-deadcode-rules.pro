-dontoptimize
-dontobfuscate
-dontpreverify
-dontnote

-ignorewarnings
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
