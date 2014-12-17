package is.hello.sense.util;

import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.graph.presenters.Presenter;

public class MockPresenter extends Presenter {
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
