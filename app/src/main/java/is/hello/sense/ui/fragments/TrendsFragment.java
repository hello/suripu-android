package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import java.util.Random;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.graph.presenters.ScopedValuePresenter.BindResult;
import is.hello.sense.graph.presenters.TrendsV2Presenter;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.widget.TrendLayout;
import is.hello.sense.ui.widget.graphing.TrendGraphLinearLayout;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;

public class TrendsFragment extends BacksideTabFragment implements TrendLayout.OnRetry {
    @Inject
    TrendsV2Presenter trendsPresenter;

    private ProgressBar initialActivityIndicator;
    private SwipeRefreshLayout swipeRefreshLayout;

    private TrendGraphLinearLayout trendGraphLinearLayout;
    boolean randomize = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPresenter(trendsPresenter);
        setHasOptionsMenu(true);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.Backside.EVENT_TRENDS, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_trends, container, false);

        this.swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.fragment_trends_refresh_container);
        swipeRefreshLayout.setOnRefreshListener(this::fetchTrends);
        Styles.applyRefreshLayoutStyle(swipeRefreshLayout);
        this.initialActivityIndicator = (ProgressBar) view.findViewById(R.id.fragment_trends_loading);
        this.trendGraphLinearLayout = (TrendGraphLinearLayout) view.findViewById(R.id.fragment_trends_trendgraph);
        this.trendGraphLinearLayout.setAnimatorContext(getAnimatorContext());
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeRefreshLayout.setRefreshing(true);
        bindAndSubscribe(trendsPresenter.trends, this::bindTrends, this::presentError);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_trends, menu);
        super.onCreateOptionsMenu(menu, inflater);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        randomize = false;
        if (item.getItemId() == R.id.action_last_3_months) {
            trendsPresenter.updateTrend(Trends.TimeScale.LAST_3_MONTHS);
            fetchTrends();
            return true;
        }
        if (item.getItemId() == R.id.action_last_month) {
            trendsPresenter.updateTrend(Trends.TimeScale.LAST_MONTH);
            fetchTrends();
            return true;
        }
        if (item.getItemId() == R.id.action_last_week) {
            trendsPresenter.updateTrend(Trends.TimeScale.LAST_WEEK);
            fetchTrends();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        trendsPresenter.unbindScope();

        this.initialActivityIndicator = null;
        this.swipeRefreshLayout = null;
    }


    @Override
    public void onSwipeInteractionDidFinish() {
        WelcomeDialogFragment.showIfNeeded(getActivity(), R.xml.welcome_dialog_trends, false);
    }

    @Override
    public void onUpdate() {
        if (trendsPresenter.bindScope(getScope()) == BindResult.WAITING_FOR_VALUE) {
            fetchTrends();
        }
    }

    public void bindTrends(@NonNull Trends trends) {
        trendGraphLinearLayout.update(trends);
        swipeRefreshLayout.setRefreshing(false);
        initialActivityIndicator.setVisibility(View.GONE);
    }

    public void presentError(Throwable e) {
        trendGraphLinearLayout.presentError(this);
        swipeRefreshLayout.setRefreshing(false);
        initialActivityIndicator.setVisibility(View.GONE);
    }

    @Override
    public void fetchTrends() {
        if (randomize) {
            Random random = new Random();
            switch (random.nextInt(3)) {
                case 0:
                    trendsPresenter.updateTrend(Trends.TimeScale.LAST_3_MONTHS);
                    break;
                case 1:
                    trendsPresenter.updateTrend(Trends.TimeScale.LAST_MONTH);
                    break;
                default:
                    trendsPresenter.updateTrend(Trends.TimeScale.LAST_WEEK);

            }
        }
        swipeRefreshLayout.setRefreshing(true);
        trendsPresenter.update();
        randomize = true;
    }
}
