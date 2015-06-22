package is.hello.sense.graph.presenters;

import android.content.ComponentCallbacks2;

import org.junit.Before;
import org.junit.Test;

import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.util.MockPresenter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PresenterTests extends SenseTestCase {
    private MockPresenter presenter = new MockPresenter();

    @Before
    public void initialize() {
        presenter.reset();
    }


    @Test
    public void forgetData() {
        presenter.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN);
        assertFalse(presenter.onForgetDataForLowMemoryCalled);

        presenter.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND);
        assertTrue(presenter.onForgetDataForLowMemoryCalled);

        presenter.onContainerResumed();
        assertTrue(presenter.onReloadForgottenDataCalled);

        presenter.onReloadForgottenDataCalled = false;
        presenter.onContainerResumed();
        assertFalse(presenter.onReloadForgottenDataCalled);
    }
}
