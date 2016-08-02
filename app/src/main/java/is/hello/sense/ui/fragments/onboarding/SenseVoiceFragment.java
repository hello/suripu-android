package is.hello.sense.ui.fragments.onboarding;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.Lazy;
import is.hello.go99.animators.OnAnimationCompleted;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.VoiceResponse;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.ViewAnimator;
import is.hello.sense.ui.common.VoiceHelpDialogFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.util.Views;
import rx.Observable;
import rx.schedulers.Schedulers;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class SenseVoiceFragment extends InjectionFragment {

    private static final int INTERVAL_SECONDS = 10; //todo consult jimmy prishil for best poll time
    @Inject
    ApiService apiService;

    private static final int[] FAIL_STATE = new int[]{android.R.attr.state_middle};
    private static final int[] OK_STATE = new int[]{android.R.attr.state_last};
    private final Lazy<Integer> TRANSLATE_Y =
            () -> getResources().getDimensionPixelSize(R.dimen.sense_voice_translate_y);
    private OnboardingToolbar toolbar;
    private TextView title;
    private TextView subtitle;
    private TextView tryText;
    private TextView questionText;
    private Button retryButton;
    private Button skipButton;
    private ImageView senseImageView;
    private ImageView senseCircleView;
    private View nightStandView;
    private final ViewAnimator viewAnimator = new ViewAnimator(LoadingDialogFragment.DURATION_DEFAULT);

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
        final ViewGroup animatedViewGroup = (ViewGroup) view.findViewById(R.id.sense_voice_container);
        senseCircleView = (ImageView) animatedViewGroup.findViewById(R.id.animated_circles_view);
        senseImageView = (ImageView) animatedViewGroup.findViewById(R.id.sense_voice_view);
        nightStandView = animatedViewGroup.findViewById(R.id.nightstand);

        Views.setTimeOffsetOnClickListener(retryButton,this::onRetry);
        Views.setTimeOffsetOnClickListener(skipButton,this::onSkip);
        toolbar = OnboardingToolbar.of(this, view)
                                   .setOnHelpClickListener(ignored -> showVoiceTipDialog(true))
                                   .setWantsHelpButton(true)
                                   .setWantsBackButton(false);

        senseImageView.setScaleX(0.6f);
        senseImageView.setScaleY(0.6f);

        retryButton.setText(R.string.action_continue);

        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(senseCircleView.getDrawable() != null) {
            viewAnimator.onViewCreated(
                    createAnimatorSetFor((StateListDrawable) senseCircleView.getDrawable()));
        }
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
                                    tryText.setTranslationY(-TRANSLATE_Y.get()*2);
                                    questionText.setTranslationY(TRANSLATE_Y.get()/2);
                                })
                                .addOnAnimationCompleted( complete -> {
                                    if(complete){
                                        tryText.setVisibility(View.VISIBLE);
                                    }
                                })
                                .translationY(-TRANSLATE_Y.get()/2)
                                .fadeIn()
                                .start();
                    }
                };
        animatorFor(nightStandView)
                .alpha(0.4f)
                .translationY(TRANSLATE_Y.get())
                .start();

        animatorFor(senseImageView)
                .scale(1)
                .translationY(TRANSLATE_Y.get()/4)
                .addOnAnimationCompleted(showQuestionTextAnimation)
                .start();

        animatorFor(senseCircleView)
                .fadeIn()
                .translationY(TRANSLATE_Y.get()/4)
                .start();

        animatorFor(retryButton)
                .translationY(TRANSLATE_Y.get())
                .fadeOut(View.INVISIBLE)
                .start();

        animatorFor(skipButton)
                .translationY(TRANSLATE_Y.get()*1.25f)
                .fadeIn()
                .start();

        updateUI(false);
        poll(INTERVAL_SECONDS);
    }

    private void onSkip(final View view) {
        onFinish(false);
    }

    private void onFinish(final boolean success){
        retryButton.setEnabled(false);
        skipButton.setEnabled(false);
        questionText.postDelayed(
                () -> getFragmentNavigation()
                        .flowFinished(this, success ? Activity.RESULT_OK : Activity.RESULT_CANCELED, null),
                success ? LoadingDialogFragment.DURATION_DEFAULT * 3 : 0);
    }

    private void updateUI(final boolean onError){
        if(onError) {
            toolbar.setWantsHelpButton(true);
        } else {
            senseCircleView.setImageResource(R.drawable.sense_voice_circle_selector);
            senseCircleView.getDrawable().setAlpha(0);
            viewAnimator.resetAnimation(
                    createAnimatorSetFor((StateListDrawable) senseCircleView.getDrawable()));
            title.setVisibility(View.INVISIBLE);
            subtitle.setVisibility(View.INVISIBLE);
            toolbar.setWantsHelpButton(false);
        }
    }

    private void presentError(final Throwable throwable) {
        ErrorDialogFragment.presentError(getActivity(), throwable);
        updateUI(true);
    }

    private void showVoiceTipDialog(final boolean shouldShow) {
        VoiceHelpDialogFragment bottomSheet = (VoiceHelpDialogFragment) getFragmentManager().findFragmentByTag(VoiceHelpDialogFragment.TAG);
        if (bottomSheet != null && !shouldShow) {
            bottomSheet.dismissSafely();
        } else if(bottomSheet == null && shouldShow){
            bottomSheet = VoiceHelpDialogFragment.newInstance();
            bottomSheet.showAllowingStateLoss(getFragmentManager(), VoiceHelpDialogFragment.TAG);
        }
    }

    private void poll(final int pollInterval){
        //todo make into presenter when more stable
        bindAndSubscribe(Observable.interval(pollInterval, TimeUnit.SECONDS, Schedulers.io())
                         .flatMap( ignored -> apiService.getOnboardingVoiceResponse()),
                         this::handleVoiceResponse,
                         this::presentError);
    }

    private void handleVoiceResponse(@NonNull final ArrayList<VoiceResponse> voiceResponses) {
        if(voiceResponses.isEmpty()){
            return;
        }
        Collections.sort(voiceResponses,
                         (thisResponse, otherResponse) -> thisResponse.dateTime.compareTo(otherResponse.dateTime)) ;
        final VoiceResponse voiceResponse = voiceResponses.get(0);
        if(voiceResponse.result.equals(VoiceResponse.Result.OK)){
            updateState(R.string.sense_voice_question_temperature,
                        R.color.primary,
                        OK_STATE);
            onFinish(true);
        } else{
            updateState(R.string.error_sense_voice_not_detected,
                        R.color.text_dark,
                        FAIL_STATE);
        }
    }

    private void updateState(@StringRes final int stringRes,
                             @ColorRes final int textColorRes,
                             final int[] imageState){
        questionText.postDelayed(
                stateSafeExecutor.bind(()-> {
                    senseImageView.setImageState(imageState, false);
                    questionText.setText(stringRes);
                    questionText.setTextColor(ContextCompat.getColor(questionText.getContext(), textColorRes));
                }), LoadingDialogFragment.DURATION_DEFAULT);
        senseCircleView.setImageState(imageState, false);
        viewAnimator.resetAnimation(
                createAnimatorSetFor((StateListDrawable) senseCircleView.getDrawable()));
    }

    private AnimatorSet createAnimatorSetFor(@NonNull final StateListDrawable stateListDrawable) {
        final int MAX_ALPHA = 20;
        final LayerDrawable layerDrawable = (LayerDrawable) stateListDrawable.getCurrent();
        final Drawable outerCircle = layerDrawable.getDrawable(0);
        final Drawable middleCircle = layerDrawable.getDrawable(1);
        final Drawable innerCircle = layerDrawable.getDrawable(2);
        final AnimatorSet animSet = new AnimatorSet();
        animSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animSet.playSequentially(
                ObjectAnimator.ofInt(innerCircle, "alpha", 0, MAX_ALPHA).setDuration(200),
                ObjectAnimator.ofInt(middleCircle, "alpha", 0, MAX_ALPHA).setDuration(200),
                ObjectAnimator.ofInt(outerCircle, "alpha", 0, MAX_ALPHA).setDuration(200),
                ObjectAnimator.ofInt(layerDrawable, "alpha", MAX_ALPHA, 0).setDuration(600));

        animSet.setStartDelay(200);

        return animSet;
    }
}
