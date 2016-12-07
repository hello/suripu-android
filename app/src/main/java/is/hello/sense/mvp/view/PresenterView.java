package is.hello.sense.mvp.view;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.support.annotation.CallSuper;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import is.hello.sense.R;
import is.hello.sense.ui.recycler.CardItemDecoration;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;

public abstract class PresenterView extends FrameLayout {
    protected final Context context;

    public PresenterView(@NonNull final Activity activity) {
        super(activity);
        if (useAppCompat()) {
            final Context contextThemeWrapper = new ContextThemeWrapper(activity, R.style.AppTheme_AppCompat);
            final LayoutInflater localInflater = activity.getLayoutInflater().cloneInContext(contextThemeWrapper);
            localInflater.inflate(getLayoutRes(), this);
        } else {
            activity.getLayoutInflater().inflate(getLayoutRes(), this);
        }
        this.context = activity;
    }

    /**
     * To avoid updating the entire application and acitvities themes we can set the theme of individual
     * views using this.
     *
     * @return true will use AppCompat. Should be true for any view that requires
     * {@link android.support.design.widget.TabLayout}.
     */
    protected boolean useAppCompat() {
        return false;
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

    /**
     * Helper function for establishing our standard recycler view decoration.
     *
     * @param recyclerView  view to modify.
     * @param layoutManager layout manager to use.
     */
    protected final void setUpStandardRecyclerViewDecorations(@NonNull final RecyclerView recyclerView,
                                                              @NonNull final LinearLayoutManager layoutManager) {

        final Resources resources = getResources();
        recyclerView.setLayoutManager(layoutManager);
        final CardItemDecoration decoration = new CardItemDecoration(resources);
        decoration.contentInset = new Rect(0, 0, 0, resources.getDimensionPixelSize(R.dimen.x12));
        recyclerView.addItemDecoration(decoration);
        recyclerView.addItemDecoration(new FadingEdgesItemDecoration(layoutManager,
                                                                     resources,
                                                                     FadingEdgesItemDecoration.Style.ROUNDED_EDGES));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);
    }

}
