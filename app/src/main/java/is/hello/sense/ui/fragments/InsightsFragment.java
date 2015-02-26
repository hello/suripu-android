package is.hello.sense.ui.fragments;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Insight;
import is.hello.sense.api.model.Question;
import is.hello.sense.graph.presenters.InsightsPresenter;
import is.hello.sense.graph.presenters.Presenter;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import is.hello.sense.ui.adapter.InsightsAdapter;
import is.hello.sense.ui.dialogs.InsightInfoDialogFragment;
import is.hello.sense.ui.dialogs.QuestionsDialogFragment;
import is.hello.sense.ui.widget.util.ListViews;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Markdown;

public class InsightsFragment extends UndersideTabFragment implements AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {
    @Inject InsightsPresenter insightsPresenter;
    @Inject Markdown markdown;

    @Inject QuestionsPresenter questionsPresenter;

    private InsightsAdapter insightsAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    private ListView listView;
    private View topSpacing;
    private ViewGroup questionContainer;
    private TextView questionAnswerTitle;

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


        this.listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);
        View[] spacers = new View[Styles.CARD_SPACING_OUT_COUNT];
        Styles.addCardSpacing(listView, Styles.CARD_SPACING_HEADER_AND_FOOTER, spacers);
        this.topSpacing = spacers[0];


        FrameLayout questionLayoutFix = new FrameLayout(getActivity());
        this.questionContainer = (ViewGroup) inflater.inflate(R.layout.sub_fragment_new_question, questionLayoutFix, false);
        this.questionAnswerTitle = (TextView) questionContainer.findViewById(R.id.sub_fragment_new_question_title);

        Button skipQuestion = (Button) questionContainer.findViewById(R.id.sub_fragment_new_question_skip);
        Views.setSafeOnClickListener(skipQuestion, ignored -> questionsPresenter.skipQuestion());

        Button answerQuestion = (Button) questionContainer.findViewById(R.id.sub_fragment_new_question_answer);
        Views.setSafeOnClickListener(answerQuestion, ignored -> answerQuestion());

        questionLayoutFix.addView(questionContainer);
        ListViews.addHeaderView(listView, questionLayoutFix, null, false);


        this.insightsAdapter = new InsightsAdapter(getActivity(), markdown, () -> swipeRefreshLayout.setRefreshing(false));
        listView.setAdapter(insightsAdapter);

        setQuestionVisible(false);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(insightsPresenter.insights, insightsAdapter::bindInsights, insightsAdapter::insightsUnavailable);

        bindAndSubscribe(questionsPresenter.currentQuestion, currentQuestion -> {
            if (currentQuestion == null || isAnswerQuestionOpen()) {
                hideNewQuestion();
            } else {
                showNewQuestion(currentQuestion);
            }
        }, ignored -> questionContainer.setVisibility(View.GONE));
    }

    @Override
    public void onSwipeInteractionDidFinish() {

    }

    @Override
    public void onUpdate() {
        onRefresh();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (level >= Presenter.BASE_TRIM_LEVEL) {
            insightsAdapter.clear();
        }
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

    public void setQuestionVisible(boolean isVisible) {
        Resources resources = getResources();
        int spacerHeight = resources.getDimensionPixelSize(R.dimen.gap_small);

        int newSpacingHeight;
        if (isVisible) {
            questionContainer.setVisibility(View.VISIBLE);
            newSpacingHeight = spacerHeight;
        } else {
            questionContainer.setVisibility(View.GONE);
            newSpacingHeight = spacerHeight - listView.getDividerHeight();
        }

        topSpacing.getLayoutParams().height = newSpacingHeight;
        topSpacing.requestLayout();
    }

    public void showNewQuestion(@NonNull Question question) {
        questionAnswerTitle.setText(question.getText());
        setQuestionVisible(true);
    }

    public void hideNewQuestion() {
        setQuestionVisible(false);
    }

    public boolean isAnswerQuestionOpen() {
        return (getFragmentManager().findFragmentByTag(QuestionsDialogFragment.TAG) != null);
    }

    public void answerQuestion() {
        QuestionsDialogFragment dialogFragment = new QuestionsDialogFragment();
        dialogFragment.show(getActivity().getFragmentManager(), QuestionsDialogFragment.TAG);
        hideNewQuestion();
    }

    //endregion
}
