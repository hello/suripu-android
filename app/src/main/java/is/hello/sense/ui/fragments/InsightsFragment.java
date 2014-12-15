package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Insight;
import is.hello.sense.graph.presenters.InsightsPresenter;
import is.hello.sense.graph.presenters.Presenter;
import is.hello.sense.ui.adapter.InsightsAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.InsightDetailsDialogFragment;
import is.hello.sense.util.Markdown;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class InsightsFragment extends InjectionFragment implements AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {
    @Inject InsightsPresenter presenter;
    @Inject Markdown markdown;

    private InsightsAdapter insightsAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        presenter.update();
        addPresenter(presenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_insights, container, false);

        this.swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.fragment_insights_refresh_container);
        swipeRefreshLayout.setOnRefreshListener(this);

        this.insightsAdapter = new InsightsAdapter(getActivity(), markdown, () -> swipeRefreshLayout.setRefreshing(false));

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setAdapter(insightsAdapter);
        listView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeRefreshLayout.setRefreshing(true);
        bindAndSubscribe(presenter.insights, insightsAdapter::bindInsights, insightsAdapter::insightsUnavailable);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (level >= Presenter.BASE_TRIM_LEVEL) {
            insightsAdapter.clear();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Insight insight = insightsAdapter.getItem(position);
        InsightDetailsDialogFragment dialogFragment = InsightDetailsDialogFragment.newInstance(insight);
        dialogFragment.show(getFragmentManager(), InsightDetailsDialogFragment.TAG);
    }

    @Override
    public void onRefresh() {
        swipeRefreshLayout.setRefreshing(true);
        presenter.update();
    }
}
