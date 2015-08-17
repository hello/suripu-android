package is.hello.sense.graph.presenters.questions;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.model.Question.Choice;
import rx.Observable;

public class ReviewQuestionProvider implements QuestionProvider {
    static final long QUESTION_ID_NONE = -1;
    static final long QUESTION_ID_INITIAL = 0;
    static final long QUESTION_ID_GOOD = 1;
    static final long QUESTION_ID_BAD = 2;

    private final Resources resources;
    private final TriggerListener triggerListener;

    private Question currentQuestion;
    @VisibleForTesting long currentQuestionId;

    //region Lifecycle

    public ReviewQuestionProvider(@NonNull Resources resources,
                                  @NonNull TriggerListener triggerListener) {
        this.resources = resources;
        this.triggerListener = triggerListener;
        setCurrentQuestionId(QUESTION_ID_INITIAL);
    }

    @Override
    public String getName() {
        // Should not change between releases
        return "ReviewQuestionProvider";
    }

    @Nullable
    @Override
    public Bundle saveState() {
        Bundle savedState = new Bundle();
        savedState.putLong("currentQuestionId", currentQuestionId);
        return savedState;
    }

    @Override
    public void restoreState(@NonNull Bundle savedState) {
        setCurrentQuestionId(savedState.getLong("currentQuestionId"));
    }

    @Override
    public boolean lowMemory() {
        this.currentQuestionId = QUESTION_ID_NONE;
        this.currentQuestion = null;

        return true;
    }

    public String getString(int id) throws Resources.NotFoundException {
        return resources.getString(id);
    }

    //endregion


    //region Questions

    @Override
    public Observable<Question> prepare() {
        return Observable.just(getCurrentQuestion());
    }

    void setCurrentQuestionId(long currentQuestionId) {
        this.currentQuestionId = currentQuestionId;
        if (currentQuestionId == QUESTION_ID_NONE) {
            this.currentQuestion = null;
        } else if (currentQuestionId == QUESTION_ID_INITIAL) {
            List<Choice> choices = new ArrayList<>(3);
            choices.add(Choice.create(R.string.question_text_rating_prompt_initial_yes,
                                      getString(R.string.question_text_rating_prompt_initial_yes)));
            choices.add(Choice.create(R.string.question_text_rating_prompt_initial_no,
                                      getString(R.string.question_text_rating_prompt_initial_no)));
            choices.add(Choice.create(R.string.question_text_rating_prompt_initial_help,
                                      getString(R.string.question_text_rating_prompt_initial_help)));
            this.currentQuestion = Question.create(currentQuestionId, 0,
                                                   resources.getString(R.string.question_text_rating_prompt_initial),
                                                   Question.Type.CHOICE,
                                                   DateTime.now(),
                                                   Question.AskTime.ANYTIME,
                                                   choices);
        } else if (currentQuestionId == QUESTION_ID_GOOD) {
            List<Choice> choices = new ArrayList<>(3);
            choices.add(Choice.create(R.string.question_text_rating_prompt_good_yes,
                                      getString(R.string.question_text_rating_prompt_good_yes)));
            choices.add(Choice.create(R.string.question_text_rating_prompt_good_no,
                                      getString(R.string.question_text_rating_prompt_good_no)));
            choices.add(Choice.create(R.string.question_text_rating_prompt_good_never,
                                      getString(R.string.question_text_rating_prompt_good_never)));
            this.currentQuestion = Question.create(currentQuestionId, 0,
                                                   resources.getString(R.string.question_text_rating_prompt_good),
                                                   Question.Type.CHOICE,
                                                   DateTime.now(),
                                                   Question.AskTime.ANYTIME,
                                                   choices);
        } else if (currentQuestionId == QUESTION_ID_BAD) {
            List<Choice> choices = new ArrayList<>(2);
            choices.add(Choice.create(R.string.question_text_rating_prompt_bad_yes,
                                      getString(R.string.question_text_rating_prompt_bad_yes)));
            choices.add(Choice.create(R.string.question_text_rating_prompt_bad_no,
                                      getString(R.string.question_text_rating_prompt_bad_no)));
            this.currentQuestion = Question.create(currentQuestionId, 0,
                                                   resources.getString(R.string.question_text_rating_prompt_bad),
                                                   Question.Type.CHOICE,
                                                   DateTime.now(),
                                                   Question.AskTime.ANYTIME,
                                                   choices);
        } else {
            throw new IllegalArgumentException("Unknown question id");
        }
    }

    @Nullable
    @Override
    public Question getCurrentQuestion() {
        return currentQuestion;
    }

    @Override
    public void answerCurrent(@NonNull List<Choice> choices) {
        int choiceId = (int) choices.get(0).getId();
        switch (choiceId) {
            // First screen
            case R.string.question_text_rating_prompt_initial_yes: {
                setCurrentQuestionId(QUESTION_ID_GOOD);
                break;
            }
            case R.string.question_text_rating_prompt_initial_no: {
                setCurrentQuestionId(QUESTION_ID_BAD);
                break;
            }
            case R.string.question_text_rating_prompt_initial_help: {
                setCurrentQuestionId(QUESTION_ID_NONE);
                triggerListener.onShowHelp();
                break;
            }

            // Second screen (good)
            case R.string.question_text_rating_prompt_good_yes: {
                setCurrentQuestionId(QUESTION_ID_NONE);
                triggerListener.onWriteReview();
                break;
            }
            case R.string.question_text_rating_prompt_good_no: {
                setCurrentQuestionId(QUESTION_ID_NONE);
                break;
            }
            case R.string.question_text_rating_prompt_good_never: {
                setCurrentQuestionId(QUESTION_ID_NONE);
                triggerListener.onSuppressPrompt(true);
                break;
            }

            // Second screen (bad)
            case R.string.question_text_rating_prompt_bad_yes: {
                setCurrentQuestionId(QUESTION_ID_NONE);
                triggerListener.onSendFeedback();
                break;
            }
            case R.string.question_text_rating_prompt_bad_no: {
                setCurrentQuestionId(QUESTION_ID_NONE);
                triggerListener.onSuppressPrompt(false);
                break;
            }
        }
    }

    @Override
    public void skipCurrent() {
        setCurrentQuestionId(QUESTION_ID_NONE);
        triggerListener.onSuppressPrompt(false);
    }

    //endregion


    public interface TriggerListener {
        void onWriteReview();
        void onSendFeedback();
        void onShowHelp();
        void onSuppressPrompt(boolean forever);
    }
}
