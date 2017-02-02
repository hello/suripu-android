package is.hello.sense.api.model.v2.voice;

import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.View;

import java.util.Arrays;

import is.hello.go99.ViewVisibility;
import is.hello.sense.R;
import is.hello.sense.util.AnimatorSetHandler;


public class VoiceTutorialFactory {

    public static boolean isFailState(final int[] senseImageState) {
        if (senseImageState == null){
            return true;
        }
        return (Arrays.equals(senseImageState, StateHolder.FAIL.state));
    }

    public enum VoiceTutorial {
        SUCCESS(QuestionTextState.SUCCESS,
                SenseImageState.SUCCESS_STATE),
        SUCCESS_FAIL_STATE(QuestionTextState.SUCCESS_FAIL_STATE,
                           SenseImageState.SUCCESS_STATE),
        NOT_DETECTED(QuestionTextState.NOT_DETECTED,
                     SenseImageState.SINGLE_FAIL_STATE),
        ERROR(QuestionTextState.ERROR,
              SenseImageState.LOOP_FAIL_STATE);

        private final QuestionTextState questionTextState;
        private final SenseImageState senseImageState;

        VoiceTutorial(@NonNull final QuestionTextState questionTextState,
                      @NonNull final SenseImageState senseImageState) {
            this.questionTextState = questionTextState;
            this.senseImageState = senseImageState;
        }

        public QuestionTextState getQuestionTextState() {
            return questionTextState;
        }

        public SenseImageState getSenseImageState() {
            return senseImageState;
        }

        @NonNull
        public int[] getState() {
            return senseImageState.getState();
        }

    }

    public enum QuestionTextState {
        SUCCESS(R.string.sense_voice_question_temperature,
                R.color.primary,
                View.INVISIBLE,
                false),
        SUCCESS_FAIL_STATE(R.string.sense_voice_question_temperature,
                           R.color.primary,
                           View.INVISIBLE,
                           true),
        NOT_DETECTED(R.string.error_sense_voice_not_detected,
                     R.color.text_dark,
                     View.INVISIBLE,
                     true),
        ERROR(R.string.error_sense_voice_problem,
              R.color.text_dark,
              View.GONE,
              true),
        FIRST_ON_WAKE_STATE(R.string.sense_voice_wake_phrase,
                            R.color.text_dark,
                            View.VISIBLE,
                            true),
        SECOND_ON_WAKE_STATE(R.string.sense_voice_question_temperature,
                             R.color.text_dark,
                             View.VISIBLE,
                             false);
        @StringRes
        public final int question;
        @ColorRes
        public final int color;
        public final int tryTextVisibility;
        public final boolean animateText;

        QuestionTextState(@StringRes final int question,
                          @ColorRes final int color,
                          @ViewVisibility final int tryTextVisibility,
                          final boolean animateText) {
            this.question = question;
            this.color = color;
            this.tryTextVisibility = tryTextVisibility;
            this.animateText = animateText;
        }
    }


    public enum SenseImageState {
        SUCCESS_STATE(AnimatorSetHandler.SINGLE_ANIMATION, StateHolder.OK),
        SINGLE_FAIL_STATE(AnimatorSetHandler.SINGLE_ANIMATION, StateHolder.FAIL),
        LOOP_FAIL_STATE(AnimatorSetHandler.LOOP_ANIMATION, StateHolder.FAIL),
        WAIT_STATE(AnimatorSetHandler.SINGLE_ANIMATION, StateHolder.WAIT),
        WAKE_STATE(AnimatorSetHandler.LOOP_ANIMATION, StateHolder.WAKE);
        @DrawableRes
        public static final int SRC_CIRCLE_DRAWABLE = R.drawable.sense_voice_circle_selector;
        private final int repeatCount;
        private final StateHolder stateHolder;

        SenseImageState(final int repeatCount,
                        final StateHolder stateHolder) {
            this.repeatCount = repeatCount;
            this.stateHolder = stateHolder;
        }

        @NonNull
        public int[] getState() {
            return stateHolder.state;
        }

        public int getRepeatCount() {
            return repeatCount;
        }
    }

    private enum StateHolder {
        WAKE(new int[]{android.R.attr.state_first}),
        FAIL(new int[]{android.R.attr.state_middle}),
        OK(new int[]{android.R.attr.state_last}),
        WAIT(new int[]{});

        private final int[] state;

        StateHolder(@NonNull final int[] state) {
            this.state = state;
        }


    }
}
