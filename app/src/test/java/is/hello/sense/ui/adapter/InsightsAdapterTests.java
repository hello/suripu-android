package is.hello.sense.ui.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.api.model.Insight;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.model.v2.ImageUrl;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.RecyclerAdapterTesting;
import is.hello.sense.util.RecyclerAdapterTesting.Observer;
import is.hello.sense.util.markup.text.MarkupString;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class InsightsAdapterTests extends SenseTestCase {
    private final FrameLayout fakeParent = new FrameLayout(getContext());
    private final FakeInteractionListener listener = new FakeInteractionListener();
    private final DateFormatter dateFormatter = new DateFormatter(getContext());
    private InsightsAdapter adapter;

    //region Lifecycle

    @Before
    public void setUp() {
        this.adapter = new InsightsAdapter(getContext(), dateFormatter, listener);
    }

    @After
    public void tearDown() {
        listener.clear();
    }

    //endregion


    //region Rendering

    @Test
    public void loadingIndicatorHook() throws Exception {
        adapter.bindQuestion(null);
        assertThat(listener.wasCallbackCalled(FakeInteractionListener.Callback.DISMISS_LOADING_INDICATOR),
                   is(true));
    }

    @Test
    public void questionRendering() throws Exception {
        Question question = Question.create(0, 0, "Do you like to travel through space and time?",
                                            Question.Type.CHOICE, DateTime.now(), Question.AskTime.ANYTIME, null);

        adapter.bindQuestion(question);

        assertThat(adapter.getItemCount(), is(equalTo(1)));

        final InsightsAdapter.QuestionViewHolder holder =
                RecyclerAdapterTesting.createAndBindView(adapter, fakeParent, 0);

        assertThat(holder.title.getText().toString(), is(equalTo("Do you like to travel through space and time?")));

        holder.skip(fakeParent);
        assertThat(listener.wasCallbackCalled(FakeInteractionListener.Callback.SKIP_QUESTION),
                   is(true));

        holder.answer(fakeParent);
        assertThat(listener.wasCallbackCalled(FakeInteractionListener.Callback.ANSWER_QUESTION),
                   is(true));
    }

    @Test
    public void insightRendering() throws Exception {
        final Insight insight = Insight.create(0, "Light is bad",
                                               new MarkupString("You should have less of it"),
                                               DateTime.now().minusDays(5),
                                               "LIGHT", "Too much light makes you sleep poorly");

        adapter.bindInsights(Lists.newArrayList(insight));

        assertThat(adapter.getItemCount(), is(equalTo(1)));

        final InsightsAdapter.InsightViewHolder holder =
                RecyclerAdapterTesting.createAndBindView(adapter, fakeParent, 0);

        assertThat(holder.date.getText().toString(), is(equalTo("5 days ago")));
        assertThat(holder.body.getText().toString(), is(equalTo("You should have less of it")));
    }

    @Test
    public void loadingInsights() throws Exception {
        final Insight insight = Insight.create(0, "Light is bad",
                                               new MarkupString("You should have less of it"),
                                               DateTime.now().minusDays(5),
                                               "LIGHT", "Too much light makes you sleep poorly");

        adapter.bindInsights(Lists.newArrayList(insight));

        assertThat(adapter.getItemCount(), is(equalTo(1)));

        final InsightsAdapter.InsightViewHolder holder1 =
                RecyclerAdapterTesting.createAndBindView(adapter, fakeParent, 0);

        assertThat(holder1.itemView.isClickable(), is(true));

        final Observer observer = new Observer();
        adapter.registerAdapterDataObserver(observer);

        adapter.setLoadingInsightPosition(0);
        observer.assertChangeOccurred(Observer.Change.Type.CHANGED, 0, 1);

        final InsightsAdapter.InsightViewHolder holder2 =
                RecyclerAdapterTesting.createAndBindView(adapter, fakeParent, 0);

        assertThat(holder2.itemView.isClickable(), is(false));

        observer.reset();
        adapter.setLoadingInsightPosition(RecyclerView.NO_POSITION);
        observer.assertChangeOccurred(Observer.Change.Type.CHANGED, 0, 1);
    }

    //endregion


    static class FakeInteractionListener implements InsightsAdapter.InteractionListener {
        final List<Callback> callbacks = new ArrayList<>();

        @Override
        public void onDismissLoadingIndicator() {
            callbacks.add(Callback.DISMISS_LOADING_INDICATOR);
        }

        @Override
        public void onSkipQuestion() {
            callbacks.add(Callback.SKIP_QUESTION);
        }

        @Override
        public void onAnswerQuestion() {
            callbacks.add(Callback.ANSWER_QUESTION);
        }

        @Override
        public void onInsightClicked(int position, @NonNull Insight insight) {
            callbacks.add(Callback.INSIGHT_CLICKED);
        }

        void clear() {
            callbacks.clear();
        }

        boolean wasCallbackCalled(@NonNull Callback callback) {
            return callbacks.contains(callback);
        }


        enum Callback {
            DISMISS_LOADING_INDICATOR,
            SKIP_QUESTION,
            ANSWER_QUESTION,
            INSIGHT_CLICKED,
        }
    }
}
