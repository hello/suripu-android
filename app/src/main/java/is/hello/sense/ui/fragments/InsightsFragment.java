package is.hello.sense.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Insight;
import is.hello.sense.graph.presenters.InsightsPresenter;
import is.hello.sense.ui.common.InjectionFragment;

public class InsightsFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    @Inject InsightsPresenter presenter;

    private InsightsAdapter insightsAdapter;

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

        this.insightsAdapter = new InsightsAdapter(getActivity());

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setAdapter(insightsAdapter);
        listView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(presenter.insights, insightsAdapter::bindInsights, insightsAdapter::insightsUnavailable);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Insight insight = insightsAdapter.getItem(position);
    }


    public static class InsightsAdapter extends ArrayAdapter<Insight> {
        public InsightsAdapter(@NonNull Context context) {
            super(context, R.layout.item_insight);
        }

        public void bindInsights(@NonNull List<Insight> insights) {
            clear();
            addAll(insights);
        }

        public void insightsUnavailable(Throwable e) {
            clear();
        }
    }
}
