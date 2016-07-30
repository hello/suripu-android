package is.hello.sense.ui.fragments.onboarding;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.go99.animators.MultiAnimator;
import is.hello.go99.animators.OnAnimationCompleted;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.ViewAnimator;
import is.hello.sense.ui.common.VoiceHelpDialogFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.util.Views;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class SenseVoiceFragment extends InjectionFragment {

    @Inject
    ApiService apiService;

    private static final int TRANSLATE_Y = 120;
    private OnboardingToolbar toolbar;
    private TextView title;
    private TextView subtitle;
    private TextView tryText;
    private TextView questionText;
    private Button retryButton;
    private Button skipButton;
    private ViewGroup animatedViewGroup;
    private ImageView senseImageView;
    private final ViewAnimator viewAnimator = new ViewAnimator();
    private View nightStandView;
    private ImageView senseCircleView;

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_sense_voice, container, false);
        title = (TextView) view.findViewById(R.id.fragment_sense_voice_title);
        subtitle = (TextView) view.findViewById(R.id.fragment_sense_voice_subtitle);
        tryText = (TextView) view.findViewById(R.id.fragment_sense_voice_try_text);
        questionText = (TextView) view.findViewById(R.id.fragment_sense_voice_question_text);
        skipButton = (Button) view.findViewById(R.id.fragment_sense_voice_skip);
        retryButton = (Button) view.findViewById(R.id.fragment_sense_voice_retry);
        animatedViewGroup = (ViewGroup) view.findViewById(R.id.sense_voice_container);
        senseImageView = (ImageView) animatedViewGroup.findViewById(R.id.sense_voice_view);
        nightStandView = animatedViewGroup.findViewById(R.id.nightstand);
        senseCircleView = (ImageView) animatedViewGroup.findViewById(R.id.animated_circles_view);
        viewAnimator.setAnimatedView(senseCircleView);

        Views.setTimeOffsetOnClickListener(retryButton,this::onRetry);
        Views.setTimeOffsetOnClickListener(skipButton,this::onSkip);
        toolbar = OnboardingToolbar.of(this, view)
                                   .setOnHelpClickListener(ignored -> showVoiceTipDialog(true))
                                   .setWantsHelpButton(true)
                                   .setWantsBackButton(false);

        //todo make into presenter when more stable
        bindAndSubscribe(apiService.getOnboardingVoiceResponse(),
                         voiceResponses -> {
                             Log.d(getClass().getName(), "onCreateView: " + voiceResponses);
                         },
                         this::presentError);

        senseImageView.setScaleX(0.6f);
        senseImageView.setScaleY(0.6f);

        retryButton.setText(R.string.action_continue);

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
        senseCircleView = null;
        senseImageView = null;
        showVoiceTipDialog(false);
    }

    private void onRetry(final View view) {
        final OnAnimationCompleted showQuestionTextAnimation =
                isFinished -> {
                    if(isFinished){
                        animatorFor(questionText)
                                .addOnAnimationWillStart( multiAnimator -> {
                                    tryText.setTranslationY(-TRANSLATE_Y*2);
                                    questionText.setTranslationY(TRANSLATE_Y/2);
                                })
                                .addOnAnimationCompleted( complete -> {
                                    if(complete){
                                        tryText.setVisibility(View.VISIBLE);
                                        final Drawable circleDrawable = senseCircleView.getDrawable().mutate();
                                        circleDrawable.setLevel(1);
                                        circleDrawable.invalidateSelf();
                                    }
                                })
                                .translationY(-TRANSLATE_Y/2)
                                .fadeIn()
                                .start();
                    }
                };
        animatorFor(nightStandView)
                .alpha(0.4f)
                .translationY(TRANSLATE_Y)
                .start();

        animatorFor(senseImageView)
                .scale(1)
                .translationY(TRANSLATE_Y)
                .addOnAnimationCompleted(showQuestionTextAnimation)
                .start();

        animatorFor(senseCircleView)
                .fadeIn()
                .translationY(TRANSLATE_Y)
                .start();

        animateUI(skipButton, 0, TRANSLATE_Y, 1, 1);
        animateUI(retryButton, 0, TRANSLATE_Y, 1, 0);
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
                           final int endAlpha){
        final MultiAnimator multiAnimator = animatorFor(view)
                .slideYAndFade(startY, endY, startAlpha, endAlpha)
                .fadeIn();
        multiAnimator.start();
    }

    private void presentError(final Throwable throwable) {
        ErrorDialogFragment.presentError(getActivity(), throwable);
    }

    private void showVoiceTipDialog(final boolean shouldShow) {
        VoiceHelpDialogFragment bottomSheet = (VoiceHelpDialogFragment) getFragmentManager().findFragmentByTag(VoiceHelpDialogFragment.TAG);
        if (bottomSheet != null && !shouldShow) {
            bottomSheet.dismissSafely();
        } else if(bottomSheet == null && shouldShow){
            bottomSheet = VoiceHelpDialogFragment.newInstance();
            //bottomSheet.setTargetFragment(SenseVoiceFragment.this, OPTION_FACEBOOK_DESCRIPTION);
            bottomSheet.showAllowingStateLoss(getFragmentManager(), VoiceHelpDialogFragment.TAG);
        }
    }
}
