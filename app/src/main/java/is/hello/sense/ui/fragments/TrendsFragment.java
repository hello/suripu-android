package is.hello.sense.ui.fragments;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.functional.Functions;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.presenters.ScopedValuePresenter.BindResult;
import is.hello.sense.graph.presenters.TrendsPresenter;
import is.hello.sense.ui.adapter.TrendsAdapter;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.recycler.CardItemDecoration;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;

public class TrendsFragment extends BacksideTabFragment implements TrendsAdapter.OnTrendOptionSelected, TrendsAdapter.OnRetry {
    @Inject TrendsPresenter trendsPresenter;

    private TrendsAdapter trendsAdapter;
    private ProgressBar initialActivityIndicator;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPresenter(trendsPresenter);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.Backside.EVENT_TRENDS, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_trends, container, false);

        this.swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.fragment_trends_refresh_container);
        swipeRefreshLayout.setOnRefreshListener(trendsPresenter::update);
        Styles.applyRefreshLayoutStyle(swipeRefreshLayout);

        final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.fragment_trends_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);

        final Resources resources = getResources();
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new CardItemDecoration(resources));

        final FadingEdgesItemDecoration fadingEdges =
                new FadingEdgesItemDecoration(layoutManager, resources,
                                              FadingEdgesItemDecoration.Style.ROUNDED_EDGES);
        fadingEdges.setInsets(getContentInsets());
        recyclerView.addItemDecoration(fadingEdges);

        this.trendsAdapter = new TrendsAdapter(getActivity());
        trendsAdapter.setOnTrendOptionSelected(this);
        recyclerView.setAdapter(trendsAdapter);

        this.initialActivityIndicator = (ProgressBar) view.findViewById(R.id.fragment_trends_loading);

        insetSwipeRefreshLayout(swipeRefreshLayout);

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
        this.trendsAdapter = null;

        this.initialActivityIndicator = null;
        this.swipeRefreshLayout = null;
    }

    @Override
    protected boolean automaticallyApplyContentInsets() {
        return false;
    }

    @Override
    public void onSwipeInteractionDidFinish() {
        WelcomeDialogFragment.showIfNeeded(getActivity(), R.xml.welcome_dialog_trends, false);
    }

    @Override
    public void onUpdate() {
        if (trendsPresenter.bindScope(getScope()) == BindResult.WAITING_FOR_VALUE) {
            trendsPresenter.update();
        }
    }

    public void bindTrends(@NonNull ArrayList<TrendsPresenter.Rendered> trends) {
        swipeRefreshLayout.setRefreshing(false);
        trendsAdapter.replaceAll(trends);
        initialActivityIndicator.setVisibility(View.GONE);
        if (Lists.isEmpty(trends)) {
            trendsAdapter.displayNoDataMessage(null);
        }
    }

    public void presentError(Throwable e) {
        swipeRefreshLayout.setRefreshing(false);
        trendsAdapter.clear();
        initialActivityIndicator.setVisibility(View.GONE);
        trendsAdapter.displayNoDataMessage(this);
    }

    @Override
    public void onTrendOptionSelected(int trendIndex, @NonNull String option) {
        swipeRefreshLayout.setRefreshing(true);
        bindAndSubscribe(trendsPresenter.updateTrend(trendIndex, option),
                         Functions.NO_OP,
                         this::presentError);
    }

    @Override
    public void fetchTrends() {
        trendsPresenter.update();
    }
}
