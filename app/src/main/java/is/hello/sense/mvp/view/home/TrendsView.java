package is.hello.sense.mvp.view.home;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
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
import is.hello.sense.ui.widget.graphing.TrendFeedView;
import is.hello.sense.ui.widget.graphing.TrendFeedViewItem;
import is.hello.sense.ui.widget.graphing.TrendGraphView;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.StateSafeExecutor;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public final class TrendsView extends PresenterView {
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar initialActivityIndicator;
    private TrendFeedView trendFeedView;
    private SelectorView timeScaleSelector;
    private AnimatorContext animatorContext;


    public TrendsView(@NonNull final Activity activity, @NonNull final AnimatorContext animatorContext) {
        super(activity);
        this.animatorContext = animatorContext;
    }

    @NonNull
    @Override
    public final View createView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_trends, container, false);

        this.swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.fragment_trends_refresh_container);
        Styles.applyRefreshLayoutStyle(swipeRefreshLayout);

        this.initialActivityIndicator = (ProgressBar) view.findViewById(R.id.fragment_trends_loading);
        this.trendFeedView = (TrendFeedView) view.findViewById(R.id.fragment_trends_trendgraph);
        this.trendFeedView.setAnimatorContext(animatorContext);
        this.timeScaleSelector = (SelectorView) view.findViewById(R.id.fragment_trends_time_scale);
        timeScaleSelector.setButtonLayoutParams(new SelectorView.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        timeScaleSelector.setVisibility(View.INVISIBLE);
        timeScaleSelector.setBackground(new TabsBackgroundDrawable(context.getResources(),
                                                                   TabsBackgroundDrawable.Style.SUBNAV));
        return view;
    }


    @Override
    public final void destroyView() {
        super.destroyView();
        this.initialActivityIndicator = null;
        swipeRefreshLayout.setOnRefreshListener(null);
        this.swipeRefreshLayout = null;
        this.timeScaleSelector.setOnSelectionChangedListener(null);
        this.timeScaleSelector = null;
        this.trendFeedView = null;
        this.animatorContext = null;
    }

    @Override
    public final void detach() {
        super.detach();
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

    public boolean isTimeScaleVisible() {
        return timeScaleSelector.getVisibility() == View.VISIBLE;
    }

    public void hideTimeScaleSelector() {
        timeScaleSelector.setVisibility(View.GONE);
    }

    public Trends.TimeScale setSelectionChanged(final int newSelectionIndex) {
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

    public void transitionInTimeScaleSelector(@NonNull final StateSafeExecutor executor) {
        timeScaleSelector.setVisibility(View.INVISIBLE);
        Views.runWhenLaidOut(timeScaleSelector, executor.bind(() -> {
            timeScaleSelector.setTranslationY(-timeScaleSelector.getMeasuredHeight());
            timeScaleSelector.setVisibility(View.VISIBLE);
            animatorFor(timeScaleSelector, animatorContext)
                    .translationY(0f)
                    .start();
        }));
    }

    public void transitionOutTimeScaleSelector() {
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
