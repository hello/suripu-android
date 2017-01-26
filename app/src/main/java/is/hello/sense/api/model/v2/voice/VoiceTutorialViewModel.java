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

import static is.hello.sense.api.model.v2.voice.VoiceTutorialViewModel.SenseImageViewModel.FAIL_STATE;
import static is.hello.sense.api.model.v2.voice.VoiceTutorialViewModel.SenseImageViewModel.OK_STATE;

public class VoiceTutorialViewModel {

    private final QuestionTextViewModel questionTextViewModel;
    private final SenseImageViewModel senseImageViewModel;

    public static VoiceTutorialViewModel getOnSuccessModel(final int[] senseImageState) {
        return new VoiceTutorialViewModel(new QuestionTextViewModel(R.string.sense_voice_question_temperature,
                                                                    R.color.primary,
                                                                    View.INVISIBLE,
                                                                    Arrays.equals(senseImageState, FAIL_STATE)),
                                          new SenseImageViewModel(0, OK_STATE));
    }

    public static VoiceTutorialViewModel getOnNotDetectedModel() {
        return new VoiceTutorialViewModel(new QuestionTextViewModel(R.string.error_sense_voice_not_detected,
                                                             R.color.text_dark,
                                                             View.INVISIBLE,
                                                             true),
                                   new SenseImageViewModel(0, FAIL_STATE));
    }

    public static VoiceTutorialViewModel getOnErrorModel() {
        return new VoiceTutorialViewModel(new QuestionTextViewModel(R.string.error_sense_voice_problem,
                                                                    R.color.text_dark,
                                                                    View.GONE,
                                                                    true),
                                          new SenseImageViewModel(AnimatorSetHandler.LOOP_ANIMATION,
                                                                  FAIL_STATE));
    }

    private VoiceTutorialViewModel(@NonNull final QuestionTextViewModel questionTextViewModel,
                                  @NonNull final  SenseImageViewModel senseImageViewModel) {
        this.questionTextViewModel = questionTextViewModel;
        this.senseImageViewModel = senseImageViewModel;
    }

    public QuestionTextViewModel getQuestionTextViewModel() {
        return questionTextViewModel;
    }

    public SenseImageViewModel getSenseImageViewModel() {
        return senseImageViewModel;
    }

    public static class QuestionTextViewModel {
        @StringRes
        public final int question;
        @ColorRes
        public final int color;
        public final int tryTextVisibility;
        public final boolean animateText;

        public static QuestionTextViewModel getFirstOnWakeModel() {
            return new QuestionTextViewModel(R.string.sense_voice_wake_phrase,
                                             R.color.text_dark,
                                             View.VISIBLE,
                                             true);
        }

        public static QuestionTextViewModel getSecondOnWakeModel() {
            return new QuestionTextViewModel(R.string.sense_voice_question_temperature,
                                             R.color.text_dark,
                                             View.VISIBLE,
                                             false);
        }

        QuestionTextViewModel(@StringRes final int question,
                              @ColorRes final int color,
                              @ViewVisibility final int tryTextVisibility,
                              final boolean animateText) {
            this.question = question;
            this.color = color;
            this.tryTextVisibility = tryTextVisibility;
            this.animateText = animateText;
        }
    }


    public static class SenseImageViewModel {
        @DrawableRes
        public static final int SRC_CIRCLE_DRAWABLE = R.drawable.sense_voice_circle_selector;
        static final int[] WAKE_STATE = new int[]{android.R.attr.state_first};
        static final int[] FAIL_STATE = new int[]{android.R.attr.state_middle};
        static final int[] OK_STATE = new int[]{android.R.attr.state_last};
        static final int[] WAIT_STATE = new int[]{};
        public final int repeatCount;
        public final int[] state;

        public static SenseImageViewModel getOnWaitModel() {
            return new SenseImageViewModel(0, WAIT_STATE);
        }

        public static SenseImageViewModel getOnWakeModel() {
            return new SenseImageViewModel(AnimatorSetHandler.LOOP_ANIMATION,
                                           WAKE_STATE);
        }

        SenseImageViewModel(final int repeatCount,
                            final int[] state) {
            this.repeatCount = repeatCount;
            this.state = state;
        }
    }
}
