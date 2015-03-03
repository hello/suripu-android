package is.hello.sense.ui.animation;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PropertyAnimatorProxy implements Animator.AnimatorListener {
    private static final Set<View> ANIMATING_VIEWS = new HashSet<>();

    private final View view;
    private final @Nullable AnimatorContext system;

    private final HashMap<String, Float> properties = new HashMap<>();
    private final List<OnAnimationCompleted> onAnimationCompletedListeners = new ArrayList<>();
    private PropertyAnimatorProxy previousInChain;

    private boolean animationStarted = false;
    private boolean animationEnded = false;
    private boolean animationCanceled = false;

    private long duration = Animation.DURATION_NORMAL;
    private long startDelay = 0;
    private TimeInterpolator interpolator = Animation.INTERPOLATOR_DEFAULT;
    private Runnable onAnimationWillStart;


    //region Creation

    public PropertyAnimatorProxy(@NonNull View view, @Nullable AnimatorContext system) {
        this.view = view;
        this.system = system;
    }

    public static @NonNull PropertyAnimatorProxy animate(@NonNull View forView) {
        return new PropertyAnimatorProxy(forView, null);
    }

    public static @NonNull PropertyAnimatorProxy animate(@NonNull View forView, @Nullable AnimatorContext system) {
        return new PropertyAnimatorProxy(forView, system);
    }

    public static void stop(@NonNull View... forViews) {
        for (View forView : forViews) {
            forView.animate().cancel();
            forView.clearAnimation();
        }
    }

    public static boolean isAnimating(@NonNull View view) {
        return ANIMATING_VIEWS.contains(view);
    }

    //endregion


    //region Delegation

    public PropertyAnimatorProxy setDuration(long duration) {
        this.duration = duration;
        return this;
    }

    public long getDuration() {
        return duration;
    }

    public PropertyAnimatorProxy setStartDelay(long startDelay) {
        this.startDelay = startDelay;
        return this;
    }

    public PropertyAnimatorProxy setInterpolator(TimeInterpolator interpolator) {
        this.interpolator = interpolator;
        return this;
    }

    public PropertyAnimatorProxy x(float value) {
        properties.put("x", value);
        return this;
    }

    public PropertyAnimatorProxy scaleX(float value) {
        properties.put("scaleX", value);
        return this;
    }

    public PropertyAnimatorProxy scaleY(float value) {
        properties.put("scaleY", value);
        return this;
    }

    public PropertyAnimatorProxy scale(float value) {
        return scaleX(value).scaleY(value);
    }

    public PropertyAnimatorProxy y(float value) {
        properties.put("y", value);
        return this;
    }

    public PropertyAnimatorProxy translationY(float value) {
        properties.put("translationY", value);
        return this;
    }

    public PropertyAnimatorProxy translationX(float value) {
        properties.put("translationX", value);
        return this;
    }

    public PropertyAnimatorProxy alpha(float value) {
        properties.put("alpha", value);
        return this;
    }

    public PropertyAnimatorProxy rotation(float value) {
        properties.put("rotation", value);
        return this;
    }

    private void buildAndStart() {
        if (onAnimationWillStart != null) {
            onAnimationWillStart.run();
        }

        ViewPropertyAnimator animator = view.animate();
        animator.setDuration(duration);
        animator.setInterpolator(interpolator);
        animator.setStartDelay(startDelay);
        animator.setListener(this);

        for (Map.Entry<String, Float> property : properties.entrySet()) {
            switch (property.getKey()) {
                case "x":
                    animator.x(property.getValue());
                    break;

                case "y":
                    animator.y(property.getValue());
                    break;

                case "scaleX":
                    animator.scaleX(property.getValue());
                    break;

                case "scaleY":
                    animator.scaleY(property.getValue());
                    break;

                case "translationX":
                    animator.translationX(property.getValue());
                    break;

                case "translationY":
                    animator.translationY(property.getValue());
                    break;

                case "rotation":
                    animator.rotation(property.getValue());
                    break;

                case "alpha":
                    animator.alpha(property.getValue());
                    break;

                default:
                    throw new IllegalStateException("Unknown animation property " + property.getKey());
            }
        }

        if (system != null) {
            system.beginAnimation();
        }

        ANIMATING_VIEWS.add(view);
    }

    public void start() {
        if (previousInChain != null) {
            previousInChain.start();
        } else {
            buildAndStart();
        }
    }

    public void cancel() {
        view.animate().cancel();
    }

    //endregion


    //region Properties

    public View getView() {
        return view;
    }

    public PropertyAnimatorProxy setOnAnimationWillStart(Runnable onAnimationWillStart) {
        this.onAnimationWillStart = onAnimationWillStart;
        return this;
    }

    public PropertyAnimatorProxy addOnAnimationCompleted(@NonNull OnAnimationCompleted onAnimationCompleted) {
        this.onAnimationCompletedListeners.add(onAnimationCompleted);
        return this;
    }

    //endregion


    //region Listener

    @Override
    public void onAnimationStart(Animator animator) {
        if (animationStarted)
            return;

        this.animationStarted = true;
    }

    @Override
    public void onAnimationEnd(Animator animator) {
        if (animationEnded)
            return;

        this.animationEnded = true;

        for (OnAnimationCompleted listener : onAnimationCompletedListeners) {
            listener.onAnimationCompleted(!animationCanceled);
        }

        if (system != null) {
            view.post(system::endAnimation);
        }
        ANIMATING_VIEWS.remove(view);
    }

    @Override
    public void onAnimationCancel(Animator animator) {
        if (animationCanceled || !animationStarted)
            return;

        this.animationCanceled = true;

        if (system != null) {
            view.post(system::endAnimation);
        }
        ANIMATING_VIEWS.remove(view);
    }

    @Override
    public void onAnimationRepeat(Animator animator) {}

    //endregion


    //region Chaining

    public PropertyAnimatorProxy andThen() {
        PropertyAnimatorProxy nextAnimation = animate(getView());
        nextAnimation.setDuration(duration);
        nextAnimation.setInterpolator(interpolator);
        addOnAnimationCompleted(finished -> {
            if (finished) {
                nextAnimation.buildAndStart();
            }
        });
        nextAnimation.previousInChain = this;
        return nextAnimation;
    }

    //endregion


    //region Canned Animations

    public PropertyAnimatorProxy fadeIn() {
        return setOnAnimationWillStart(() -> {
            view.setAlpha(0f);
            view.setVisibility(View.VISIBLE);
        }).alpha(1f);
    }

    public PropertyAnimatorProxy fadeOut(@ViewVisibility int targetVisibility) {
        return alpha(0f).addOnAnimationCompleted(finished -> {
            if (finished) {
                view.setVisibility(targetVisibility);
            }
        });
    }

    public PropertyAnimatorProxy simplePop(float amount) {
        return setDuration(Animation.DURATION_FAST / 2)
                .setInterpolator(new AccelerateInterpolator())
                .scale(amount)
                .andThen()
                .setInterpolator(new DecelerateInterpolator())
                .scale(1.0f);
    }

    public PropertyAnimatorProxy zoomInFrom(float startScale) {
        return setOnAnimationWillStart(() -> {
            view.setAlpha(0f);
            view.setScaleX(startScale);
            view.setScaleY(startScale);
            view.setVisibility(View.VISIBLE);
        }).scale(1f).alpha(1f);
    }

    public PropertyAnimatorProxy zoomOutTo(@ViewVisibility int targetVisibility, float endScale) {
        return scale(endScale).alpha(0f).addOnAnimationCompleted(finished -> {
            if (finished) {
                view.setVisibility(targetVisibility);

                view.setAlpha(1f);
                view.setScaleX(1f);
                view.setScaleY(1f);
            }
        });
    }

    public PropertyAnimatorProxy slideYAndFade(float startDeltaY, float endDeltaY,
                                               float startAlpha, float endAlpha) {
        return setOnAnimationWillStart(() -> {
            float y = view.getY();
            float startY = y + startDeltaY;
            float endY = y + endDeltaY;

            view.setAlpha(startAlpha);
            view.setY(startY);
            view.setVisibility(View.VISIBLE);

            y(endY);
            alpha(endAlpha);
        });
    }

    public PropertyAnimatorProxy slideXAndFade(float startDeltaX, float endDeltaX,
                                               float startAlpha, float endAlpha) {
        return setOnAnimationWillStart(() -> {
            float x = view.getX();
            float startX = x + startDeltaX;
            float endX = x + endDeltaX;

            view.setAlpha(startAlpha);
            view.setX(startX);
            view.setVisibility(View.VISIBLE);

            x(endX);
            alpha(endAlpha);
        });
    }

    //endregion


    public interface OnAnimationCompleted {
        void onAnimationCompleted(boolean finished);
    }

    @IntDef({View.VISIBLE, View.INVISIBLE, View.GONE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ViewVisibility {}
}
