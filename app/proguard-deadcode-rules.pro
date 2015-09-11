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
-dontwarn com.google.protobuf.**
-dontwarn is.hello.commonsense.bluetooth.transmission.protobuf.**

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
-keep class is.hello.commonsense.bluetooth.transmission.protobuf.** { *; }
-keep class com.hello.ble.protobuf.** { *; }

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

-keep class org.joda.** { *; }
-dontwarn org.joda.**

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

-keepnames class * implements java.io.Serializable

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
