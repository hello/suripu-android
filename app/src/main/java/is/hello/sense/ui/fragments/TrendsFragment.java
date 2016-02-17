package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.graph.presenters.ScopedValuePresenter.BindResult;
import is.hello.sense.graph.presenters.TrendsPresenter;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.ui.widget.TrendLayout;
import is.hello.sense.ui.widget.graphing.TrendGraphLinearLayout;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;

public class TrendsFragment extends BacksideTabFragment implements TrendLayout.OnRetry, SelectorView.OnSelectionChangedListener {
    @Inject
    TrendsPresenter trendsPresenter;

    private ProgressBar initialActivityIndicator;
    private SwipeRefreshLayout swipeRefreshLayout;

    private TrendGraphLinearLayout trendGraphLinearLayout;

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
        //todo erase after design
        SelectorView selectorView = (SelectorView) view.findViewById(R.id.fragment_trends_time_scale);
        selectorView.addOption("Last Week", "Last Week", true);
        selectorView.addOption("Last Month", "Last Month", true);
        selectorView.addOption("Last 3 Months", "Last 3 Months", true);
        selectorView.setOnSelectionChangedListener(this);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeRefreshLayout.setRefreshing(true);
        bindAndSubscribe(trendsPresenter.trends, this::bindTrends, this::presentError);
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
        swipeRefreshLayout.setRefreshing(true);
        trendsPresenter.update();
    }

    @Override
    public void onSelectionChanged(int newSelectionIndex) {
        // todo erase after design
        swipeRefreshLayout.setRefreshing(true);
        if (newSelectionIndex == 2) {
            trendsPresenter.setTimeScale(Trends.TimeScale.LAST_3_MONTHS);
        } else if (newSelectionIndex == 1) {
            trendsPresenter.setTimeScale(Trends.TimeScale.LAST_MONTH);
        } else {
            trendsPresenter.setTimeScale(Trends.TimeScale.LAST_WEEK);
        }

    }
}
