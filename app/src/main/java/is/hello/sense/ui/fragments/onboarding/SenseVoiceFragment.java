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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.Lazy;
import is.hello.commonsense.util.StringRef;
import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.VoiceResponse;
import is.hello.sense.interactors.SenseVoiceInteractor;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.ViewAnimator;
import is.hello.sense.ui.common.VoiceHelpDialogFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.fragments.ScopedInjectionFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.AnimatorSetHandler;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.Subscriptions;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class SenseVoiceFragment extends ScopedInjectionFragment {

    @Inject
    SenseVoiceInteractor senseVoicePresenter;

    private static final int MAX_ALPHA = 20;
    private static final int VOICE_FAIL_COUNT_THRESHOLD = 2;
    private static final int[] FAIL_STATE = new int[]{android.R.attr.state_middle};
    private static final int[] OK_STATE = new int[]{android.R.attr.state_last};
    private static final int[] WAIT_STATE = new int[]{};

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
    private Lazy<Integer> TRANSLATE_Y =
            () -> getResources().getDimensionPixelSize(R.dimen.sense_voice_translate_y);
    private final ViewAnimator viewAnimator = new ViewAnimator(LoadingDialogFragment.DURATION_DEFAULT,
                                                               new AccelerateDecelerateInterpolator());

    private Subscription voiceTipSubscription;
    private Subscription requestDelayedSubscription;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPresenter(senseVoicePresenter);

        if(savedInstanceState == null){
            Analytics.trackEvent(Analytics.Onboarding.EVENT_VOICE_TUTORIAL, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        voiceTipSubscription = Subscriptions.empty();
        requestDelayedSubscription = Subscriptions.empty();

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
        final ViewGroup questionTextGroup = (ViewGroup) animatedViewGroup.findViewById(R.id.fragment_sense_voice_question_container);

        Views.setTimeOffsetOnClickListener(retryButton,this::onRetry);
        Views.setTimeOffsetOnClickListener(skipButton,this::onSkip);
        toolbar = OnboardingToolbar.of(this, view)
                                   .setOnHelpClickListener(ignored -> showVoiceTipDialog(true, this::poll))
                                   .setHelpButtonIcon(R.drawable.info_button_icon_small)
                                   .setWantsHelpButton(false)
                                   .setWantsBackButton(false);

        requestCreateViewLayoutChanges(questionTextGroup);

        return view;
    }

    private void requestCreateViewLayoutChanges(final ViewGroup questionTextGroup) {
        senseImageView.setScaleX(0.6f);
        senseImageView.setScaleY(0.6f);

        senseCircleView.post(stateSafeExecutor.bind( () -> {
            //move circle view to center after sense is translated
            senseCircleView.setY(
                    senseImageView.getY() + TRANSLATE_Y.get()*0.6f - ((senseCircleView.getHeight() - senseImageView.getHeight()) / 2)
                                );
            senseCircleView.invalidate();
            questionTextGroup.setY( senseCircleView.getY() - questionTextGroup.getMeasuredHeight() -
                                            getResources().getDimensionPixelSize(R.dimen.sense_voice_fixed_margin));
            questionTextGroup.invalidate();
        }));

        retryButton.setText(R.string.action_continue);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(senseCircleView.getDrawable() != null) {
            viewAnimator.onViewCreated(
                    createAnimatorSetFor((StateListDrawable) senseCircleView.getDrawable()));
        }

        bindAndSubscribe(senseVoicePresenter.voiceResponse,
                         this::handleVoiceResponse,
                         this::presentError);
    }

    @Override
    public void onPause() {
        super.onPause();
        viewAnimator.onPause();
        //cancelAll(questionText);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(senseCircleView.getDrawable() != null) {
            viewAnimator.onResume();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewAnimator.onDestroyView();
        toolbar.onDestroyView();
        toolbar = null;
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
        nightStandView = null;
        voiceTipSubscription.unsubscribe();
        voiceTipSubscription = null;
        showVoiceTipDialog(false, null);
        requestDelayedSubscription.unsubscribe();
        requestDelayedSubscription = null;
        TRANSLATE_Y = null;
    }

    private void onRetry(final View view){
        getAnimatorContext().transaction(
                transaction -> {
                    if(senseCircleView.getDrawable() == null) {
                        onContinue(transaction);
                    }
                    updateButtons(false, transaction);
                }, complete -> {
                    if(complete) {
                        poll(true);
                        updateState(R.string.sense_voice_question_temperature,
                                    R.color.text_dark,
                                    View.VISIBLE,
                                    WAIT_STATE,
                                    AnimatorSetHandler.LOOP_ANIMATION,
                                    true);
                    }
                });
    }

    private void onContinue(@NonNull final AnimatorContext.Transaction transaction) {

        transaction.animatorFor(nightStandView)
                .alpha(0.4f)
                .translationY(TRANSLATE_Y.get());

        transaction.animatorFor(senseImageView)
                .scale(1)
                .translationY(TRANSLATE_Y.get()*0.60f);

        transaction.animatorFor(title)
                .fadeOut(View.INVISIBLE);

        transaction.animatorFor(subtitle)
                .fadeOut(View.INVISIBLE);

        senseCircleView.setImageResource(R.drawable.sense_voice_circle_selector);
        senseCircleView.getDrawable().setAlpha(0);
        questionText.setAlpha(0);
    }

    private void onSkip(final View view) {
        onFinish(false);
    }

    private void onFinish(final boolean success){
        voiceTipSubscription.unsubscribe();
        requestDelayedSubscription.unsubscribe();
        senseVoicePresenter.reset();
        toolbar.setWantsHelpButton(false);
        retryButton.setEnabled(false);
        skipButton.setEnabled(false);
        skipButton.setVisibility(View.INVISIBLE);
        senseVoicePresenter.updateHasCompletedTutorial(success);
        bindAndSubscribe(Observable.timer(success ? LoadingDialogFragment.DURATION_DEFAULT * 3 : 0, TimeUnit.MILLISECONDS),
                ignored -> finishFlowWithResult(success ? Activity.RESULT_OK : Activity.RESULT_CANCELED),
                this::presentError);
    }

    private void updateButtons(final boolean onError, @NonNull final AnimatorContext.Transaction transaction){
        final float buttonTranslateY = retryButton.getMeasuredHeight();
        toolbar.setWantsHelpButton(!onError);
        if(onError) {
            transaction.animatorFor(skipButton)
                       .slideYAndFade(0, -buttonTranslateY, 1, 1);

            transaction.animatorFor(retryButton)
                       .slideYAndFade(0, -buttonTranslateY, 0, 1);
        } else {
            transaction.animatorFor(retryButton)
                       .translationY(buttonTranslateY)
                       .fadeOut(View.INVISIBLE);

            transaction.animatorFor(skipButton)
                       .translationY(buttonTranslateY);
        }
    }

    private void presentError(final Throwable throwable) {
        poll(false);
        voiceTipSubscription.unsubscribe();
        final ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(throwable, getActivity())
                .withMessage(StringRef.from(R.string.error_internet_connection_generic_message))
                .build();

        if(ApiException.isNetworkError(throwable)){
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        }
        updateState(R.string.error_sense_voice_problem,
                    R.color.text_dark,
                    View.GONE,
                    FAIL_STATE,
                    AnimatorSetHandler.LOOP_ANIMATION,
                    true);
        getAnimatorContext().transaction(
                transaction -> updateButtons(true,transaction), null);
    }

    private void showVoiceTipDialog(final boolean shouldShow,
                                    @Nullable final Action1<Boolean> isShownAction) {
        VoiceHelpDialogFragment bottomSheet =
                (VoiceHelpDialogFragment) getFragmentManager()
                        .findFragmentByTag(VoiceHelpDialogFragment.TAG);
        if (bottomSheet != null && !shouldShow) {
            bottomSheet.dismissSafely();
        } else if(bottomSheet == null && shouldShow){
            bottomSheet = VoiceHelpDialogFragment.newInstance();

            if(isShownAction != null) {
                voiceTipSubscription = bottomSheet.subject
                        .subscribe(isShownAction,
                                   this::presentError);
            }

            bottomSheet.showAllowingStateLoss(getFragmentManager(), VoiceHelpDialogFragment.TAG);
        }
    }

    private void poll(final boolean start){
        Log.d(SenseVoiceFragment.class.getName(), "poll: " + start);
        if(start) {
            requestDelayed();
        } else{
            requestDelayedSubscription.unsubscribe();
            senseVoicePresenter.voiceResponse.forget();
        }
    }

    private void requestDelayed() {
        //retry once again after a delay respecting fragment lifecycle
        requestDelayedSubscription = bind(Observable.timer(SenseVoiceInteractor.UPDATE_DELAY_SECONDS, TimeUnit.SECONDS))
                                           .subscribe(ignored -> senseVoicePresenter.update(),
                                                      this::presentError);
    }

    private void handleVoiceResponse(@Nullable final VoiceResponse voiceResponse) {
        sendAnalyticsEvent(voiceResponse);

        if(SenseVoiceInteractor.hasSuccessful(voiceResponse)){
            updateState(R.string.sense_voice_question_temperature,
                        R.color.primary,
                        View.GONE,
                        OK_STATE,
                        0,
                        Arrays.equals(senseImageView.getDrawableState(), FAIL_STATE));
            onFinish(true);
        } else{
            @StringRes final int errorText = voiceResponse == null ?
                    R.string.error_sense_voice_problem :
                    R.string.error_sense_voice_not_detected;

            updateState(errorText,
                        R.color.text_dark,
                        View.GONE,
                        FAIL_STATE,
                        0,
                        true);
            if(senseVoicePresenter.getFailCount() == VOICE_FAIL_COUNT_THRESHOLD){
                showVoiceTipDialog(true, this::poll);
            }
            //return to normal wait state
            questionText.postOnAnimationDelayed(stateSafeExecutor.bind(() ->
                updateState(R.string.sense_voice_question_temperature,
                            R.color.text_dark,
                            View.VISIBLE,
                            WAIT_STATE,
                            AnimatorSetHandler.LOOP_ANIMATION,
                            true)
            ), LoadingDialogFragment.DURATION_DEFAULT * 3);

            requestDelayed();
        }
    }

    private void updateState(@StringRes final int stringRes,
                             @ColorRes final int textColorRes,
                             final int tryVisibility,
                             final int[] imageState,
                             final int repeatCount,
                             final boolean animateText){

        if (animateText){

            animatorFor(questionText)
                    .withStartDelay(LoadingDialogFragment.DURATION_DEFAULT)
                    .translationY(TRANSLATE_Y.get())
                    .fadeOut(View.INVISIBLE)
                    .addOnAnimationWillStart(willStart -> {
                        stateSafeExecutor.execute(() -> {
                            senseImageView.setImageState(imageState, false);
                        });
                    })
                    .addOnAnimationCompleted(complete -> {
                        if (complete) {
                            stateSafeExecutor.execute(() -> {
                                tryText.setVisibility(tryVisibility);
                                questionText.setText(stringRes);
                                questionText.setTextColor(ContextCompat.getColor(questionText.getContext(), textColorRes));
                                animatorFor(questionText)
                                        .translationY(0)
                                        .fadeIn()
                                        .start();
                            });
                        }
                    }).start();

        } else {
            senseImageView.postDelayed(stateSafeExecutor.bind( () -> {
                senseImageView.setImageState(imageState, false);
                tryText.setVisibility(tryVisibility);
                questionText.setText(stringRes);
                questionText.setTextColor(ContextCompat.getColor(questionText.getContext(), textColorRes));
            }), LoadingDialogFragment.DURATION_DEFAULT);
        }

        senseCircleView.setImageState(imageState, false);
        viewAnimator.setRepeatCount(repeatCount);
        viewAnimator.resetAnimation(
                createAnimatorSetFor((StateListDrawable) senseCircleView.getDrawable()));
    }

    private AnimatorSet createAnimatorSetFor(@NonNull final StateListDrawable stateListDrawable) {
        final LayerDrawable layerDrawable = (LayerDrawable) stateListDrawable.getCurrent();
        final Drawable outerCircle = layerDrawable.getDrawable(0);
        final Drawable middleCircle = layerDrawable.getDrawable(1);
        final Drawable innerCircle = layerDrawable.getDrawable(2);
        final AnimatorSet animSet = new AnimatorSet();
        animSet.playSequentially(
                ObjectAnimator.ofInt(innerCircle, "alpha", 0, MAX_ALPHA).setDuration(200),
                ObjectAnimator.ofInt(middleCircle, "alpha", 0, MAX_ALPHA).setDuration(200),
                ObjectAnimator.ofInt(outerCircle, "alpha", 0, MAX_ALPHA).setDuration(200),
                ObjectAnimator.ofInt(layerDrawable, "alpha", MAX_ALPHA, 0).setDuration(600));

        animSet.setStartDelay(200);

        return animSet;
    }

    private void sendAnalyticsEvent(@NonNull final VoiceResponse voiceResponse){
        Analytics.trackEvent(Analytics.Onboarding.EVENT_VOICE_COMMAND,
                             Analytics.createProperties(Analytics.Onboarding.PROP_VOICE_COMMAND_STATUS,
                                                        voiceResponse.result));
    }
}
