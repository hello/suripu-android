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
import is.hello.sense.graph.presenters.TrendsPresenter;
import is.hello.sense.ui.adapter.TrendsAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.util.Styles;

public class TrendsFragment extends InjectionFragment implements SwipeRefreshLayout.OnRefreshListener {
    @Inject TrendsPresenter trendsPresenter;

    private TrendsAdapter trendsAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPresenter(trendsPresenter);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trends, container, false);

        this.swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.fragment_trends_refresh_container);
        swipeRefreshLayout.setOnRefreshListener(this);

        ListView listView = (ListView) view.findViewById(android.R.id.list);

        this.trendsAdapter = new TrendsAdapter(getActivity());
        listView.setAdapter(trendsAdapter);

        Styles.addCardSpacingHeaderAndFooter(listView);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(trendsPresenter.trends, this::bindTrends, this::presentError);
    }

    @Override
    public void onResume() {
        super.onResume();

        swipeRefreshLayout.setRefreshing(true);
        trendsPresenter.update();
    }


    public void bindTrends(@NonNull ArrayList<TrendGraph> trends) {
        swipeRefreshLayout.setRefreshing(false);
        trendsAdapter.bindTrends(trends);
    }

    public void presentError(Throwable e) {
        swipeRefreshLayout.setRefreshing(false);
        ErrorDialogFragment.presentError(getFragmentManager(), e);

        trendsAdapter.trendsUnavailable(e);
    }


    @Override
    public void onRefresh() {
        trendsPresenter.update();
    }
}
