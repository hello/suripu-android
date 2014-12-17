package is.hello.sense.graph.presenters;

import android.content.ComponentCallbacks2;

import junit.framework.TestCase;

import is.hello.sense.util.MockPresenter;

public class PresenterTests extends TestCase {
    private MockPresenter presenter = new MockPresenter();

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        presenter.reset();
    }


    public void testForgetData() {
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
