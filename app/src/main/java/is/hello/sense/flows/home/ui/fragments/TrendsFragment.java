package is.hello.sense.flows.home.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;


import org.joda.time.LocalDate;

import javax.inject.Inject;

import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.api.model.v2.Trends.TimeScale;
import is.hello.sense.flows.home.ui.views.TrendsView;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.TrendsInteractor;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.mvp.util.ViewPagerPresenterChild;
import is.hello.sense.mvp.util.ViewPagerPresenterChildDelegate;
import is.hello.sense.ui.widget.graphing.trends.TrendFeedViewItem;
import is.hello.sense.ui.widget.graphing.trends.TrendGraphView;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;

public abstract class TrendsFragment extends PresenterFragment<TrendsView>
        implements
        TrendFeedViewItem.OnRetry,
        TrendGraphView.AnimationCallback,
        ViewPagerPresenterChild {

    @Inject
    TrendsInteractor trendsInteractor;
    @Inject
    PreferencesInteractor preferencesInteractor;
    private final ViewPagerPresenterChildDelegate presenterChildDelegate = new ViewPagerPresenterChildDelegate(this);

    //region PresenterFragment
    @Override
    public final void initializePresenterView() {
        if (presenterView == null) {
            presenterView = new TrendsView(getActivity(), getAnimatorContext());
            presenterChildDelegate.onViewInitialized();
        }
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
        showLoadingState();
        bindAndSubscribe(trendsInteractor.trends, this::bindTrends, this::presentError);
    }


    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        presenterChildDelegate.setUserVisibleHint(isVisibleToUser);
    }

    @Override
    public void onResume() {
        super.onResume();
        presenterChildDelegate.onResume();
        fetchTrends();
    }

    @Override
    public void onPause() {
        super.onPause();
        presenterChildDelegate.onPause();
    }

    //endregion
    //region ViewPagerPresenterChild
    @Override
    public void onUserVisible() {
    }

    @Override
    public void onUserInvisible() {

    }
    //endregion

    //region onRetry
    @Override
    public final void fetchTrends() {
        trendsInteractor.setTimeScale(getTimeScale());
        trendsInteractor.update();
    }
    //endregion

    /* todo support analytics again
    @Override
    public final void onSelectionChanged(final int newSelectionIndex) {

        final String eventProperty = newTimeScale == TimeScale.LAST_3_MONTHS ? Analytics.Backside.EVENT_TIMESCALE_QUARTER :
                (newTimeScale == TimeScale.LAST_MONTH ? Analytics.Backside.EVENT_TIMESCALE_MONTH : Analytics.Backside.EVENT_TIMESCALE_WEEK);
        final Properties properties = new Properties();
        properties.put(Analytics.Backside.EVENT_TIMESCALE, eventProperty);
        Analytics.trackEvent(Analytics.Backside.EVENT_CHANGE_TRENDS_TIMESCALE, properties);

    }*/
    //region AnimationCallback
    @Override
    public final void isFinished() {
    }
    //endregion


    //region methods
    public void showLoadingState() {
        if (!presenterView.hasTrends()) {
            final LocalDate creationDate = preferencesInteractor.getAccountCreationDate();
            final boolean showWelcomeBack = !DateFormatter.isInLast2Weeks(creationDate) && !DateFormatter.isTodayForTimeline(creationDate);
            presenterView.showWelcomeCard(showWelcomeBack);
        }
    }

    protected abstract TimeScale getTimeScale();

    public final void bindTrends(@NonNull final Trends trends) {
        presenterView.updateTrends(trends);
        isFinished();
    }

    public final void presentError(final Throwable e) {
        presenterView.showError(this);
    }
    //endregion
}
