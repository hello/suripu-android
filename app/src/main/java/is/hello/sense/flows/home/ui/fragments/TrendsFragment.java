package is.hello.sense.flows.home.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;


import org.joda.time.LocalDate;

import javax.inject.Inject;

import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.api.model.v2.Trends.TimeScale;
import is.hello.sense.flows.home.ui.activities.HomeActivity;
import is.hello.sense.flows.home.ui.adapters.TrendsAdapter;
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
        ViewPagerPresenterChild,
        HomeActivity.ScrollUp {

    @Inject
    TrendsInteractor trendsInteractor;
    @Inject
    PreferencesInteractor preferencesInteractor;
    private final ViewPagerPresenterChildDelegate presenterChildDelegate = new ViewPagerPresenterChildDelegate(this);

    //region PresenterFragment
    @Override
    public final void initializePresenterView() {
        if (presenterView == null) {
            final LocalDate creationDate = preferencesInteractor.getAccountCreationDate();
            final boolean accountIsMoreThan2WeeksOld = !DateFormatter.isInLast2Weeks(creationDate) && !DateFormatter.isTodayForTimeline(creationDate);
            presenterView = new TrendsView(getActivity(), new TrendsAdapter(getAnimatorContext(), this, this, accountIsMoreThan2WeeksOld));
            presenterChildDelegate.onViewInitialized();
        }
    }

    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addInteractor(trendsInteractor);
    }

    @Override
    public final void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(trendsInteractor.trends, this::bindTrends, this::presentError);
    }


    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Analytics.trackEvent(Analytics.Backside.EVENT_TRENDS, null);
        presenterChildDelegate.setUserVisibleHint(isVisibleToUser);
    }

    @Override
    public void onResume() {
        super.onResume();
        presenterChildDelegate.onResume();
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
        fetchTrends();
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

    //region AnimationCallback
    @Override
    public final void isFinished() {
    }
    //endregion

    @Override
    public void scrollUp() {
        if (presenterView == null) {
            return;
        }
        presenterView.scrollUp();
    }

    //region methods

    protected abstract TimeScale getTimeScale();

    public final void bindTrends(@NonNull final Trends trends) {
        presenterView.updateTrends(trends);
        isFinished();
    }

    public final void presentError(final Throwable e) {
        presenterView.showError();
    }
    //endregion
}
