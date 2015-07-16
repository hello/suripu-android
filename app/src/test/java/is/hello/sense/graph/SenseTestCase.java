package is.hello.sense.graph;

import android.content.Context;
import android.content.res.Resources;

import org.junit.runner.RunWith;
import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.manifest.AndroidManifest;
import org.robolectric.res.FileFsFile;

import is.hello.sense.BuildConfig;
import is.hello.sense.SenseApplication;

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
