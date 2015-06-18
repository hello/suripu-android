package is.hello.sense.graph.presenters;

import android.os.Bundle;
import android.support.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;

import is.hello.sense.util.MockPresenter;
import rx.functions.Action1;

import static org.junit.Assert.assertTrue;

public class PresenterContainerTests {
    private static final int NUMBER_PRESENTERS = 3;
    private final PresenterContainer presenterContainer = new PresenterContainer();

    @Before
    public void initialize() {
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


    @Test
    public void onContainerDestroyed() {
        presenterContainer.onContainerDestroyed();
        assertAll(mock -> assertTrue(mock.onContainerDestroyedCalled));
    }

    @Test
    public void onTrimMemory() {
        presenterContainer.onTrimMemory(Presenter.BASE_TRIM_LEVEL);
        assertAll(mock -> assertTrue(mock.onTrimMemoryCalled));
    }

    @Test
    public void onContainerResumed() {
        presenterContainer.onContainerResumed();
        assertAll(mock -> assertTrue(mock.onContainerResumedCalled));
    }

    @Test
    public void onSaveState() {
        presenterContainer.onSaveState(new Bundle());
        assertAll(mock -> assertTrue(mock.onSaveStateCalled));
    }

    @Test
    public void onRestoreState() {
        presenterContainer.onRestoreState(new Bundle());
        assertAll(mock -> assertTrue(mock.onRestoreStateCalled));
    }
}
