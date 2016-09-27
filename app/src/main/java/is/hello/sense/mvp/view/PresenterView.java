package is.hello.sense.mvp.view;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.widget.FrameLayout;

public abstract class PresenterView extends FrameLayout {
    protected final Context context;

    public PresenterView(@NonNull final Activity activity) {
        super(activity);
        activity.getLayoutInflater().inflate(getLayoutRes(), this);
        this.context = activity;
    }

    protected final String getString(@StringRes final int res) {
        return context.getString(res);
    }


    public void viewCreated() {

    }

    public void resume() {

    }

    public void pause() {

    }

    @CallSuper
    public void destroyView() {
        release();
    }


    /**
     * Remove any reference to Fragment using it.
     * Will usually be context and any listeners.
     */
    @CallSuper
    public void detach() {
        release();
    }

    @LayoutRes
    protected abstract int getLayoutRes();

    /**
     * This will be called in both destroyView and detach to ensure nothing survives.
     */
    public abstract void releaseViews();

    private void release() {
        releaseViews();
    }

}
