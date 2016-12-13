package is.hello.sense.flows.home.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.widget.graphing.trends.TrendFeedView;
import is.hello.sense.ui.widget.graphing.trends.TrendFeedViewItem;
import is.hello.sense.ui.widget.graphing.trends.TrendGraphView;

@SuppressLint("ViewConstructor")
public final class TrendsView extends PresenterView {
    private final TrendFeedView trendFeedView;


    public TrendsView(@NonNull final Activity activity, @NonNull final AnimatorContext animatorContext) {
        super(activity);
        this.trendFeedView = (TrendFeedView) findViewById(R.id.fragment_trends_trendgraph);
        this.trendFeedView.setAnimatorContext(animatorContext);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_trends;
    }

    @Override
    public void releaseViews() {

    }


    public final void setTrendFeedViewAnimationCallback(@NonNull final TrendGraphView.AnimationCallback callback) {
        this.trendFeedView.setAnimationCallback(callback);
    }


    public final void updateTrends(@NonNull final Trends trends) {
        trendFeedView.bindTrends(trends);
    }


    public final void showError(@NonNull final TrendFeedViewItem.OnRetry onRetry) {
        trendFeedView.presentError(onRetry);
    }


    public boolean hasTrends() {
        return trendFeedView.hasTrends();
    }

    public void showWelcomeCard(final boolean showWelcomeBack) {
        this.trendFeedView.showWelcomeCard(showWelcomeBack);
    }


}
