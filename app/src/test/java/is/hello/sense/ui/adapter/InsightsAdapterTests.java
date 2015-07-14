package is.hello.sense.ui.adapter;

import android.support.annotation.NonNull;
import android.util.Pair;
import android.view.View;
import android.widget.FrameLayout;

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.api.model.Insight;
import is.hello.sense.api.model.InsightCategory;
import is.hello.sense.api.model.Question;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.ListAdapterTesting;
import is.hello.sense.util.markup.text.MarkupString;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InsightsAdapterTests extends SenseTestCase {
    private final FrameLayout fakeParent = new FrameLayout(getContext());
    private final FakeListener listener = new FakeListener();
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
        adapter.bindData(Pair.create(null, null));
        assertTrue(listener.wasCallbackCalled(FakeListener.Callback.DISMISS_LOADING_INDICATOR));
    }

    @Test
    public void questionRendering() throws Exception {
        Question question = Question.create(0, 0, "Do you like to travel through space and time?",
                Question.Type.CHOICE, LocalDateTime.now(), Question.AskTime.ANYTIME, null);

        adapter.bindData(Pair.create(null, question));

        assertEquals(1, adapter.getCount());

        View view = adapter.getView(0, null, fakeParent);
        InsightsAdapter.QuestionViewHolder holder = ListAdapterTesting.getViewHolder(view);

        assertEquals("Do you like to travel through space and time?", holder.title.getText().toString());

        holder.skip(fakeParent);
        assertTrue(listener.wasCallbackCalled(FakeListener.Callback.SKIP_QUESTION));

        holder.answer(fakeParent);
        assertTrue(listener.wasCallbackCalled(FakeListener.Callback.ANSWER_QUESTION));
    }

    @Test
    public void insightRendering() throws Exception {
        Insight insight = Insight.create(0, "Light is bad",
                new MarkupString("You should have less of it"), DateTime.now().minusDays(5),
                InsightCategory.LIGHT, "Too much light makes you sleep poorly");

        adapter.bindData(Pair.create(Lists.newArrayList(insight), null));

        assertEquals(1, adapter.getCount());

        View view = adapter.getView(0, null, fakeParent);
        InsightsAdapter.InsightViewHolder holder = ListAdapterTesting.getViewHolder(view);

        assertEquals("5 days ago", holder.date.getText().toString());
        assertEquals("Too much light makes you sleep poorly", holder.preview.getText().toString());
        assertEquals("You should have less of it", holder.body.getText().toString());
        assertEquals(View.VISIBLE, holder.previewDivider.getVisibility());
    }

    //endregion


    static class FakeListener implements InsightsAdapter.Listener {
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
        }
    }
}
