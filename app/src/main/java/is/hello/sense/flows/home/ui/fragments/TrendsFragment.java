package is.hello.sense.flows.home.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ToggleButton;

import com.segment.analytics.Properties;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.api.model.v2.Trends.TimeScale;
import is.hello.sense.flows.home.ui.views.TrendsView;
import is.hello.sense.interactors.ScopedValueInteractor.BindResult;
import is.hello.sense.interactors.TrendsInteractor;
import is.hello.sense.mvp.presenters.SubPresenterFragment;
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.ui.widget.graphing.trends.TrendFeedViewItem;
import is.hello.sense.ui.widget.graphing.trends.TrendGraphView;
import is.hello.sense.util.Analytics;

//todo this class and its view need to be rethought.
public class TrendsFragment extends SubPresenterFragment<TrendsView> implements
        TrendFeedViewItem.OnRetry,
        TrendGraphView.AnimationCallback {

    @Inject
    TrendsInteractor trendsInteractor;


    @Override
    public final void initializePresenterView() {
        if (presenterView == null) {
            presenterView = new TrendsView(getActivity(), getAnimatorContext());
        }
    }


    @Override
    public void onUserVisible() {
        fetchTrends();
    }

    @Override
    public void onUserInvisible() {

    }

    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Analytics.trackEvent(Analytics.Backside.EVENT_TRENDS, null);
        addInteractor(trendsInteractor);
        setHasOptionsMenu(true);
    }

    @Override
    public final void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenterView.setTrendFeedViewAnimationCallback(this);
        bindAndSubscribe(trendsInteractor.trends, this::bindTrends, this::presentError);
    }

    public final void bindTrends(@NonNull final Trends trends) {
        presenterView.updateTrends(trends);
        isFinished();
    }

    public final void presentError(final Throwable e) {
        presenterView.showError(this);

    }

    @Override
    public final void fetchTrends() {
        trendsInteractor.setTimeScale(getTimeScale());
        trendsInteractor.update();
    }
    /* todo support analytics again
    @Override
    public final void onSelectionChanged(final int newSelectionIndex) {

        final String eventProperty = newTimeScale == TimeScale.LAST_3_MONTHS ? Analytics.Backside.EVENT_TIMESCALE_QUARTER :
                (newTimeScale == TimeScale.LAST_MONTH ? Analytics.Backside.EVENT_TIMESCALE_MONTH : Analytics.Backside.EVENT_TIMESCALE_WEEK);
        final Properties properties = new Properties();
        properties.put(Analytics.Backside.EVENT_TIMESCALE, eventProperty);
        Analytics.trackEvent(Analytics.Backside.EVENT_CHANGE_TRENDS_TIMESCALE, properties);

    }*/

    @Override
    public final void isFinished() {
        if (presenterView != null) {
            presenterView.isFinished();
        }
    }

    @NonNull
    protected TimeScale getTimeScale() {
        return TimeScale.LAST_WEEK;
    }

    ;
}
