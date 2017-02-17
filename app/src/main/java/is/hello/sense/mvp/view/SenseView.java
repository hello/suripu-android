package is.hello.sense.mvp.view;

import android.annotation.SuppressLint;
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
import android.widget.FrameLayout;


import is.hello.sense.ui.recycler.CardItemDecoration;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;

public abstract class SenseView extends FrameLayout {
    protected final Context context;

    public SenseView(@NonNull final Activity activity) {
        super(activity);
        this.context = activity;
        if (getLayoutRes() == 0){
            return;
        }
        activity.getLayoutInflater().inflate(getLayoutRes(), this);
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
     * Override to change recycler view padding from an edge.
     *
     * @return distance recycler view should be from edge
     */
    public Rect contentInset() {
        return new Rect(0, 0, 0, 0);
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
        decoration.contentInset = contentInset();
        recyclerView.addItemDecoration(decoration);
        recyclerView.addItemDecoration(new FadingEdgesItemDecoration(layoutManager,
                                                                     resources,
                                                                     FadingEdgesItemDecoration.Style.ROUNDED_EDGES));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);
    }


    @SuppressLint("ViewConstructor")
    public static class EmptySenseView extends SenseView {
        public EmptySenseView(@NonNull final Activity activity) {
            super(activity);
        }

        @Override
        protected int getLayoutRes() {
            return 0;
        }

        @Override
        public void releaseViews() {

        }
    }

}
