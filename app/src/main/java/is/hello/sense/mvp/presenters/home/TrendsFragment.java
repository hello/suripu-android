package is.hello.sense.mvp.presenters.home;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ToggleButton;

import com.segment.analytics.Properties;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.api.model.v2.Trends.TimeScale;
import is.hello.sense.interactors.ScopedValueInteractor.BindResult;
import is.hello.sense.interactors.TrendsInteractor;
import is.hello.sense.mvp.view.home.TrendsView;
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.ui.widget.graphing.TrendFeedViewItem;
import is.hello.sense.ui.widget.graphing.TrendGraphView;
import is.hello.sense.util.Analytics;

//todo this class and its view need to be rethought.
public class TrendsFragment extends BacksideTabFragment<TrendsView> implements
        TrendFeedViewItem.OnRetry,
        SelectorView.OnSelectionChangedListener,
        TrendGraphView.AnimationCallback {

    @Inject
    TrendsInteractor trendsInteractor;


    @Override
    public final void initializePresenterView() {
        if (presenterView == null) {
            presenterView= new TrendsView(getActivity(), getAnimatorContext());
        }
    }

    @Override
    public final void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            Analytics.trackEvent(Analytics.Backside.EVENT_TRENDS, null);
        }
    }

    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addInteractor(trendsInteractor);
        setHasOptionsMenu(true);
    }

    @Override
    public final void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenterView.setTimeScaleSelectOnSelectionChangedListener(this);
        presenterView.setSwipeRefreshLayoutRefreshListener(this::fetchTrends);
        presenterView.setTrendFeedViewAnimationCallback(this);
        bindAndSubscribe(trendsInteractor.trends, this::bindTrends, this::presentError);
        presenterView.setTimeScaleButton(trendsInteractor.getTimeScale());
    }

    @Override
    public final void onSwipeInteractionDidFinish() {
    }

    @Override
    public final void onUpdate() {
        if (trendsInteractor.bindScope(getScope()) == BindResult.WAITING_FOR_VALUE) {
            fetchTrends();
        }
    }

    public final void bindTrends(@NonNull final Trends trends) {
        presenterView.updateTrends(trends);
        final List<TimeScale> availableTimeScales = trends.getAvailableTimeScales();
        if (availableTimeScales.size() > 1) {
            if (availableTimeScales.size() != presenterView.getTimeScaleButtonCount()) {
                presenterView.removeAllTimeScaleButtons();
                for (final TimeScale timeScale : availableTimeScales) {
                    final ToggleButton button = presenterView.addTimeScaleButton(timeScale.titleRes, false);
                    if (timeScale == trendsInteractor.getTimeScale()) {
                        presenterView.setSelectedTimeScaleButton(button);
                        fetchTrends();
                    }
                }
                presenterView.setTimeScaleButtonTags(trends.getAvailableTimeScaleTags());
            }
            if (!presenterView.isTimeScaleVisible()) {
                presenterView.transitionInTimeScaleSelector(stateSafeExecutor);
            }
        } else if (presenterView.isTimeScaleVisible()) {
            presenterView.transitionOutTimeScaleSelector();
        } else {
            presenterView.hideTimeScaleSelector();
        }
        isFinished();
    }

    public final void presentError(final Throwable e) {
        presenterView.showError(this);

    }

    @Override
    public final void fetchTrends() {
        presenterView.setRefreshing(true);
        trendsInteractor.update();
    }

    @Override
    public final void onSelectionChanged(final int newSelectionIndex) {
        final Trends.TimeScale newTimeScale = presenterView.setSelectionChanged(newSelectionIndex);
        trendsInteractor.setTimeScale(newTimeScale);

        final String eventProperty = newTimeScale == TimeScale.LAST_3_MONTHS ? Analytics.Backside.EVENT_TIMESCALE_QUARTER :
                (newTimeScale == TimeScale.LAST_MONTH ? Analytics.Backside.EVENT_TIMESCALE_MONTH : Analytics.Backside.EVENT_TIMESCALE_WEEK);
        final Properties properties = new Properties();
        properties.put(Analytics.Backside.EVENT_TIMESCALE, eventProperty);
        Analytics.trackEvent(Analytics.Backside.EVENT_CHANGE_TRENDS_TIMESCALE, properties);

    }

    @Override
    public final void isFinished() {
        presenterView.isFinished();
    }
}
