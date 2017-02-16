package is.hello.sense.flows.home.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.flows.home.ui.adapters.TrendsAdapter;
import is.hello.sense.mvp.view.SenseView;

@SuppressLint("ViewConstructor")
public class TrendsView extends SenseView {
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

}
