package is.hello.sense.graph.presenters.questions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.model.Question.Choice;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class ReviewQuestionProviderTests extends InjectionTestCase {
    @Inject ApiService apiService;

    private final TrackingReceiver trackingReceiver;
    private final ReviewQuestionProvider questionProvider;

    public ReviewQuestionProviderTests() {
        this.trackingReceiver = new TrackingReceiver();
        LocalBroadcastManager.getInstance(getContext())
                             .registerReceiver(trackingReceiver,
                                               new IntentFilter(ReviewQuestionProvider.ACTION_COMPLETED));

        this.questionProvider = new ReviewQuestionProvider(getContext(),
                                                           apiService);
        assertThat(questionProvider.getCurrentQuestion(), is(notNullValue()));
    }

    @After
    public void tearDown() throws Exception {
        trackingReceiver.responses.clear();
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void saveState() {
        questionProvider.setCurrentQuestionId(ReviewQuestionProvider.QUESTION_ID_GOOD);

        Bundle savedState = questionProvider.saveState();
        assertThat(savedState, is(notNullValue()));
        assertThat(savedState.size(), is(not(0)));

        questionProvider.setCurrentQuestionId(ReviewQuestionProvider.QUESTION_ID_NONE);

        questionProvider.restoreState(savedState);
        assertThat(questionProvider.currentQuestionId,
                   is(equalTo(ReviewQuestionProvider.QUESTION_ID_GOOD)));
        assertThat(questionProvider.getCurrentQuestion(),
                   is(notNullValue()));
    }

    @Test
    public void lowMemory() {
        questionProvider.setCurrentQuestionId(ReviewQuestionProvider.QUESTION_ID_INITIAL);

        assertThat(questionProvider.lowMemory(), is(true));

        assertThat(questionProvider.currentQuestionId,
                   is(equalTo(ReviewQuestionProvider.QUESTION_ID_NONE)));
        assertThat(questionProvider.getCurrentQuestion(),
                   is(nullValue()));
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

        assertThat(trackingReceiver.responses, hasItem(ReviewQuestionProvider.RESPONSE_SHOW_HELP));
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

        assertThat(trackingReceiver.responses, hasItem(ReviewQuestionProvider.RESPONSE_WRITE_REVIEW));
        assertThat(questionProvider.getCurrentQuestion(), is(nullValue()));

        Choice secondChoice = question.getChoices().get(1);
        questionProvider.answerCurrent(Lists.newArrayList(secondChoice));

        Question second = questionProvider.getCurrentQuestion();
        assertThat(second, is(nullValue()));

        Choice thirdChoice = question.getChoices().get(2);
        questionProvider.answerCurrent(Lists.newArrayList(thirdChoice));

        assertThat(trackingReceiver.responses, hasItem(ReviewQuestionProvider.RESPONSE_SUPPRESS_TEMPORARILY));
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

        assertThat(trackingReceiver.responses, hasItem(ReviewQuestionProvider.RESPONSE_SEND_FEEDBACK));
        assertThat(questionProvider.getCurrentQuestion(), is(nullValue()));

        Choice secondChoice = question.getChoices().get(1);
        questionProvider.answerCurrent(Lists.newArrayList(secondChoice));

        Question second = questionProvider.getCurrentQuestion();
        assertThat(second, is(nullValue()));
    }

    @Test
    public void skipQuestionAdvanceImmediately() {
        questionProvider.setCurrentQuestionId(ReviewQuestionProvider.QUESTION_ID_INITIAL);

        questionProvider.skipCurrent(true).subscribe();
        assertThat(questionProvider.getCurrentQuestion(), is(nullValue()));
        assertThat(trackingReceiver.responses, hasItem(ReviewQuestionProvider.RESPONSE_SUPPRESS_TEMPORARILY));
    }

    @Test
    public void skipQuestionAdvanceDeferred() {
        questionProvider.setCurrentQuestionId(ReviewQuestionProvider.QUESTION_ID_INITIAL);

        Sync.last(questionProvider.skipCurrent(false));
        assertThat(questionProvider.getCurrentQuestion(), is(nullValue()));
        assertThat(trackingReceiver.responses, hasItem(ReviewQuestionProvider.RESPONSE_SUPPRESS_TEMPORARILY));
    }

    static class TrackingReceiver extends BroadcastReceiver {
        final List<Integer> responses = new ArrayList<>();
        
        @Override
        public void onReceive(Context context, Intent intent) {
            final int response = intent.getIntExtra(ReviewQuestionProvider.EXTRA_RESPONSE,
                                                    ReviewQuestionProvider.RESPONSE_SUPPRESS_TEMPORARILY);
            responses.add(response);
        }
    }
}
