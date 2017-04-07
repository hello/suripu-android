package is.hello.sense.ui.fragments.onboarding;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.Lazy;
import is.hello.commonsense.util.StringRef;
import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.VoiceResponse;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.SenseVoiceInteractor;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.ViewAnimator;
import is.hello.sense.ui.common.VoiceHelpDialogFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.fragments.BaseHardwareFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.AnimatorSetHandler;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.Subscriptions;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class SenseVoiceFragment extends BaseHardwareFragment {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ERROR_STATE, NOT_DETECTED_STATE, SUCCESS_STATE, SUCCESS_AFTER_ERROR_STATE})
    @Target({ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
    public @interface TutorialViewState{}

    static final int ERROR_STATE = 0;
    static final int NOT_DETECTED_STATE = 1;
    static final int SUCCESS_STATE = 2;
    static final int SUCCESS_AFTER_ERROR_STATE = 3;

    static final int[] SENSE_WAKE_STATE = new int[]{android.R.attr.state_first};
    static final int[] SENSE_FAIL_STATE = new int[]{android.R.attr.state_middle};
    static final int[] SENSE_OK_STATE = new int[]{android.R.attr.state_last};
    static final int[] SENSE_WAIT_STATE = new int[]{};

    public static class UIHandler extends Handler {
        final WeakReference<SenseVoiceFragment> fragmentRef;

        UIHandler(@NonNull final SenseVoiceFragment fragment) {
            super(Looper.getMainLooper());
            this.fragmentRef = new WeakReference<>(fragment);
        }

        /**
         * @param msg with callbacks will always be executed with {@link is.hello.sense.util.StateSafeExecutor}
         *            provided by fragment to ensure work is paused when appropriate.
         */
        @Override
        public void dispatchMessage(@NonNull final Message msg) {
            final SenseVoiceFragment fragment = Functions.extract(fragmentRef);
            if (fragment == null || fragment.isRemoving()) {
                removeCallbacksAndMessages(null);
                return;
            }

            final Runnable callback = msg.getCallback();
            if(callback != null) {
                fragment.stateSafeExecutor.execute(callback);
            } else {
                super.dispatchMessage(msg);
            }
        }
    }

    @Inject
    SenseVoiceInteractor senseVoiceInteractor;

    private static final int MAX_ALPHA = 20;
    private static final int VOICE_FAIL_COUNT_THRESHOLD = 2;

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
    private final Lazy<Integer> TRANSLATE_Y =
            () -> getResources().getDimensionPixelSize(R.dimen.sense_voice_translate_y);
    private final float SENSE_SCALE_FACTOR = 0.6f;

    private final ViewAnimator viewAnimator = new ViewAnimator(LoadingDialogFragment.DURATION_DEFAULT,
                                                               new AccelerateDecelerateInterpolator());
    @NonNull
    private Subscription voiceTipSubscription = Subscriptions.empty();
    @NonNull
    private Subscription requestDelayedSubscription = Subscriptions.empty();

    private final Runnable animateToNormalStateRunnable = SenseVoiceFragment.this::animateToNormalState;

    private final UIHandler uiHandler = new UIHandler(this);

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPresenter(senseVoiceInteractor);
        if(savedInstanceState == null){
            Analytics.trackEvent(Analytics.Onboarding.EVENT_VOICE_TUTORIAL, null);
        }
    }

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
        final ViewGroup questionTextGroup = (ViewGroup) animatedViewGroup.findViewById(R.id.fragment_sense_voice_question_container);

        Views.setTimeOffsetOnClickListener(retryButton,this::onRetry);
        Views.setTimeOffsetOnClickListener(skipButton,this::onSkip);
        toolbar = OnboardingToolbar.of(this, view)
                                   .setOnHelpClickListener(ignored -> showVoiceTipDialog(true, this::onVoiceTipDismissed))
                                   .setWantsHelpButton(false)
                                   .setWantsBackButton(false);

        requestCreateViewLayoutChanges(questionTextGroup);

        return view;
    }

    private void requestCreateViewLayoutChanges(final ViewGroup questionTextGroup) {
        senseImageView.setScaleX(SENSE_SCALE_FACTOR);
        senseImageView.setScaleY(SENSE_SCALE_FACTOR);

        Views.runWhenLaidOut(senseCircleView, () -> {
            //move circle view to center after sense is translated
            senseCircleView.setY(
                    senseImageView.getY() + TRANSLATE_Y.get() * SENSE_SCALE_FACTOR - ((senseCircleView.getHeight() - senseImageView.getHeight()) / 2)
                                );
            senseCircleView.invalidate();
            questionTextGroup.setY( senseCircleView.getY()
                                            - questionTextGroup.getMeasuredHeight()
                                            - getResources().getDimensionPixelSize(R.dimen.sense_voice_fixed_margin));
            questionTextGroup.invalidate();
        });

        retryButton.setText(R.string.action_continue);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(senseCircleView.getDrawable() != null) {
            viewAnimator.onViewCreated(
                    createAnimatorSetFor(senseCircleView.getDrawable()));
        }

        bindAndSubscribe(senseVoiceInteractor.voiceResponse,
                         this::handleVoiceResponse,
                         this::presentError);
    }

    @Override
    public void onPause() {
        super.onPause();
        viewAnimator.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(viewAnimator.canResume()) {
            viewAnimator.onResume();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewAnimator.onDestroyView();
        if(toolbar != null) {
            toolbar.onDestroyView();
        }
        if(retryButton != null) {
            retryButton.setOnClickListener(null);
        }
        if(skipButton != null) {
            skipButton.setOnClickListener(null);
        }
        voiceTipSubscription.unsubscribe();
        voiceTipSubscription = Subscriptions.empty();
        showVoiceTipDialog(false, null);
        requestDelayedSubscription.unsubscribe();
        requestDelayedSubscription = Subscriptions.empty();

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
                        animateToNormalState();
                        getAnimatorContext().runWhenIdle(() -> poll(true));
                    }
                });
    }

    private void onContinue(@NonNull final AnimatorContext.Transaction transaction) {

        transaction.animatorFor(nightStandView)
                .alpha(0.4f)
                .translationY(TRANSLATE_Y.get());

        transaction.animatorFor(senseImageView)
                .scale(1)
                .translationY(TRANSLATE_Y.get() * SENSE_SCALE_FACTOR);

        transaction.animatorFor(title)
                .fadeOut(View.INVISIBLE);

        transaction.animatorFor(subtitle)
                .fadeOut(View.INVISIBLE);

        senseCircleView.setImageResource(R.drawable.sense_voice_circle_selector);
        senseCircleView.getDrawable().setAlpha(0);
        questionText.setAlpha(0);
    }

    private void onSkip(final View view) {
        Analytics.trackEvent(Analytics.Onboarding.EVENT_VOICE_TUTORIAL_SKIP, null);
        onFinish(false);
    }

    private void onFinish(final boolean success) {
        voiceTipSubscription.unsubscribe();
        requestDelayedSubscription.unsubscribe();
        senseVoiceInteractor.reset();
        senseVoiceInteractor.updateHasCompletedTutorial(success);
        toolbar.setWantsHelpButton(false);
        retryButton.setEnabled(false);
        skipButton.setEnabled(false);
        skipButton.setVisibility(View.INVISIBLE);
        this.postDelayed(() -> finishFlowWithResult(success ? Activity.RESULT_OK : Activity.RESULT_CANCELED),
                         success ? LoadingDialogFragment.DURATION_DEFAULT * 3 : 0);
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
        final ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.PresenterBuilder(throwable)
                .withMessage(StringRef.from(R.string.error_internet_connection_generic_message))
                .build();

        if(ApiException.isNetworkError(throwable)){
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        }
        updateState(ERROR_STATE);
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
                voiceTipSubscription.unsubscribe();
                voiceTipSubscription = bottomSheet.subject.subscribe(isShownAction,
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
            senseVoiceInteractor.voiceResponse.forget();
        }
    }

    private void requestDelayed() {
        //request after a delay respecting fragment lifecycle
        requestDelayedSubscription.unsubscribe();
        requestDelayedSubscription = bind(Observable.interval(SenseVoiceInteractor.UPDATE_DELAY_SECONDS, TimeUnit.SECONDS))
                                           .subscribe(ignored -> senseVoiceInteractor.update(),
                                                      this::presentError);
    }

    private void handleVoiceResponse(@NonNull final VoiceResponse voiceResponse) {
        sendAnalyticsEvent(voiceResponse);

        getAnimatorContext().runWhenIdle(() -> {
            this.uiHandler.removeCallbacks(animateToNormalStateRunnable);

            if(SenseVoiceInteractor.hasSuccessful(voiceResponse)){
                animateToWaitState();
                if(Arrays.equals(SENSE_FAIL_STATE, senseImageView.getDrawableState())) {
                    updateState(SUCCESS_AFTER_ERROR_STATE);
                } else {
                    updateState(SUCCESS_STATE);
                }
                onFinish(true);
            } else {
                updateState(NOT_DETECTED_STATE);
                if (senseVoiceInteractor.getFailCount() == VOICE_FAIL_COUNT_THRESHOLD) {
                    this.postDelayed(() -> showVoiceTipDialog(true, this::onVoiceTipDismissed),
                                     LoadingDialogFragment.DURATION_DEFAULT*2);
                }
                //return to normal state
                this.postDelayed(animateToNormalStateRunnable,
                                 LoadingDialogFragment.DURATION_DEFAULT * 3);
            }
        });

    }

    private void onVoiceTipDismissed(final boolean isDismissed) {
        this.poll(isDismissed);
        stateSafeExecutor.setCanExecute(isDismissed);
        if(isDismissed) {
            stateSafeExecutor.executePendingForResume();
            if(viewAnimator.canResume()) {
                viewAnimator.onResume();
            }
        } else {
            viewAnimator.onPause();
        }
    }

    private void updateState(@SenseVoiceFragment.TutorialViewState
                             final int state) {
        final int[] senseImageState;
        final int senseRepeatCount;

        final boolean animateText;
        final int tryTextVisibility;
        final int question;
        final int color;

        switch (state) {
            case SUCCESS_STATE:
                senseImageState = SENSE_OK_STATE;
                senseRepeatCount = AnimatorSetHandler.SINGLE_ANIMATION;
                tryTextVisibility = View.VISIBLE;
                question = R.string.sense_voice_question_temperature;
                color = R.color.primary;
                animateText = false;
                break;
            case SUCCESS_AFTER_ERROR_STATE:
                senseImageState = SENSE_OK_STATE;
                senseRepeatCount = AnimatorSetHandler.SINGLE_ANIMATION;
                tryTextVisibility = View.VISIBLE;
                question = R.string.sense_voice_question_temperature;
                color = R.color.primary;
                animateText = true;
                break;
            case ERROR_STATE:
                senseImageState = SENSE_FAIL_STATE;
                senseRepeatCount = AnimatorSetHandler.LOOP_ANIMATION;
                tryTextVisibility = View.GONE;
                question = R.string.error_sense_voice_problem;
                color = R.color.primary_text;
                animateText = true;
                break;
            case NOT_DETECTED_STATE:
                senseImageState = SENSE_FAIL_STATE;
                senseRepeatCount = AnimatorSetHandler.SINGLE_ANIMATION;
                tryTextVisibility = View.INVISIBLE;
                question = R.string.error_sense_voice_not_detected;
                color = R.color.primary_text;
                animateText = true;
                break;
            default:
                throw new IllegalStateException("unhandled voice tutorial state" + state);
        }

        this.post(() -> setSenseCircleViewState(senseImageState,
                                                senseRepeatCount));
        this.postDelayed(() -> {
            setQuestionState(animateText,
                             tryTextVisibility,
                             question,
                             color);
            setSenseImageViewState(senseImageState);
        }, LoadingDialogFragment.DURATION_DEFAULT);
    }

    private void animateToNormalState(){
        animateToWaitState();
        animateToWakeState();
    }

    private void animateToWaitState(){
        this.post(() -> {
            setSenseCircleViewState(SENSE_WAIT_STATE,
                                    AnimatorSetHandler.SINGLE_ANIMATION);
            setSenseImageViewState(SENSE_WAIT_STATE);
        });
    }

    private void animateToWakeState(){
        this.post(() -> setQuestionState(true,
                                         View.VISIBLE,
                                         R.string.sense_voice_wake_phrase,
                                         R.color.primary_text));

        this.postDelayed(() -> setSenseCircleViewState(SENSE_WAKE_STATE,
                                                       AnimatorSetHandler.LOOP_ANIMATION),
                         LoadingDialogFragment.DURATION_DEFAULT);

        this.postDelayed(() -> {
            setSenseImageViewState(SENSE_WAKE_STATE);
            setQuestionState(false,
                             View.VISIBLE,
                             R.string.sense_voice_question_temperature,
                             R.color.primary_text);
        }, LoadingDialogFragment.DURATION_DEFAULT*2);
    }

    /**
     * Important to wrap onCompletion lambda with stateSafeExecutor to prevent execution if fragment view is gone/destroyed
     * Anytime an operation is delayed, it is not guaranteed to have access to same views when attempt to execute.
     */
    private void setQuestionState(final boolean animateText,
                                  final int tryTextVisibility,
                                  final int question,
                                  final int color){
        if (animateText){
            animatorFor(questionText, animatorContext)
                    .translationY(TRANSLATE_Y.get())
                    .fadeOut(View.INVISIBLE)
                    .addOnAnimationCompleted(complete -> {
                        if (complete) {
                            this.post(() -> {
                                tryText.setVisibility(tryTextVisibility);
                                questionText.setText(question);
                                questionText.setTextColor(ContextCompat.getColor(questionText.getContext(),
                                                                                 color));
                                animatorFor(questionText, animatorContext)
                                        .translationY(0)
                                        .fadeIn()
                                        .start();
                            });
                        }
                    }).start();
        } else {
            this.post(() -> {
                tryText.setVisibility(tryTextVisibility);
                questionText.setText(question);
                questionText.setTextColor(ContextCompat.getColor(questionText.getContext(), color));
            });
        }
    }

    private void setSenseImageViewState(final int[] state){
        senseImageView.setImageState(state, false);
    }

    private void setSenseCircleViewState(@NonNull final int[] state,
                                         final int repeatCount){
        senseCircleView.setImageState(state, false);
        viewAnimator.setRepeatCount(repeatCount);
        viewAnimator.resetAnimation(
                createAnimatorSetFor(senseCircleView.getDrawable()));
    }

    private AnimatorSet createAnimatorSetFor(@NonNull final Drawable drawable) {
        final AnimatorSet animSet = new AnimatorSet();
        animSet.setStartDelay(200);

        if(drawable.getCurrent() instanceof LayerDrawable) {
            final LayerDrawable layerDrawable = (LayerDrawable) drawable.getCurrent();
            final Drawable outerCircle = layerDrawable.getDrawable(0);
            final Drawable middleCircle = layerDrawable.getDrawable(1);
            final Drawable innerCircle = layerDrawable.getDrawable(2);

            animSet.playSequentially(
                    ObjectAnimator.ofInt(innerCircle, "alpha", 0, MAX_ALPHA).setDuration(200),
                    ObjectAnimator.ofInt(middleCircle, "alpha", 0, MAX_ALPHA).setDuration(200),
                    ObjectAnimator.ofInt(outerCircle, "alpha", 0, MAX_ALPHA).setDuration(200),
                    ObjectAnimator.ofInt(layerDrawable, "alpha", MAX_ALPHA, 0).setDuration(600));
        }

        return animSet;
    }

    private void sendAnalyticsEvent(@Nullable final VoiceResponse voiceResponse){
        if(voiceResponse == null){
            return;
        }
        Analytics.trackEvent(Analytics.Onboarding.EVENT_VOICE_COMMAND,
                             Analytics.createProperties(Analytics.Onboarding.PROP_VOICE_COMMAND_STATUS,
                                                        voiceResponse.result));
    }

    private boolean post(@NonNull final Runnable runnable) {
        return this.uiHandler.post(runnable);
    }

    /**
     * @return true if successfully queued
     */
    private boolean postDelayed(@NonNull final Runnable runnable, final long delay) {
        return this.uiHandler.postDelayed(runnable, delay);
    }
}
