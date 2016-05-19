# suripu-android

[![Circle
CI](https://circleci.com/gh/hello/suripu-android.svg?style=svg&circle-token=6f1d36ce3c3ebe9c0127da90f189ee7dbc6f6861)](https://circleci.com/gh/hello/suripu-android)

It's Sense for Android. Don't see what you're looking for here? Check the [Wiki](https://github.com/hello/suripu-android/wiki).

# Building

## External dependencies

- [Java](http://support.apple.com/kb/DL1572) (on 10.10 and later).
- [Android Studio](http://developer.android.com/sdk/index.html).
- The [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) (for retrolambda).
- The key stores `Hello-Android-Internal.keystore` and `Hello-Android-Release.keystore`. You can get these from a mobile team member.
- An S3 key pair to access the internal maven repository. You can get this from Tim.

## Before your first build

- Place the `*.keystore` files into the same directory as your local copy of the `suripu-android` repository.
- If you're building on a platform other than OS X, you will need to define `JAVA_HOME` in your environment.
- Add your S3 key pair to your `$HOME/.gradle/gradle.properties` file using the property keys `helloAwsAccessKeyID` and `helloAwsSecretKey`.

# Components

The Android Sense app is currently broken into several projects:

- [`Būrūberi`](https://github.com/hello/android-buruberi): A wrapper around the Android Bluetooth APIs that provides a simpler interface, and tries to hide some of the deficiencies in the platform. _Open Source._
- [`AnimeAndroidGo99`](https://github.com/hello/anime-android-go-99): All of the tooling around animations used by the app. Documentation available on the project page. _Open source._
- [`CommonSense`](https://github.com/hello/android-commonsense): The code used to communicate with Sense devices over Bluetooth Low Energy. Builds on top of the `būrūberi` project. _Proprietary._
- `suripu-android`: Everything else.

# Contributing

## Branching

The project follows [gitflow](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow) for the most part. The `master` branch contains the latest stable snapshot of the project, and releases are tagged.

When creating releases, you should merge `develop` into `master`, and then create a new tag from the contents of master. Tags should follow the form `<major>.<minor>.<bugfix>rc<number>`. The rc number should be incremented as candidate builds for public release are created internally.

Bug fixes on stable code should be made by creating a new branch from the target tag, and then submitting a pull request into master. Once the code has been reviewed and tested, a build should be deployed from the branch, a tag should be created from the branch, and the branch should be merged into master and deleted.

## Style

- The project has a code formatter configuration checked into git. Ideally, all new code should try to match the style of existing code in the project as closely as possible. You can run the code formatter to help with this.
- The project currently does not follow Google style for member fields. All fields in the project are not prefixed with `m`. E.g. `name`, not `mName`.
- All new code should use the `@NonNull` and `@Nullable` annotations for method parameters and return values. Any fields in an object that can be `null` should be annotated with `@Nullable`. Non-null fields generally do not need to be annotated.
- When possible, prefer `public final` fields over a `private` field and getter pair.
- Single method interfaces should be implemented using lambdas unless a reference to the implementation of the interface (`this`) is required.

## Patterns

The project is written using non-strict model-view-presenter patterns. All data flow happens through [RxJava](https://github.com/ReactiveX/RxJava) [`Observable`](https://www.google.com/webhp?sourceid=chrome-instant&ion=1&espv=2&ie=UTF-8#q=observable%20rxjava%20javadoc) objects. Presenter state is typically held inside of a `PresenterSubject` object.

`PresenterSubject` behaves differently from typical `Observable` objects. It will never complete, and does not terminate when errors occur. If you use the tools for observables provided by the application, this detail generally does not matter.

The most common type of presenter, a single value presenter, can be created quickly by subclassing the `ValuePresenter` class. This will give you updating, low memory management, and state serialization for free.

The project uses dependency injection through [Dagger 1](http://square.github.io/dagger/) to increase testable surface, and ease singleton creep. Convenience classes are provided that will perform dependency injection for you transparently. See `InjectionActivity`, `InjectionFragment`, `InjectionDialogFragment`, and `InjectionTestCase`. When using one of these classes, you only need to add your subclass to the appropriate module for `@Inject` fields to be satisfied.

All fragments should extend `SenseFragment`, and all dialog fragments should extend `SenseDialogFragment`. By doing this, you gain convenience facilities, and enable easier migration to support library fragments in the future if we decide to support older versions of Android.

Presenters and their views are loosely coupled through dependency injection. The general composition pattern is to use retained fragments for all major UI components, and to bind to the presenter's subjects in `onViewCreated`.

# Testing

All of the components are tested via Robolectric 3.0, targeting SDK level 21. Continuous integration is run on circleCI for each component. A base test case class and domain appropriate testing utility classes are available are provided in each project.

If you have a branch that should not be run on continuous integration before merging, prefix your branch with `no-test-`.

## Coverage

- `Būrūberi`: Near complete coverage.
- `AnimeAndroidGo99`: Near complete coverage.
- `CommonSense`: Near complete coverage.
- `suripu-android`: Has coverage for most core parts of the application.
