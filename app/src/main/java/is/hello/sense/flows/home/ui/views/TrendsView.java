package is.hello.sense.flows.home.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.ToggleButton;

import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.ui.widget.TabsBackgroundDrawable;
import is.hello.sense.ui.widget.graphing.trends.TrendFeedView;
import is.hello.sense.ui.widget.graphing.trends.TrendFeedViewItem;
import is.hello.sense.ui.widget.graphing.trends.TrendGraphView;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.StateSafeExecutor;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

@SuppressLint("ViewConstructor")
public final class TrendsView extends PresenterView {
    private final SwipeRefreshLayout swipeRefreshLayout;
    private final ProgressBar initialActivityIndicator;
    private final TrendFeedView trendFeedView;
    private final SelectorView timeScaleSelector;
    private final AnimatorContext animatorContext;


    public TrendsView(@NonNull final Activity activity, @NonNull final AnimatorContext animatorContext) {
        super(activity);
        this.animatorContext = animatorContext;
        this.swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.fragment_trends_refresh_container);
        Styles.applyRefreshLayoutStyle(swipeRefreshLayout);

        this.initialActivityIndicator = (ProgressBar) findViewById(R.id.fragment_trends_loading);
        this.trendFeedView = (TrendFeedView) findViewById(R.id.fragment_trends_trendgraph);
        this.trendFeedView.setAnimatorContext(animatorContext);
        this.timeScaleSelector = (SelectorView) findViewById(R.id.fragment_trends_time_scale);
        timeScaleSelector.setButtonLayoutParams(new SelectorView.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        timeScaleSelector.setVisibility(View.INVISIBLE);
        timeScaleSelector.setBackground(new TabsBackgroundDrawable(context.getResources(),
                                                                   TabsBackgroundDrawable.Style.SUBNAV));
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_trends;
    }


    @Override
    public final void releaseViews() {
        if (swipeRefreshLayout != null) {
            this.swipeRefreshLayout.setOnRefreshListener(null);
        }
        if (timeScaleSelector != null) {
            this.timeScaleSelector.setOnSelectionChangedListener(null);
        }
    }

    public final void setSwipeRefreshLayoutRefreshListener(@NonNull final SwipeRefreshLayout.OnRefreshListener listener) {
        swipeRefreshLayout.setOnRefreshListener(listener);
        swipeRefreshLayout.setRefreshing(true);
    }

    public final void setTrendFeedViewAnimationCallback(@NonNull final TrendGraphView.AnimationCallback callback) {
        this.trendFeedView.setAnimationCallback(callback);
    }

    public final void setTimeScaleSelectOnSelectionChangedListener(@NonNull final SelectorView.OnSelectionChangedListener listener) {
        timeScaleSelector.setOnSelectionChangedListener(listener);
    }

    public final void setTimeScaleButton(@NonNull final Trends.TimeScale timeScale) {
        final ToggleButton buttonToSelect = timeScaleSelector.getButtonForTag(timeScale);
        if (buttonToSelect != null) {
            timeScaleSelector.setSelectedButton(buttonToSelect);
        } else {
            timeScaleSelector.setSelectedIndex(0);
        }
    }

    public final int getTimeScaleButtonCount() {
        return timeScaleSelector.getButtonCount();
    }

    public final void removeAllTimeScaleButtons() {
        timeScaleSelector.removeAllButtons();
    }

    public final ToggleButton addTimeScaleButton(@StringRes final int title, final boolean wantsDivider) {
        return timeScaleSelector.addOption(title, wantsDivider);
    }

    public final void setSelectedTimeScaleButton(@NonNull final ToggleButton button) {
        timeScaleSelector.setSelectedButton(button);
    }

    public void setTimeScaleButtonTags(@NonNull final Object[] tags) {
        timeScaleSelector.setButtonTags(tags);
    }

    public final boolean isTimeScaleVisible() {
        return timeScaleSelector.getVisibility() == View.VISIBLE;
    }

    public final void hideTimeScaleSelector() {
        timeScaleSelector.setVisibility(View.GONE);
    }

    public final Trends.TimeScale setSelectionChanged(final int newSelectionIndex) {
        timeScaleSelector.clicked(newSelectionIndex);
        trendFeedView.setLoading(true);
        return (Trends.TimeScale) timeScaleSelector.getButtonTagAt(newSelectionIndex);
    }

    public final void updateTrends(@NonNull final Trends trends) {
        trendFeedView.bindTrends(trends);
        swipeRefreshLayout.setRefreshing(false);
        initialActivityIndicator.setVisibility(View.GONE);
    }

    public final void setRefreshing(final boolean refreshing) {
        swipeRefreshLayout.setRefreshing(refreshing);
    }

    public final void showError(@NonNull final TrendFeedViewItem.OnRetry onRetry) {
        timeScaleSelector.setVisibility(View.GONE);
        trendFeedView.presentError(onRetry);
        timeScaleSelector.setEnabled(true);
        swipeRefreshLayout.setRefreshing(false);
        initialActivityIndicator.setVisibility(View.GONE);
    }

    public final void isFinished() {
        if (trendFeedView != null && timeScaleSelector != null && !trendFeedView.isAnimating()) {
            timeScaleSelector.setEnabled(true);
        }
    }

    public final void transitionInTimeScaleSelector(@NonNull final StateSafeExecutor executor) {
        timeScaleSelector.setVisibility(View.INVISIBLE);
        Views.runWhenLaidOut(timeScaleSelector, executor.bind(() -> {
            timeScaleSelector.setTranslationY(-timeScaleSelector.getMeasuredHeight());
            timeScaleSelector.setVisibility(View.VISIBLE);
            animatorFor(timeScaleSelector, animatorContext)
                    .translationY(0f)
                    .start();
        }));
    }

    public final void transitionOutTimeScaleSelector() {
        animatorFor(timeScaleSelector, animatorContext)
                .translationY(-timeScaleSelector.getMeasuredHeight())
                .addOnAnimationCompleted(finished -> {
                    if (finished) {
                        timeScaleSelector.setVisibility(View.GONE);
                    }
                })
                .start();
    }
}
