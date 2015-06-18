package is.hello.sense.graph;

import android.content.Context;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import is.hello.sense.BuildConfig;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public abstract class SenseTestCase {
    protected Context getContext() {
        return RuntimeEnvironment.application;
    }
}
