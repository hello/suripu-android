package is.hello.sense.graph;

import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;

import org.junit.runner.RunWith;
import org.junit.runners.model.InitializationError;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.manifest.AndroidManifest;
import org.robolectric.res.FileFsFile;

import is.hello.sense.BuildConfig;
import is.hello.sense.SenseApplication;

import static org.robolectric.util.FragmentTestUtil.startVisibleFragment;

@RunWith(SenseTestCase.WorkaroundTestRunner.class)
@Config(constants = BuildConfig.class,
        application = SenseApplication.class,
        sdk = 21)
public abstract class SenseTestCase {
    protected Context getContext() {
        return RuntimeEnvironment.application.getApplicationContext();
    }

    protected Resources getResources() {
        return getContext().getResources();
    }

    public String getString(int id) throws Resources.NotFoundException {
        return getResources().getString(id);
    }

    /**
     * This is an expensive operation...avoid if possible.
     * Temporary fix for issue https://github.com/robolectric/robolectric/issues/1460
     * with fragment transactions starting during the onViewCreated method
     */
    public static void startNestedVisibleFragment(@NonNull final Fragment fragment){
        Robolectric.getForegroundThreadScheduler().pause();
        startVisibleFragment(fragment);
        Robolectric.getForegroundThreadScheduler().advanceToLastPostedRunnable();
    }

    public static class WorkaroundTestRunner extends RobolectricGradleTestRunner {
        public WorkaroundTestRunner(Class<?> klass) throws InitializationError {
            super(klass);
        }

        @Override
        protected AndroidManifest getAppManifest(Config config) {
            AndroidManifest appManifest = super.getAppManifest(config);

            // Currently not automatic
            FileFsFile assets = FileFsFile.from("src", "test", "assets");
            return new AndroidManifest(
                    appManifest.getAndroidManifestFile(),
                    appManifest.getResDirectory(),
                    assets,
                    "is.hello.sense"
            );
        }
    }
}
