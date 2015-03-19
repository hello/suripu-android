package is.hello.sense.ui.handholding;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RelativeLayout;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.functional.Functions;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.animation.AnimatorContext;
import is.hello.sense.ui.animation.PropertyAnimatorProxy;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.Subscription;
import rx.android.operators.OperatorConditionalBinding;
import rx.functions.Action1;

@SuppressLint("ViewConstructor")
public class TutorialOverlayView extends RelativeLayout {
    private final Activity activity;
    private final Tutorial tutorial;
    private final TextView descriptionText;

    private @Nullable InteractionView interactionView;
    private View anchorView;
    private float interactionStartX = 0f, interactionStartY = 0f;
    private boolean trackingInteraction = false;
    private boolean dispatchedLastEvent = false;

    private @Nullable ViewGroup container;
    private @Nullable AnimatorContext animatorContext;

    //region Creation

    public TutorialOverlayView(@NonNull Activity activity, @NonNull Tutorial tutorial) {
        super(activity);

        this.activity = activity;
        this.tutorial = tutorial;

        LayoutInflater inflater = LayoutInflater.from(activity);
        this.descriptionText = (TextView) inflater.inflate(R.layout.sub_fragment_tutorial_description, this, false);
        descriptionText.setText(tutorial.descriptionRes);
        Views.setSafeOnClickListener(descriptionText, ignored -> interactionCompleted());

        Drawable dismissIcon = descriptionText.getCompoundDrawablesRelative()[2].mutate();
        dismissIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        descriptionText.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, dismissIcon, null);

