package is.hello.sense.graph;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.junit.runner.RunWith;
import org.junit.runners.model.InitializationError;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.manifest.AndroidManifest;
import org.robolectric.res.FileFsFile;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

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
     * 1) Temporary fix for issue https://github.com/robolectric/robolectric/issues/1460
     * with fragment transactions starting during the onViewCreated method
     * <p>
     * 2) the injected dependencies in the fragment are not included in main Application objectGraph.
     * <p>
     * Warning - this will break if the fragment is already attached to activity
     */
    public static void startNestedVisibleFragment(@NonNull final Fragment fragment,
                                                  @NonNull final Class<? extends Activity> activityClazz,
                                                  @IdRes final int containerResId) {
        Robolectric.getForegroundThreadScheduler().pause();
        startVisibleFragment(fragment, activityClazz, containerResId);
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


    /**
     * Uses reflection to update a final field value so it can be spied on.
     *
     * @param classWithField class containing the field.
     * @param field          field to remove final from.
     * @param newValue       value to change field with.
     * @throws Exception
     */
    protected void changeFinalFieldValue(@NonNull final Object classWithField,
                                         @NonNull final Field field,
                                         @Nullable final Object newValue) throws Exception {
        field.setAccessible(true);
        final Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(classWithField, newValue);
    }

}
