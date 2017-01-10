package is.hello.sense.flows.home.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.flows.home.ui.adapters.TrendsAdapter;
import is.hello.sense.mvp.view.PresenterView;

@SuppressLint("ViewConstructor")
public final class TrendsView extends PresenterView {
    private final RecyclerView recyclerView;
    private final TrendsAdapter trendsAdapter;

    public TrendsView(@NonNull final Activity activity,
                      @NonNull final TrendsAdapter trendsAdapter) {
        super(activity);
        this.trendsAdapter = trendsAdapter;
        this.recyclerView = (RecyclerView) findViewById(R.id.view_trends_recycler);
        setUpStandardRecyclerViewDecorations(this.recyclerView, new LinearLayoutManager(activity));
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

    public final void updateTrends(@NonNull final Trends trends) {
        trendsAdapter.setTrends(trends);
    }


    public final void showError() {
        trendsAdapter.showError();
    }

    public void scrollUp() {
        this.recyclerView.smoothScrollToPosition(0);
    }


}
