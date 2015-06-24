suripu-android
==============

It's Sense for Android.

Prerequisites
=============

- [Java](http://support.apple.com/kb/DL1572) (on Yosemite).
- [Android Studio](http://developer.android.com/sdk/index.html).
- The [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) (for lambda support).
- The correct SDK and build tools. These will be automatically installed by Android Studio and the Square SDK manager gradle plugin.
- The key stores `Hello-Android-Internal.keystore` and `Hello-Android-Release.keystore`. Acquire these from another team member.
- The buruberi project checked out into the same directory as `suripu-android`.

Building
========

Place the `.keystore` files into the same directory as your local copy of the `suripu-android` repository.

_Warning:_ In order to support continuous integration without external assets, the build process will default to your global user keystore if you don't have `Hello-Android-Internal.keystore` in the correct location. This means you cannot generate builds for internal distribution.

If you're building the app on a platform other than OS X, you will need to define JAVA_HOME in order for the project to find your installation of the JDK 8.

Branching
=========

The project follows [gitflow](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow) for the most part. The `master` branch contains the latest stable snapshot of the project, and releases are tagged.

When creating releases, you should merge `develop` into `master`, and then create a new tag from the contents of master. Tags should follow the form `<major>.<minor>.<bugfix>rc<number>`. The rc number should be incremented as candidate builds for public release are created internally.

Bug fixes on stable code should be made by creating a new branch from the target tag, and then submitting a pull request into master. Once the code has been reviewed and tested, a build should be deployed from the branch, a tag should be created from the branch, and the branch should be merged into master and deleted.

Contributing
============

All code in the Sense for Android project is written in a restricted subset of Java 8 via the retrolambda back-compiler. It is safe to use all forms of Java 8 lambdas without restriction, with consideration for general memory usage by instances. __It is not__ possible to use interface default methods, or any of the JDK 8 classes and methods that have not been explicitly backported into the project. __Note:__ new code should not use anonymous inner classes for inline interface implementations unless a reference to `this` is required.

Code contributed to the project should more or less match the default "Reformat Code…" option in Android Studio. Generally this means opening braces on the same line, using spaces for indentation, and spaces after keywords on control structures. The project explicitly goes against the conventions in this formatter when it comes to @Attributes. Attributes are generally formatted to look like keywords. When in doubt, just run the reformat code option on your contribution.

All new code _without exception_ should use the `@NonNull` and `@Nullable` attributes for method parameters. Any fields in an object that can be `null` should be annotated with `@Nullable`. The project does not apply `@NonNull` in the same way.

Patterns
========

The majority of the project is written in the Model-View-Presenter pattern. All data flow happens through RxJava `Observable` objects, and all presenter state is held in `PresenterSubject` instances. __Important:__ the `PresenterSubject` class explicitly violates several interface guidelines from RxJava. A `PresenterSubject` never completes, and will silently emit new values after an error has occur. Any code that generically operates on `Observable` objects should take `PresenterSubject` into account until it can be replaced by something better. 

The project extensively uses dependency injection through `Dagger` to increase testable surface, and ease singleton creep. Convenience classes are provided that will perform dependency injection for you transparently. See `InjectionActivity`, `InjectionFragment`, `InjectionDialogFragment`, and `InjectionTestCase`. When using one of these classes, you only need to add your subclass to the appropriate module for `@Inject` fields to be satisifed.

Presenters and their views are loosely coupled through dependency injection. The general composition pattern is to use retained fragments for all major UI components, and to bind to the presenter's subjects in `onViewCreated`. The most common type of presenter, a single value presenter, can be created quickly by subclassing the `ValuePresenter` class. This will give you updating, low memory management, and state serialization for free.

Modules
=======

The project is broken into four modules:

- `ApiModule`: responsible for all communication with the backend. Satisfies all model and network related dependencies.
- `BluetoothModule`: responsible for all Bluetooth dependencies.
- `SenseAppModule`: responsible for all presenter dependencies.
- `TestModule`: an incomplete superset of `ApiModule`, `BluetoothModule`, and `SenseAppModule` that stubs out classes for tests.

Testing
=======

The project currently contains unit tests for most parts of the project with major logic. All of the presenters have accompanying synchronous unit tests, and most of the Bluetooth stack's non-radio related functionality is equipped. Any new presenters introduced into the project should have unit tests accompanying them when merged into `master`.

All tests are run within Robolectric on both your local computer, and on circleCI. If you have a branch that should not be run on continuous integration before merging, prefix your branch with `no-test-`.

Deploying Internally
====================

Most non-testing deployment can be done through the included `deploy` ruby script. In order to use this script, you will need to `gem install colorize`, and `export HELLO_DEPLOY_HOCKEY_TOKEN` in your bash profile. Hockey API tokens can be generated [here](https://rink.hockeyapp.net/manage/auth_tokens). You will need to give Upload and Release permissions to the token for the script to work.

The build script is capable of generating four flavors:

- `alpha`: Uses the internal keystore and has unique package id. Includes extended error reporting, a debug interface accessible by shaking your phone, a Bluetooth debugging tool for Sense, and talks to the dev backend instead of production. These builds are generally used to share work in progress features.
- `beta`: Uses the release keystore and release package id. Includes the debug interface, but otherwise is identical to a release build.
- `store`: Uses the release keystore and release package id. No debug interfaces are included. This is the debug build. You cannot generate one of these builds without tagging master during the build process.
- `feature`: Identical to beta, but deploys to `Sense New Features` on Hockey instead of `Sense Beta`.

The following flags are available for the build script:

- `-t / --[no-]tests`: Runs the tests on the project before deploying. Requires a connected device or emulator.
- `-k / --[no-]clean`: Cleans the project before deploying. Recommended if the dependency graph has changed.
- `-sTAG / --save-tag=TAG`: Creates a tag on the current branch. Required for new releases.

The `deploy` script is capable of generating multiple builds sequentially in a single invocation. Just specify the build flavors one after another. The builds are generated in the order specified.

Deploying Externally
====================

External deploying is accomplished through a combination of the `deploy` script, HockeyApp, and the Play Store console. A typical public deployment goes as follows:

- Generate alpha, beta, and store builds using the `deploy` script.
	- Store builds must be generated from the `master` branch.
	- Store builds must be tagged using the scheme `<version>rc<number>`.
	- The standard `deploy` invocation is `./scripts/deploy -k -s1.0.8rc1 alpha beta store`.
- Deploy the builds internally using HockeyApp.
	- Alpha and beta builds should have diff-style release notes.
	- Store builds should have release notes covering all the changes since the last public deploy.
- [Ideally] a 24 hour period should pass without crashes recorded or bugs reported.
- Publish the generated `app-store.apk` file to the Play Store – revealed at the end of the build process and downloadable from HockeyApp if necessary.
