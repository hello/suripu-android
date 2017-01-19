package is.hello.sense.flows.home.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
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

    @VisibleForTesting
    public final ViewPagerPresenterChildDelegate presenterChildDelegate = new ViewPagerPresenterChildDelegate(this);

    //region PresenterFragment
    @Override
    public void initializePresenterView() {
        if (this.presenterView == null) {
            this.presenterView = new TrendsView(getActivity(), createTrendsAdapter());
            this.presenterChildDelegate.onViewInitialized();
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addInteractor(this.trendsInteractor);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(this.trendsInteractor.trends,
                         this::bindTrends,
                         this::presentError);
    }


    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Analytics.trackEvent(Analytics.Backside.EVENT_TRENDS, null);
        this.presenterChildDelegate.setUserVisibleHint(isVisibleToUser);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.presenterChildDelegate.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.presenterChildDelegate.onPause();
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
    public void fetchTrends() {
        this.trendsInteractor.setTimeScale(getTimeScale());
        this.trendsInteractor.update();
    }
    //endregion

    //region AnimationCallback
    @Override
    public void isFinished() {

    }
    //endregion

    //region scrollUp
    @Override
    public void scrollUp() {
        if (this.presenterView == null) {
            return;
        }
        this.presenterView.scrollUp();
    }
    //endregion

    //region methods

    protected abstract TimeScale getTimeScale();

    @VisibleForTesting
    void bindTrends(@NonNull final Trends trends) {
        this.presenterView.updateTrends(trends);
    }

    @VisibleForTesting
    void presentError(final Throwable e) {
        this.presenterView.showError();
    }

    @VisibleForTesting
    boolean isAccountMoreThan2WeeksOld() {
        final LocalDate creationDate = this.preferencesInteractor.getAccountCreationDate();
        return !DateFormatter.isInLast2Weeks(creationDate) && !DateFormatter.isTodayForTimeline(creationDate);
    }

    @VisibleForTesting
    TrendsAdapter createTrendsAdapter() {
        return new TrendsAdapter(getAnimatorContext(),
                                 this,
                                 this,
                                 isAccountMoreThan2WeeksOld());
    }
    //endregion
}
