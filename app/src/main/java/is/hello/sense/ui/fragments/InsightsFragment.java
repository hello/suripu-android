package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Insight;
import is.hello.sense.graph.presenters.InsightsPresenter;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import is.hello.sense.ui.adapter.InsightsAdapter;
import is.hello.sense.ui.dialogs.InsightInfoDialogFragment;
import is.hello.sense.ui.dialogs.QuestionsDialogFragment;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Markdown;

public class InsightsFragment extends UndersideTabFragment implements AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener, InsightsAdapter.Listener {
    @Inject InsightsPresenter insightsPresenter;
    @Inject Markdown markdown;
    @Inject DateFormatter dateFormatter;

    @Inject QuestionsPresenter questionsPresenter;

    private InsightsAdapter insightsAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPresenter(insightsPresenter);
        addPresenter(questionsPresenter);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.TopView.EVENT_MAIN_VIEW, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_insights, container, false);

        this.swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.fragment_insights_refresh_container);
        swipeRefreshLayout.setOnRefreshListener(this);
        Styles.applyRefreshLayoutStyle(swipeRefreshLayout);

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        Styles.addCardSpacing(listView, Styles.CARD_SPACING_HEADER_AND_FOOTER, null);

        this.insightsAdapter = new InsightsAdapter(getActivity(), markdown, dateFormatter, this);
        listView.setAdapter(insightsAdapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(insightsPresenter.insights,
                         insightsAdapter::bindInsights,
                         insightsAdapter::insightsUnavailable);

        bindAndSubscribe(questionsPresenter.currentQuestion,
                         insightsAdapter::bindCurrentQuestion,
                         insightsAdapter::currentQuestionUnavailable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        insightsPresenter.unbindScope();
        this.insightsAdapter = null;
    }

    @Override
    public void onSwipeInteractionDidFinish() {

    }

    @Override
    public void onUpdate() {
        if (!insightsPresenter.bindScope(getScope())) {
            swipeRefreshLayout.setRefreshing(true);
            insightsPresenter.update();
        }

        questionsPresenter.update();
    }


    //region Insights

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Analytics.trackEvent(Analytics.TopView.EVENT_INSIGHT_DETAIL, null);

        Insight insight = (Insight) adapterView.getItemAtPosition(position);
        InsightInfoDialogFragment dialogFragment = InsightInfoDialogFragment.newInstance(insight);
        dialogFragment.show(getFragmentManager(), InsightInfoDialogFragment.TAG);
    }

    @Override
    public void onRefresh() {
        swipeRefreshLayout.setRefreshing(true);
        insightsPresenter.update();
        questionsPresenter.update();
    }

    //endregion


    //region Questions

    @Override
    public void onDismissLoadingIndicator() {
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onSkipQuestion() {
        insightsAdapter.clearCurrentQuestion();
    }

    @Override
    public void onAnswerQuestion() {
        QuestionsDialogFragment dialogFragment = new QuestionsDialogFragment();
        dialogFragment.show(getActivity().getFragmentManager(), QuestionsDialogFragment.TAG);
        insightsAdapter.clearCurrentQuestion();
    }

    //endregion
}