        addView(descriptionText, tutorial.generateDescriptionLayoutParams());
    }

    //endregion


    //region Lifecycle

    private @Nullable Window getWindow() {
        return activity.getWindow();
    }

    private <T> Subscription bindAndSubscribe(@NonNull Observable<T> observable,
                                              @NonNull Action1<T> onNext,
                                              @NonNull Action1<Throwable> onError) {
        return observable.lift(new OperatorConditionalBinding<>(this, View::isAttachedToWindow))
                         .subscribe(onNext, onError);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        Window window = getWindow();
        if (window != null && !(window.getCallback() instanceof EventInterceptor)) {
            Logger.info(getClass().getSimpleName(), "Attaching interceptor");
            window.setCallback(new EventInterceptor(window.getCallback()));
        }

        if (interactionView == null) {
            this.anchorView = activity.findViewById(tutorial.anchorId);
            if (anchorView != null) {
                if (anchorView.isLayoutRequested()) {
                    bindAndSubscribe(Views.observeNextLayout(anchorView),
                                     ignored -> showInteractionFrom(),
                                     Functions.LOG_ERROR);
                } else {
                    showInteractionFrom();
                }
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        Window window = getWindow();
        if (window != null && (window.getCallback() instanceof EventInterceptor)) {
            Logger.info(getClass().getSimpleName(), "Detaching interceptor");
            EventInterceptor interceptor = (EventInterceptor) window.getCallback();
            window.setCallback(interceptor.getTarget());
        }

        this.anchorView = null;
    }

    //endregion


    //region Showing

    public void setAnimatorContext(@Nullable AnimatorContext animatorContext) {
        this.animatorContext = animatorContext;
    }

    public void show(@NonNull Activity from, @IdRes int containerRes) {
        this.container = (ViewGroup) from.findViewById(containerRes);
        if (container == null) {
            String idName = getResources().getResourceName(containerRes);
            throw new IllegalStateException("Could not find view by id " + idName);
        }

        descriptionText.setAlpha(0f);
        container.addView(this);

        bindAndSubscribe(Views.observeNextLayout(this),
                         ignored -> {
                             descriptionText.setTranslationY(descriptionText.getMeasuredHeight());
                             PropertyAnimatorProxy.animate(descriptionText, animatorContext)
                                     .translationY(0f)
                                     .alpha(1f)
                                     .start();
                         },
                         Functions.LOG_ERROR);
    }

    public void dismiss() {
        if (container != null) {
            ViewGroup oldContainer = container;
            PropertyAnimatorProxy.animate(this, animatorContext)
                    .setDuration(Animation.DURATION_VERY_FAST)
                    .fadeOut(GONE)
                    .addOnAnimationCompleted(finished -> {
                        oldContainer.removeView(this);
                    })
                    .start();
            this.container = null;
        }
    }

    //endregion


    //region Interactions

    private void showInteractionFrom() {
        this.interactionView = new InteractionView(getContext());

        int interactionMidX = interactionView.getMinimumWidth() / 2;
        int interactionMidY = interactionView.getMinimumHeight() / 2;

        Rect anchorFrame = new Rect();
        Views.getFrameInWindow(anchorView, anchorFrame);

        LayoutParams layoutParams = new LayoutParams(interactionView.getMinimumWidth(), interactionView.getMinimumHeight());
        layoutParams.addRule(ALIGN_PARENT_TOP);
        layoutParams.addRule(ALIGN_PARENT_LEFT);
        layoutParams.leftMargin = anchorFrame.centerX() - interactionMidX;
        layoutParams.topMargin = anchorFrame.centerY() - interactionMidY;

        postDelayed(() -> {
            interactionView.setAlpha(0f);
            addView(interactionView, layoutParams);
            PropertyAnimatorProxy.animate(interactionView)
                    .fadeIn()
                    .addOnAnimationCompleted(finished -> {
                        if (finished) {
                            interactionView.playTutorial(tutorial);
                        }
                    })
                    .start();
        }, 150);
    }

    private void interactionStarted() {
        if (interactionView != null) {
            interactionView.stopAnimation();

            PropertyAnimatorProxy.animate(interactionView)
                    .setDuration(Animation.DURATION_VERY_FAST)
                    .fadeOut(View.GONE)
                    .start();

            PropertyAnimatorProxy.animate(descriptionText)
                    .setDuration(Animation.DURATION_VERY_FAST)
                    .fadeOut(View.GONE)
                    .start();
        }
    }

    private void interactionCanceled() {
        Log.i(getClass().getSimpleName(), "interactionCanceled()");

        dismiss();
    }

    private void interactionCompleted() {
        Log.i(getClass().getSimpleName(), "interactionCompleted()");

        dismiss();
    }

    //endregion


    //region Event Handling

    private boolean interceptTouchEvent(@NonNull MotionEvent event) {
        if (!trackingInteraction && Views.isMotionEventInside(descriptionText, event)) {
            dispatchTouchEvent(event);
            this.dispatchedLastEvent = true;
            return true;
        }

        if (dispatchedLastEvent) {
            // If we don't do this, the description won't unhighlight
            // from the user dragging their finger outside.
            MotionEvent fakeCancelEvent = MotionEvent.obtain(event);
            fakeCancelEvent.setAction(MotionEvent.ACTION_CANCEL);
            dispatchTouchEvent(fakeCancelEvent);
            fakeCancelEvent.recycle();

            this.dispatchedLastEvent = false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                this.interactionStartX = event.getRawX();
                this.interactionStartY = event.getRawY();
                this.trackingInteraction = true;

                interactionStarted();

                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                if (tutorial.interaction == Interaction.TAP) {
                    if (Views.isMotionEventInside(anchorView, event)) {
                        interactionCompleted();
                    } else {
                        interactionCanceled();
                    }
                } else {
                    if (tutorial.interaction.isVertical) {
                        float deltaY = Math.abs(interactionStartY - event.getRawY());
                        if (deltaY >= getResources().getDimensionPixelSize(R.dimen.interaction_slide_positive)) {
                            interactionCompleted();
                        } else {
                            interactionCanceled();
                        }
                    } else {
                        float deltaX = Math.abs(interactionStartX - event.getRawX());
                        if (deltaX >= getResources().getDimensionPixelSize(R.dimen.interaction_slide_positive)) {
                            interactionCompleted();
                        } else {
                            interactionCanceled();
                        }
                    }
                }

                this.trackingInteraction = false;

                break;
            }

            default: {
                break;
            }
        }

        return false;
    }

    private class EventInterceptor implements Window.Callback {
        private final Window.Callback target;

        private EventInterceptor(@NonNull Window.Callback target) {
            this.target = target;
        }

        public Window.Callback getTarget() {
            return target;
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            return ((isAttachedToWindow() && interceptTouchEvent(event)) ||
                    target.dispatchTouchEvent(event));
        }

        //region Forwarded

        @Override
        public boolean dispatchTrackballEvent(MotionEvent event) {
            return target.dispatchTrackballEvent(event);
        }

        @Override
        public boolean dispatchGenericMotionEvent(MotionEvent event) {
            return target.dispatchGenericMotionEvent(event);
        }

        @Override
        public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
            return target.dispatchPopulateAccessibilityEvent(event);
        }

        @Override
        public View onCreatePanelView(int featureId) {
            return target.onCreatePanelView(featureId);
        }

        @Override
        public boolean onCreatePanelMenu(int featureId, Menu menu) {
            return target.onCreatePanelMenu(featureId, menu);
        }

        @Override
        public boolean onPreparePanel(int featureId, View view, Menu menu) {
            return target.onPreparePanel(featureId, view, menu);
        }

        @Override
        public boolean onMenuOpened(int featureId, Menu menu) {
            return target.onMenuOpened(featureId, menu);
        }

        @Override
        public boolean onMenuItemSelected(int featureId, MenuItem item) {
            return target.onMenuItemSelected(featureId, item);
        }

        @Override
        public void onWindowAttributesChanged(WindowManager.LayoutParams attrs) {
            target.onWindowAttributesChanged(attrs);
        }

        @Override
        public void onContentChanged() {
            target.onContentChanged();
        }

        @Override
        public void onWindowFocusChanged(boolean hasFocus) {
            target.onWindowFocusChanged(hasFocus);
        }

        @Override
        public void onAttachedToWindow() {
            target.onAttachedToWindow();
        }

        @Override
        public void onDetachedFromWindow() {
            target.onDetachedFromWindow();
        }

        @Override
        public void onPanelClosed(int featureId, Menu menu) {
            target.onPanelClosed(featureId, menu);
        }

        @Override
        public boolean onSearchRequested() {
            return target.onSearchRequested();
        }

        @Override
        public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
            return target.onWindowStartingActionMode(callback);
        }

        @Override
        public void onActionModeStarted(ActionMode mode) {
            target.onActionModeStarted(mode);
        }

        @Override
        public void onActionModeFinished(ActionMode mode) {
            target.onActionModeFinished(mode);
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            return target.dispatchKeyEvent(event);
        }

        @Override
        public boolean dispatchKeyShortcutEvent(KeyEvent event) {
            return target.dispatchKeyShortcutEvent(event);
        }

        //endregion
    }

    //endregion
}
