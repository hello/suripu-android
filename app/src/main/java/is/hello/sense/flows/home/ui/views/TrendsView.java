package is.hello.sense.flows.home.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.flows.home.ui.adapters.TrendsAdapter;
import is.hello.sense.flows.home.ui.fragments.TrendsFragment;
import is.hello.sense.mvp.view.PresenterView;

@SuppressLint("ViewConstructor")
public class TrendsView extends PresenterView {
    private final RecyclerView recyclerView;
    private final TrendsAdapter trendsAdapter;

    public TrendsView(@NonNull final Activity activity,
                      @NonNull final TrendsAdapter trendsAdapter) {
        super(activity);
        this.trendsAdapter = trendsAdapter;
        this.recyclerView = (RecyclerView) findViewById(R.id.view_trends_recycler);
        setUpStandardRecyclerViewDecorations(this.recyclerView, new LinearLayoutManager(activity));
        this.recyclerView.setHasFixedSize(false); // important for refreshRecyclerView() to work.
        this.recyclerView.setAdapter(trendsAdapter);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.view_trends;
    }

    @Override
    public void releaseViews() {
        this.recyclerView.setAdapter(null);
    }

    public void updateTrends(@NonNull final Trends trends) {
        this.trendsAdapter.setTrends(trends);
    }

    public void showError() {
        this.trendsAdapter.showError();
    }

    public void scrollUp() {
        this.recyclerView.smoothScrollToPosition(0);
    }

    /**
     * This is specifically for {@link is.hello.sense.ui.widget.graphing.trends.GridTrendGraphView}.
     * Unlike the other 2 graphs which have static heights, its height depends on the width of the
     * screen. It has to wait for a GlobalLayout before it can calculate its height.
     * <p>
     * In the past we used a LinearLayout which we were able to call {@link View#requestLayout()}
     * from the TrendGraphView itself after the GlobalLayout occurred.
     * Now that a RecyclerView is drawing it, the call isn't successfully re-rendering the view.
     * <p>
     * We can call this method when the graph has finished animating to redraw it from
     * {@link TrendsFragment#isFinished()}.
     */
    public void refreshRecyclerView() {
        this.recyclerView.requestLayout();
    }

}
