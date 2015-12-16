package is.hello.sense.ui.handholding;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.ActionMode;
import android.view.Gravity;
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

import is.hello.go99.Anime;
import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Logger;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

@SuppressLint("ViewConstructor")
public class TutorialOverlayView extends RelativeLayout {
    public static final @IdRes int ROOT_CONTAINER_ID = R.id.item_tutorial_description;

    private final Activity activity;
    private final Tutorial tutorial;
    private final TextView descriptionText;

    private @Nullable InteractionView interactionView;
    private @Nullable View anchorView;
    private float interactionStartX = 0f, interactionStartY = 0f;
    private boolean trackingInteraction = false;

    private @Nullable View anchorContainer;
    private @Nullable ViewGroup container;
    private @Nullable Runnable onDismiss;

    //region Creation

    public TutorialOverlayView(@NonNull Activity activity, @NonNull Tutorial tutorial) {
        super(activity);

        this.activity = activity;
        this.tutorial = tutorial;

        final Resources resources = getResources();
        final LayoutInflater inflater = LayoutInflater.from(activity);
        final ViewGroup descriptionContainer = (ViewGroup) inflater.inflate(R.layout.item_tutorial_description,
                                                                            this, false);
        this.descriptionText = (TextView) descriptionContainer.findViewById(R.id.item_tutorial_description_text);
        descriptionText.setText(tutorial.descriptionRes);
        Views.setSafeOnClickListener(descriptionText, ignored -> interactionCompleted());

        final Drawable dismissIcon = descriptionText.getCompoundDrawablesRelative()[2].mutate();
        dismissIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        descriptionText.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, dismissIcon, null);

        final View shadow = new View(activity);
        final int shadowHeight = resources.getDimensionPixelSize(R.dimen.shadow_size);
        final ViewGroup.LayoutParams shadowLayoutParams
                = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                             shadowHeight);
        if (tutorial.descriptionGravity == Gravity.TOP) {
            shadow.setBackgroundResource(R.drawable.shadow_top_down);
            descriptionContainer.addView(shadow, descriptionContainer.getChildCount(), shadowLayoutParams);

            final View topLine = new View(activity);
            topLine.setBackgroundResource(R.color.light_accent_dimmed);
            final int topLineHeight = resources.getDimensionPixelSize(R.dimen.divider_size);
            final ViewGroup.LayoutParams topLineLayoutParams
                    = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                 topLineHeight);
            descriptionContainer.addView(topLine, 0, topLineLayoutParams);
        } else {
            descriptionContainer.setBackgroundResource(R.color.light_accent);
            shadow.setBackgroundResource(R.drawable.shadow_bottom_up);
            descriptionContainer.addView(shadow, 0, shadowLayoutParams);
        }

        addView(descriptionContainer, tutorial.generateDescriptionLayoutParams());
    }

    //endregion


    //region Lifecycle

    private @Nullable Window getWindow() {
        return activity.getWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        final Window window = getWindow();
        if (window != null && !(window.getCallback() instanceof EventInterceptor)) {
            Logger.info(getClass().getSimpleName(), "Attaching interceptor");
            window.setCallback(new EventInterceptor(window.getCallback()));
        }

        if (interactionView == null) {
            if (anchorContainer != null) {
                this.anchorView = anchorContainer.findViewById(tutorial.anchorId);
            } else {
                this.anchorView = activity.findViewById(tutorial.anchorId);
            }

            if (anchorView != null) {
                Views.runWhenLaidOut(anchorView, this::showInteractionFrom);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        final Window window = getWindow();
        if (window != null && (window.getCallback() instanceof EventInterceptor)) {
            Logger.info(getClass().getSimpleName(), "Detaching interceptor");
            final EventInterceptor interceptor = (EventInterceptor) window.getCallback();
            window.setCallback(interceptor.getTarget());
        }

        this.anchorView = null;
    }

    //endregion


    //region Showing

    public void setOnDismiss(@Nullable Runnable onDismiss) {
        this.onDismiss = onDismiss;
    }

    public void setAnchorContainer(@Nullable View anchorContainer) {
        this.anchorContainer = anchorContainer;
    }

    public void show(@IdRes int containerRes) {
        this.container = (ViewGroup) activity.findViewById(containerRes);
        if (container == null) {
            final String idName = getResources().getResourceName(containerRes);
            throw new IllegalStateException("Could not find view by id " + idName);
        }

        setAlpha(0f);
        container.addView(this);

        Views.runWhenLaidOut(this, () -> {
            animatorFor(this)
                    .alpha(1f)
                    .start();
        });
    }

    public void postShow(@IdRes int containerRes) {
        post(() -> show(containerRes));
    }

    public void dismiss(boolean animate) {
        if (container != null) {
            if (animate) {
                final ViewGroup oldContainer = container;
                animatorFor(this)
                        .withDuration(Anime.DURATION_VERY_FAST)
                        .fadeOut(GONE)
                        .addOnAnimationCompleted(finished -> {
                            oldContainer.removeView(this);
                            if (onDismiss != null) {
                                onDismiss.run();
                            }
                        })
                        .start();
            } else {
                container.removeView(this);
                if (onDismiss != null) {
                    onDismiss.run();
                }
            }
            this.container = null;
        }
    }

    //endregion


    //region Interactions

    private void showInteractionFrom() {
        if (anchorView == null) {
            return;
        }

        this.interactionView = new InteractionView(getContext());

        final int interactionMidX = interactionView.getMinimumWidth() / 2;
        final int interactionMidY = interactionView.getMinimumHeight() / 2;

        final Rect anchorFrame = new Rect();
        Views.getFrameInWindow(anchorView, anchorFrame);

        final LayoutParams layoutParams = new LayoutParams(interactionView.getMinimumWidth(),
                                                           interactionView.getMinimumHeight());
        layoutParams.addRule(ALIGN_PARENT_TOP);
        layoutParams.addRule(ALIGN_PARENT_LEFT);
        layoutParams.leftMargin = anchorFrame.centerX() - interactionMidX;
        layoutParams.topMargin = anchorFrame.centerY() - interactionMidY;

        final ActionBar actionBar = activity.getActionBar();
        if (actionBar != null) {
            layoutParams.topMargin -= actionBar.getHeight();
        }

        postDelayed(() -> {
            interactionView.setAlpha(0f);
            addView(interactionView, layoutParams);
            animatorFor(interactionView)
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

            animatorFor(interactionView)
                    .withDuration(Anime.DURATION_VERY_FAST)
                    .fadeOut(View.GONE)
                    .start();

            animatorFor(descriptionText)
                    .withDuration(Anime.DURATION_VERY_FAST)
                    .fadeOut(View.GONE)
                    .start();
        }
    }

    private void interactionCanceled() {
        Log.i(getClass().getSimpleName(), "interactionCanceled()");

        dismiss(true);
    }

    private void interactionCompleted() {
        Log.i(getClass().getSimpleName(), "interactionCompleted()");

        tutorial.markShown(getContext());

        dismiss(true);
    }

    //endregion


    //region Event Handling

    private boolean interceptTouchEvent(@NonNull MotionEvent event) {
        if (!trackingInteraction && Views.isMotionEventInside(descriptionText, event)) {
            return false;
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
                    if (anchorView != null && Views.isMotionEventInside(anchorView, event)) {
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
            return ((ViewCompat.isAttachedToWindow(TutorialOverlayView.this) && interceptTouchEvent(event)) ||
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
