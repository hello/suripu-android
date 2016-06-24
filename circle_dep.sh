#!/bin/bash


# Fix the CircleCI path
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools:$PATH"

BTOOLS=/usr/local/android-sdk-linux/build-tools/24.0.0
if [ ! -e BTOOLS]
  echo y | android update sdk -u -a -t android-24 &&
  echo y | android update sdk -u -a -t platform-tools &&
  echo y | android update sdk -u -a -t build-tools-24.0.0 &&
  echo y | android update sdk -u -a -t extra-android-m2repository &&
  echo y | android update sdk -u -a -t extra-google-m2repository &&
fi