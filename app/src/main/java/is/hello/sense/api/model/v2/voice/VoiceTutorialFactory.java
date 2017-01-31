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

import static is.hello.sense.api.model.v2.voice.VoiceTutorialFactory.SenseImageState.FAIL_STATE;
import static is.hello.sense.api.model.v2.voice.VoiceTutorialFactory.SenseImageState.OK_STATE;
import static is.hello.sense.api.model.v2.voice.VoiceTutorialFactory.SenseImageState.WAIT_STATE;
import static is.hello.sense.api.model.v2.voice.VoiceTutorialFactory.SenseImageState.WAKE_STATE;

public class VoiceTutorialFactory {

    public VoiceTutorial getOnSuccess(final int[] senseImageState) {
        return new VoiceTutorial(new QuestionTextState(R.string.sense_voice_question_temperature,
                                                       R.color.primary,
                                                       View.INVISIBLE,
                                                       Arrays.equals(senseImageState, FAIL_STATE)),
                                 new SenseImageState(AnimatorSetHandler.SINGLE_ANIMATION,
                                                     OK_STATE));
    }

    public VoiceTutorial getOnNotDetected() {
        return new VoiceTutorial(new QuestionTextState(R.string.error_sense_voice_not_detected,
                                                       R.color.text_dark,
                                                       View.INVISIBLE,
                                                       true),
                                 new SenseImageState(AnimatorSetHandler.SINGLE_ANIMATION,
                                                     FAIL_STATE));
    }

    public VoiceTutorial getOnError() {
        return new VoiceTutorial(new QuestionTextState(R.string.error_sense_voice_problem,
                                                       R.color.text_dark,
                                                       View.GONE,
                                                       true),
                                 new SenseImageState(AnimatorSetHandler.LOOP_ANIMATION,
                                                     FAIL_STATE));
    }

    public QuestionTextState getFirstOnWakeState() {
        return new QuestionTextState(R.string.sense_voice_wake_phrase,
                                     R.color.text_dark,
                                     View.VISIBLE,
                                     true);
    }

    public QuestionTextState getSecondOnWakeState() {
        return new QuestionTextState(R.string.sense_voice_question_temperature,
                                     R.color.text_dark,
                                     View.VISIBLE,
                                     false);
    }

    public SenseImageState getSenseOnWaitState() {
        return new SenseImageState(AnimatorSetHandler.SINGLE_ANIMATION,
                                   WAIT_STATE);
    }

    public SenseImageState getSenseOnWakeState() {
        return new SenseImageState(AnimatorSetHandler.LOOP_ANIMATION,
                                   WAKE_STATE);
    }

    public static class VoiceTutorial {

        private final QuestionTextState questionTextState;
        private final SenseImageState senseImageState;

        private VoiceTutorial(@NonNull final QuestionTextState questionTextState,
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

    }

    public static class QuestionTextState {
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


    public static class SenseImageState {
        @DrawableRes
        public static final int SRC_CIRCLE_DRAWABLE = R.drawable.sense_voice_circle_selector;
        static final int[] WAKE_STATE = new int[]{android.R.attr.state_first};
        static final int[] FAIL_STATE = new int[]{android.R.attr.state_middle};
        static final int[] OK_STATE = new int[]{android.R.attr.state_last};
        static final int[] WAIT_STATE = new int[]{};
        public final int repeatCount;
        public final int[] state;

        SenseImageState(final int repeatCount,
                        final int[] state) {
            this.repeatCount = repeatCount;
            this.state = state;
        }
    }
}
