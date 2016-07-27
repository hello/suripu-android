package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import is.hello.go99.animators.MultiAnimator;
import is.hello.go99.animators.OnAnimationCompleted;
import is.hello.sense.R;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.ViewAnimator;
import is.hello.sense.ui.widget.util.Views;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class SenseVoiceFragment extends InjectionFragment {

    private static final int TRANSLATE_Y = 200;
    private OnboardingToolbar toolbar;
    private TextView title;
    private TextView subtitle;
    private TextView tryText;
    private TextView questionText;
    private Button retryButton;
    private Button skipButton;
    private ViewGroup animatedViewGroup;
    private final ViewAnimator viewAnimator = new ViewAnimator();

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_sense_voice_layout, container, false);
        title = (TextView) view.findViewById(R.id.fragment_sense_voice_title);
        subtitle = (TextView) view.findViewById(R.id.fragment_sense_voice_subtitle);
        tryText = (TextView) view.findViewById(R.id.fragment_sense_voice_try);
        questionText = (TextView) view.findViewById(R.id.fragment_sense_voice_question);
        skipButton = (Button) view.findViewById(R.id.fragment_sense_voice_skip);
        retryButton = (Button) view.findViewById(R.id.fragment_sense_voice_retry);
        animatedViewGroup = (ViewGroup) view.findViewById(R.id.sense_voice_container);
        viewAnimator.setAnimatedView(view.findViewById(R.id.animated_circles_view));

        Views.setTimeOffsetOnClickListener(retryButton,this::onRetry);
        Views.setTimeOffsetOnClickListener(skipButton,this::onSkip);
        toolbar = OnboardingToolbar.of(this, view)
                                   .setWantsHelpButton(true)
                                   .setWantsBackButton(false);

        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewAnimator.onViewCreated(getActivity(), R.animator.sense_voice_circles_animator);
    }

    @Override
    public void onPause() {
        super.onPause();
        viewAnimator.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        viewAnimator.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewAnimator.onDestroyView();
        if(toolbar != null){
            toolbar.onDestroyView();
            toolbar = null;
        }
        title = null;
        subtitle = null;
        tryText = null;
        questionText = null;
        retryButton.setOnClickListener(null);
        retryButton = null;
        skipButton.setOnClickListener(null);
        skipButton = null;
        animatedViewGroup = null;
    }

    private void onRetry(final View view) {
        final OnAnimationCompleted showQuestionTextAnimation =
                isFinished -> {
                    if(isFinished){
                        animatorFor(questionText)
                                .translationY(-TRANSLATE_Y)
                                .fadeIn()
                                .start();
                    }
                };

        animateUI(animatedViewGroup, 0, TRANSLATE_Y, 1, 1, showQuestionTextAnimation);
        animateUI(skipButton, 0, TRANSLATE_Y, 1, 1, null);
        animateUI(retryButton, 0, TRANSLATE_Y, 1, 0, null);
        updateUI(false);
    }

    //todo proper activity closure
    private void onSkip(final View view) {
        getActivity().finish();
    }

    private void updateUI(final boolean onError){
        if(onError) {
            toolbar.setWantsHelpButton(true);
        } else {
            title.setVisibility(View.INVISIBLE);
            subtitle.setVisibility(View.INVISIBLE);
            toolbar.setWantsHelpButton(false);
        }
    }

    private void animateUI(final View view,
                           final int startY,
                           final int endY,
                           final int startAlpha,
                           final int endAlpha,
                           @Nullable final OnAnimationCompleted onAnimationCompleted){
        final MultiAnimator multiAnimator = animatorFor(view)
                .slideYAndFade(startY, endY, startAlpha, endAlpha)
                .fadeIn();
        if(onAnimationCompleted != null){
            multiAnimator.addOnAnimationCompleted(onAnimationCompleted);
        }
        multiAnimator.start();
    }
}
