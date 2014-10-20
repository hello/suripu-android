suripu-android
==============

It's Sense.app, for Android.

Building
========

The app requires the [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) for lambda-support. You will need to clone the Android BLE stack into the same directory as your clone of the suripu-android project, like so:

	git clone git@github.com:hello/suripu-android.git
	git clone git@github.com:hello/ble-android.git

If you intend on creating release builds, you should install the Crashlytics IntelliJ plugin and log in so that your builds and their Proguard info is registered with the service.

All other app dependencies are satisifed through the gradle build system, and Android Studio.