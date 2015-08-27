package is.hello.sense.graph.presenters.questions;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import is.hello.sense.api.model.Question;
import rx.Observable;

/**
 * An object that provides questions for {@link is.hello.sense.graph.presenters.QuestionsPresenter}
 * and reacts to submitted answers and skip interactions from the user.
 * <p>
 * This class exists to allow reviews and questions to be presented
 * from the same user interface, using the same basic logic.
 */
public interface QuestionProvider {
    String LOG_TAG = QuestionProvider.class.getSimpleName();

    //region Lifecycle

    /**
     * A stable identifier of the provider.
     * <p>
     * Used to prevent unwanted deserialization mismatches.
     */
    String getName();

    /**
     * Save the state of the question provider into a bundle.
     * @return The state of the question provider; or null if it has none.
     */
    @Nullable Bundle saveState();

    /**
     * Restore the state of the question provider. Only called if
     * {@link #saveState()} returns a non-null value.
     * <p>
     * State restoration should not overwrite state applied to the provider
     * between its creation and <code>restoreState</code> being called.
     * @param savedState    The bundle from the earlier {@link #saveState()} call.
     */
    void restoreState(@NonNull Bundle savedState);

    /**
     * Informs the provider that low memory conditions have been
     * encountered and it should eject any in-memory state it can.
     *
     * @return Whether or not the provider was able to react to low memory.
     */
    boolean lowMemory();

    //endregion

    //region Binding

    /**
     * Allows the question provider to perform asynchronous state initialization
     * before the question presenter binds to the values of the provider.
     *
     * @return State initialization represented as an {@link Observable}.
     */
    Observable<Question> prepare();

    /**
     * Retrieve the current question from the provider.
     * <p>
     * Must be synchronously available.
     * @return  The current question if available; null otherwise.
     */
    @Nullable Question getCurrentQuestion();

    /**
     * Called when the user has entered the question-answer flow.
     */
    void userEnteredFlow();

    /**
     * Called in response to the user making a selection on a question's
     * choices and submitting them from the app's user interface.
     * @param choices   The user's choices.
     */
    void answerCurrent(@NonNull List<Question.Choice> choices);

    /**
     * Called in response to the user skipping a question.
     * @param advanceImmediately Whether or not the current question should be updated before returning.
     */
    Observable<Void> skipCurrent(boolean advanceImmediately);

    //endregion
}
