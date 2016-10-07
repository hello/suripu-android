package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.ToggleButton;

import com.segment.analytics.Properties;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.api.model.v2.Trends.TimeScale;
import is.hello.sense.interactors.ScopedValueInteractor.BindResult;
import is.hello.sense.interactors.TrendsInteractor;
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.ui.widget.TabsBackgroundDrawable;
import is.hello.sense.ui.widget.graphing.trends.TrendFeedView;
import is.hello.sense.ui.widget.graphing.trends.TrendFeedViewItem;
import is.hello.sense.ui.widget.graphing.trends.TrendGraphView;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class TrendsFragment extends BacksideTabFragment implements TrendFeedViewItem.OnRetry, SelectorView.OnSelectionChangedListener, TrendGraphView.AnimationCallback {
    @Inject
    TrendsInteractor trendsPresenter;

    private ProgressBar initialActivityIndicator;
    private SwipeRefreshLayout swipeRefreshLayout;

    private TrendFeedView trendFeedView;
    private SelectorView timeScaleSelector;

    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            Analytics.trackEvent(Analytics.Backside.EVENT_TRENDS, null);
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPresenter(trendsPresenter);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_trends, container, false);

        this.swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.fragment_trends_refresh_container);
        swipeRefreshLayout.setOnRefreshListener(this::fetchTrends);
        Styles.applyRefreshLayoutStyle(swipeRefreshLayout);

        this.initialActivityIndicator = (ProgressBar) view.findViewById(R.id.fragment_trends_loading);
        this.trendFeedView = (TrendFeedView) view.findViewById(R.id.fragment_trends_trendgraph);
        this.trendFeedView.setAnimatorContext(getAnimatorContext());
        this.trendFeedView.setAnimationCallback(this);
        this.timeScaleSelector = (SelectorView) view.findViewById(R.id.fragment_trends_time_scale);
        timeScaleSelector.setButtonLayoutParams(new SelectorView.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        timeScaleSelector.setVisibility(View.INVISIBLE);
        timeScaleSelector.setBackground(new TabsBackgroundDrawable(getResources(),
                                                                   TabsBackgroundDrawable.Style.SUBNAV));
        timeScaleSelector.setOnSelectionChangedListener(this);

        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeRefreshLayout.setRefreshing(true);
        bindAndSubscribe(trendsPresenter.trends, this::bindTrends, this::presentError);

        final ToggleButton buttonToSelect =
                timeScaleSelector.getButtonForTag(trendsPresenter.getTimeScale());
        if (buttonToSelect != null) {
            timeScaleSelector.setSelectedButton(buttonToSelect);
        } else {
            timeScaleSelector.setSelectedIndex(0);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        trendsPresenter.unbindScope();

        this.initialActivityIndicator = null;
        this.swipeRefreshLayout = null;
        this.timeScaleSelector = null;
        this.trendFeedView = null;
    }


    @Override
    public void onSwipeInteractionDidFinish() {
    }

    @Override
    public void onUpdate() {
        if (trendsPresenter.bindScope(getScope()) == BindResult.WAITING_FOR_VALUE) {
            fetchTrends();
        }
    }

    private void transitionInTimeScaleSelector() {
        timeScaleSelector.setVisibility(View.INVISIBLE);
        Views.runWhenLaidOut(timeScaleSelector, stateSafeExecutor.bind(() -> {
            timeScaleSelector.setTranslationY(-timeScaleSelector.getMeasuredHeight());
            timeScaleSelector.setVisibility(View.VISIBLE);
            animatorFor(timeScaleSelector, getAnimatorContext())
                    .translationY(0f)
                    .start();
        }));
    }

    private void transitionOutTimeScaleSelector() {
        animatorFor(timeScaleSelector, getAnimatorContext())
                .translationY(-timeScaleSelector.getMeasuredHeight())
                .addOnAnimationCompleted(finished -> {
                    if (finished) {
                        timeScaleSelector.setVisibility(View.GONE);
                    }
                })
                .start();
    }

    public void bindTrends(@NonNull final Trends trends) {
        trendFeedView.bindTrends(trends);
        swipeRefreshLayout.setRefreshing(false);
        initialActivityIndicator.setVisibility(View.GONE);

        final List<TimeScale> availableTimeScales = trends.getAvailableTimeScales();
        if (availableTimeScales.size() > 1) {
            if (availableTimeScales.size() != timeScaleSelector.getButtonCount()) {
                timeScaleSelector.removeAllButtons();
                for (final TimeScale timeScale : availableTimeScales) {
                    final ToggleButton button = timeScaleSelector.addOption(timeScale.titleRes, false);
                    if (timeScale == trendsPresenter.getTimeScale()) {
                        timeScaleSelector.setSelectedButton(button);
                        fetchTrends();
                    }
                }
                timeScaleSelector.setButtonTags(trends.getAvailableTimeScaleTags());
            }
            if (timeScaleSelector.getVisibility() != View.VISIBLE) {
                transitionInTimeScaleSelector();
            }
        } else if (timeScaleSelector.getVisibility() == View.VISIBLE) {
            transitionOutTimeScaleSelector();
        } else {
            timeScaleSelector.setVisibility(View.GONE);
        }
        isFinished();
    }

    public void presentError(final Throwable e) {
        timeScaleSelector.setVisibility(View.GONE);
        trendFeedView.presentError(this);
        timeScaleSelector.setEnabled(true);
        swipeRefreshLayout.setRefreshing(false);
        initialActivityIndicator.setVisibility(View.GONE);

    }

    @Override
    public void fetchTrends() {
        swipeRefreshLayout.setRefreshing(true);
        trendsPresenter.update();
    }

    @Override
    public void onSelectionChanged(final int newSelectionIndex) {
        timeScaleSelector.clicked(newSelectionIndex);
        trendFeedView.setLoading(true);
        final TimeScale newTimeScale =
                (TimeScale) timeScaleSelector.getButtonTagAt(newSelectionIndex);
        trendsPresenter.setTimeScale(newTimeScale);

        final String eventProperty = newTimeScale == TimeScale.LAST_3_MONTHS ? Analytics.Backside.EVENT_TIMESCALE_QUARTER :
                (newTimeScale == TimeScale.LAST_MONTH ? Analytics.Backside.EVENT_TIMESCALE_MONTH : Analytics.Backside.EVENT_TIMESCALE_WEEK);
        final Properties properties = new Properties();
        properties.put(Analytics.Backside.EVENT_TIMESCALE, eventProperty);
        Analytics.trackEvent(Analytics.Backside.EVENT_CHANGE_TRENDS_TIMESCALE, properties);

    }

    @Override
    public void isFinished() {
        if (trendFeedView != null && timeScaleSelector != null && !trendFeedView.isAnimating()) {
            timeScaleSelector.setEnabled(true);
        }
    }
}
