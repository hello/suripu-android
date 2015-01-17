package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.TrendGraph;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.TrendsPresenter;
import is.hello.sense.ui.adapter.TrendsAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.util.Styles;

public class TrendsFragment extends InjectionFragment implements TrendsAdapter.OnTrendOptionSelected {
    @Inject TrendsPresenter trendsPresenter;

    private TrendsAdapter trendsAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        trendsPresenter.update();
        addPresenter(trendsPresenter);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trends, container, false);

        this.swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.fragment_trends_refresh_container);
        swipeRefreshLayout.setOnRefreshListener(trendsPresenter::update);
        Styles.applyRefreshLayoutStyle(swipeRefreshLayout);

        ListView listView = (ListView) view.findViewById(android.R.id.list);

        this.trendsAdapter = new TrendsAdapter(getActivity());
        trendsAdapter.setOnTrendOptionSelected(this);
        listView.setAdapter(trendsAdapter);

        Styles.addCardSpacingHeaderAndFooter(listView);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeRefreshLayout.setRefreshing(true);
        bindAndSubscribe(trendsPresenter.trends, this::bindTrends, this::presentError);
    }


    public void bindTrends(@NonNull ArrayList<TrendGraph> trends) {
        swipeRefreshLayout.setRefreshing(false);
        trendsAdapter.bindTrends(trends);
    }

    public void presentError(Throwable e) {
        swipeRefreshLayout.setRefreshing(false);

    }


    @Override
    public void onTrendOptionSelected(int trendIndex, @NonNull String option) {
        swipeRefreshLayout.setRefreshing(true);
        bindAndSubscribe(trendsPresenter.updateTrend(trendIndex, option), Functions.NO_OP, this::presentError);
    }
}
