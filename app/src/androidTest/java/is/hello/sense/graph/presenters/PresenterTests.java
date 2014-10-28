package is.hello.sense.graph.presenters;

import android.content.ComponentCallbacks2;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import junit.framework.TestCase;

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


    static class MockPresenter extends Presenter {
        public boolean onContainerDestroyedCalled = false;
        public boolean onContainerResumedCalled = false;
        public boolean onTrimMemoryCalled = false;
        public boolean onReloadForgottenDataCalled = false;
        public boolean onForgetDataForLowMemoryCalled = false;
        public boolean onRestoreStateCalled = false;
        public boolean onSaveStateCalled = false;

        public void reset() {
            this.onContainerDestroyedCalled = false;
            this.onContainerResumedCalled = false;
            this.onTrimMemoryCalled = false;
            this.onReloadForgottenDataCalled = false;
            this.onForgetDataForLowMemoryCalled = false;
            this.onRestoreStateCalled = false;
            this.onSaveStateCalled = false;
        }

        @Override
        public void onContainerDestroyed() {
            super.onContainerDestroyed();
            onContainerDestroyedCalled = true;
        }

        @Override
        public void onContainerResumed() {
            super.onContainerResumed();
            onContainerResumedCalled = true;
        }

        @Override
        public void onTrimMemory(int level) {
            super.onTrimMemory(level);
            onTrimMemoryCalled = true;
        }

        @Override
        protected void onReloadForgottenData() {
            super.onReloadForgottenData();
            onReloadForgottenDataCalled = true;
        }

        @Override
        protected boolean onForgetDataForLowMemory() {
            onForgetDataForLowMemoryCalled = true;
            return true;
        }

        @Override
        public void onRestoreState(@NonNull Parcelable savedState) {
            super.onRestoreState(savedState);
            onRestoreStateCalled = true;
        }

        @Nullable
        @Override
        public Parcelable onSaveState() {
            onSaveStateCalled = true;
            return super.onSaveState();
        }
    }
}
