package is.hello.sense.graph.presenters;

import android.os.Bundle;
import android.support.annotation.NonNull;

import junit.framework.TestCase;

import is.hello.sense.util.MockPresenter;
import rx.functions.Action1;

public class PresenterContainerTests extends TestCase {
    private static final int NUMBER_PRESENTERS = 3;
    private final PresenterContainer presenterContainer = new PresenterContainer();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        presenterContainer.getPresenters().clear();

        for (int i = 0; i < NUMBER_PRESENTERS; i++) {
            presenterContainer.addPresenter(new MockPresenter());
        }
    }

    protected void assertAll(@NonNull Action1<MockPresenter> asserter) {
        for (Presenter presenter : presenterContainer.getPresenters()) {
            MockPresenter mock = (MockPresenter) presenter;
            asserter.call(mock);
        }
    }


    public void testOnContainerDestroyed() {
        presenterContainer.onContainerDestroyed();
        assertAll(mock -> assertTrue(mock.onContainerDestroyedCalled));
    }

    public void testOnTrimMemory() {
        presenterContainer.onTrimMemory(Presenter.BASE_TRIM_LEVEL);
        assertAll(mock -> assertTrue(mock.onTrimMemoryCalled));
    }

    public void testOnContainerResumed() {
        presenterContainer.onContainerResumed();
        assertAll(mock -> assertTrue(mock.onContainerResumedCalled));
    }

    public void testOnSaveState() {
        presenterContainer.onSaveState(new Bundle());
        assertAll(mock -> assertTrue(mock.onSaveStateCalled));
    }

    public void testOnRestoreState() {
        presenterContainer.onRestoreState(new Bundle());
        assertAll(mock -> assertTrue(mock.onRestoreStateCalled));
    }
}
