package is.hello.sense.graph.presenters.questions;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.model.Question.Choice;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.graph.presenters.questions.ReviewQuestionProvider.TriggerListener;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class ReviewQuestionProviderTests extends SenseTestCase {
    private final TrackingTriggerListener triggerListener;
    private final ReviewQuestionProvider questionProvider;

    public ReviewQuestionProviderTests() {
        this.triggerListener = new TrackingTriggerListener();
        this.questionProvider = new ReviewQuestionProvider(getResources(), triggerListener);
        assertThat(questionProvider.getCurrentQuestion(), is(notNullValue()));
    }

    @After
    public void tearDown() throws Exception {
        triggerListener.calls.clear();
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void initialPrompt() {
        questionProvider.setCurrentQuestionId(ReviewQuestionProvider.QUESTION_ID_INITIAL);

        Question question = questionProvider.getCurrentQuestion();
        assertThat(question, is(notNullValue()));
        assertThat(question.getId(), is(equalTo(ReviewQuestionProvider.QUESTION_ID_INITIAL)));
        assertThat(question.getText(),
                   is(equalTo(getString(R.string.question_text_rating_prompt_initial))));
        assertThat(question.getChoices().size(), is(equalTo(3)));


        Choice firstChoice = question.getChoices().get(0);
        questionProvider.answerCurrent(Lists.newArrayList(firstChoice));

        Question first = questionProvider.getCurrentQuestion();
        assertThat(first, is(notNullValue()));
        assertThat(first.getId(), is(equalTo(ReviewQuestionProvider.QUESTION_ID_GOOD)));

        Choice secondChoice = question.getChoices().get(1);
        questionProvider.answerCurrent(Lists.newArrayList(secondChoice));

        Question second = questionProvider.getCurrentQuestion();
        assertThat(second, is(notNullValue()));
        assertThat(second.getId(), is(equalTo(ReviewQuestionProvider.QUESTION_ID_BAD)));

        Choice thirdChoice = question.getChoices().get(2);
        questionProvider.answerCurrent(Lists.newArrayList(thirdChoice));

        assertThat(triggerListener.calls, hasItem(TrackingTriggerListener.Call.SHOW_HELP));
        assertThat(questionProvider.getCurrentQuestion(), is(nullValue()));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void goodPrompt() {
        questionProvider.setCurrentQuestionId(ReviewQuestionProvider.QUESTION_ID_GOOD);

        Question question = questionProvider.getCurrentQuestion();
        assertThat(question, is(notNullValue()));
        assertThat(question.getId(), is(equalTo(ReviewQuestionProvider.QUESTION_ID_GOOD)));
        assertThat(question.getText(),
                   is(equalTo(getString(R.string.question_text_rating_prompt_good))));
        assertThat(question.getChoices().size(), is(equalTo(3)));


        Choice firstChoice = question.getChoices().get(0);
        questionProvider.answerCurrent(Lists.newArrayList(firstChoice));

        assertThat(triggerListener.calls, hasItem(TrackingTriggerListener.Call.WRITE_REVIEW));
        assertThat(questionProvider.getCurrentQuestion(), is(nullValue()));

        Choice secondChoice = question.getChoices().get(1);
        questionProvider.answerCurrent(Lists.newArrayList(secondChoice));

        Question second = questionProvider.getCurrentQuestion();
        assertThat(second, is(nullValue()));

        Choice thirdChoice = question.getChoices().get(2);
        questionProvider.answerCurrent(Lists.newArrayList(thirdChoice));

        assertThat(triggerListener.calls, hasItem(TrackingTriggerListener.Call.SUPPRESS_PROMPT));
        assertThat(questionProvider.getCurrentQuestion(), is(nullValue()));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void badPrompt() {
        questionProvider.setCurrentQuestionId(ReviewQuestionProvider.QUESTION_ID_BAD);

        Question question = questionProvider.getCurrentQuestion();
        assertThat(question, is(notNullValue()));
        assertThat(question.getId(), is(equalTo(ReviewQuestionProvider.QUESTION_ID_BAD)));
        assertThat(question.getText(),
                   is(equalTo(getString(R.string.question_text_rating_prompt_bad))));
        assertThat(question.getChoices().size(), is(equalTo(2)));


        Choice firstChoice = question.getChoices().get(0);
        questionProvider.answerCurrent(Lists.newArrayList(firstChoice));

        assertThat(triggerListener.calls, hasItem(TrackingTriggerListener.Call.SEND_FEEDBACK));
        assertThat(questionProvider.getCurrentQuestion(), is(nullValue()));

        Choice secondChoice = question.getChoices().get(1);
        questionProvider.answerCurrent(Lists.newArrayList(secondChoice));

        Question second = questionProvider.getCurrentQuestion();
        assertThat(second, is(nullValue()));
    }

    @Test
    public void skipQuestion() {
        questionProvider.setCurrentQuestionId(ReviewQuestionProvider.QUESTION_ID_INITIAL);

        questionProvider.skipCurrent();
        assertThat(questionProvider.getCurrentQuestion(), is(nullValue()));
        assertThat(triggerListener.calls, hasItem(TrackingTriggerListener.Call.SUPPRESS_PROMPT));
    }

    static class TrackingTriggerListener implements TriggerListener {
        final List<Call> calls = new ArrayList<>();

        @Override
        public void onWriteReview() {
            calls.add(Call.WRITE_REVIEW);
        }

        @Override
        public void onSendFeedback() {
            calls.add(Call.SEND_FEEDBACK);
        }

        @Override
        public void onShowHelp() {
            calls.add(Call.SHOW_HELP);
        }

        @Override
        public void onSuppressPrompt(boolean forever) {
            calls.add(Call.SUPPRESS_PROMPT);
        }


        enum Call {
            WRITE_REVIEW,
            SEND_FEEDBACK,
            SHOW_HELP,
            SUPPRESS_PROMPT,
        }
    }
}
