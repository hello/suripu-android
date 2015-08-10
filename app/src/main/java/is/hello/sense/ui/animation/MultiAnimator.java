package is.hello.sense.ui.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.SimpleArrayMap;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * A wrapper around {@link ViewPropertyAnimator} that descends from {@link Animator}
 * to allow for generic treatment of all animations within an {@link AnimatorContext}.
 */
public final class MultiAnimator extends Animator implements Animator.AnimatorListener {
    private final View target;
    private final @Nullable AnimatorContext animatorContext;
    private final SimpleArrayMap<Property, Float> properties = new SimpleArrayMap<>();

    private long duration = Animation.DURATION_NORMAL;
    private long startDelay = 0;
    private TimeInterpolator interpolator = Animation.INTERPOLATOR_DEFAULT;

    private final List<Runnable> willStartListeners = new ArrayList<>();
    private @Nullable MultiAnimator previousInChain;


    //region Lifecycle

    public static MultiAnimator animatorFor(@NonNull View view) {
        return new MultiAnimator(view, null);
    }

    public static MultiAnimator animatorFor(@NonNull View view,
                                            @Nullable AnimatorContext animatorContext) {
        return new MultiAnimator(view, animatorContext);
    }

    private MultiAnimator(@NonNull View target,
                          @Nullable AnimatorContext animatorContext) {
        this.target = target;
        this.animatorContext = animatorContext;
    }

    //endregion


    //region Attributes

    @Override
    public long getDuration() {
        return duration;
    }

    @Override
    public MultiAnimator setDuration(long duration) {
        this.duration = duration;
        return this;
    }

    public MultiAnimator withDuration(long duration) {
        return setDuration(duration);
    }

    @Override
    public long getStartDelay() {
        return startDelay;
    }

    @Override
    public void setStartDelay(long startDelay) {
        this.startDelay = startDelay;
    }

    public MultiAnimator withStartDelay(long startDelay) {
        setStartDelay(startDelay);
        return this;
    }

    @Override
    public TimeInterpolator getInterpolator() {
        return interpolator;
    }

    @Override
    public void setInterpolator(TimeInterpolator interpolator) {
        this.interpolator = interpolator;
    }

    public MultiAnimator withInterpolator(@NonNull TimeInterpolator interpolator) {
        setInterpolator(interpolator);
        return this;
    }

    @Override
    public boolean isRunning() {
        return Animation.isAnimating(target);
    }

    //endregion


    //region Animations

    public MultiAnimator x(float value) {
        properties.put(Property.X, value);
        return this;
    }

    public MultiAnimator y(float value) {
        properties.put(Property.Y, value);
        return this;
    }

    public MultiAnimator translationX(float value) {
        properties.put(Property.TRANSLATION_X, value);
        return this;
    }

    public MultiAnimator translationY(float value) {
        properties.put(Property.TRANSLATION_Y, value);
        return this;
    }

    public MultiAnimator scale(float value) {
        properties.put(Property.SCALE, value);
        return this;
    }

    public MultiAnimator alpha(float value) {
        properties.put(Property.ALPHA, value);
        return this;
    }

    public MultiAnimator rotation(float value) {
        properties.put(Property.ROTATION, value);
        return this;
    }

    //endregion


    //region Forwarding

    @Override
    public void onAnimationStart(Animator animation) {
        ArrayList<AnimatorListener> listeners = getListeners();
        if (listeners != null) {
            for (int i = listeners.size() - 1; i >= 0; i--) {
                listeners.get(i).onAnimationStart(this);
            }
        }

        Animation.addAnimatingView(target);
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        ArrayList<AnimatorListener> listeners = getListeners();
        if (listeners != null) {
            for (int i = listeners.size() - 1; i >= 0; i--) {
                listeners.get(i).onAnimationEnd(this);
            }
        }

        if (animatorContext != null) {
            animatorContext.endAnimation();
        }

        Animation.removeAnimatingView(target);
    }

    @Override
    public void onAnimationCancel(Animator animation) {
        ArrayList<AnimatorListener> listeners = getListeners();
        if (listeners != null) {
            for (int i = listeners.size() - 1; i >= 0; i--) {
                listeners.get(i).onAnimationCancel(this);
            }
        }

        Animation.removeAnimatingView(target);
    }

    @Override
    public void onAnimationRepeat(Animator animation) {
        // Not supported
    }

    //endregion


    //region Running

    private void startInternal() {
        for (Runnable willStart : willStartListeners) {
            willStart.run();
        }

        ViewPropertyAnimator propertyAnimator = target.animate();
        propertyAnimator.setDuration(duration);
        propertyAnimator.setStartDelay(startDelay);
        propertyAnimator.setInterpolator(interpolator);
        propertyAnimator.setListener(this);

        for (int i = 0, size = properties.size(); i < size; i++) {
            Property property = properties.keyAt(i);
            float value = properties.valueAt(i);
            switch (property) {
                case X:
                    propertyAnimator.x(value);
                    break;
                case Y:
                    propertyAnimator.y(value);
                    break;
                case TRANSLATION_X:
                    propertyAnimator.translationX(value);
                    break;
                case TRANSLATION_Y:
                    propertyAnimator.translationY(value);
                    break;
                case SCALE:
                    propertyAnimator.scaleX(value);
                    propertyAnimator.scaleY(value);
                    break;
                case ALPHA:
                    propertyAnimator.alpha(value);
                    break;
                case ROTATION:
                    propertyAnimator.rotation(value);
                    break;
            }
        }

        if (animatorContext != null) {
            animatorContext.beginAnimation();
        }
    }

    @Override
    public void start() {
        if (previousInChain != null) {
            previousInChain.start();
        } else {
            startInternal();
        }
    }

    public void postStart() {
        target.post(this::start);
    }

    @Override
    public void cancel() {
        target.animate().cancel();
    }

    @Override
    public void end() {
        throw new AssertionError("end not supported by MultiAnimator.");
    }

    //endregion


    //region Convenience

    public MultiAnimator addOnAnimationWillStart(@NonNull Runnable willStart) {
        willStartListeners.add(willStart);
        return this;
    }

    public MultiAnimator addOnAnimationCompleted(@NonNull OnAnimationCompleted onAnimationCompleted) {
        addListener(new AnimatorListenerAdapter() {
            boolean canceled = false;

            @Override
            public void onAnimationCancel(Animator animation) {
                this.canceled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                onAnimationCompleted.onAnimationCompleted(!canceled);
            }
        });
        return this;
    }

    public MultiAnimator andThen() {
        MultiAnimator nextAnimation = new MultiAnimator(target, animatorContext);
        nextAnimation.setDuration(duration);
        nextAnimation.setInterpolator(interpolator);
        addOnAnimationCompleted(finished -> {
            if (finished) {
                nextAnimation.startInternal();
            }
        });
        nextAnimation.previousInChain = this;
        return nextAnimation;
    }

    //endregion


    //region Canned Animations

    public MultiAnimator fadeIn() {
        return addOnAnimationWillStart(() -> {
            target.setAlpha(0f);
            target.setVisibility(View.VISIBLE);
        }).alpha(1f);
    }

    public MultiAnimator fadeOut(@Animation.ViewVisibility int targetVisibility) {
        return alpha(0f)
                .addOnAnimationCompleted(finished -> {
                    if (finished) {
                        target.setVisibility(targetVisibility);
                    }
                });
    }

    public MultiAnimator simplePop(float amount) {
        return setDuration(Animation.DURATION_FAST / 2)
                .withInterpolator(new AccelerateInterpolator())
                .scale(amount)
                .andThen()
                .withInterpolator(new DecelerateInterpolator())
                .scale(1.0f);
    }

    public MultiAnimator slideYAndFade(float startDeltaY, float endDeltaY,
                                               float startAlpha, float endAlpha) {
        return addOnAnimationWillStart(() -> {
            float y = target.getY();
            float startY = y + startDeltaY;
            float endY = y + endDeltaY;

            target.setAlpha(startAlpha);
            target.setY(startY);
            target.setVisibility(View.VISIBLE);

            y(endY);
            alpha(endAlpha);
        });
    }

    public MultiAnimator slideXAndFade(float startDeltaX, float endDeltaX,
                                       float startAlpha, float endAlpha) {
        return addOnAnimationWillStart(() -> {
            float x = target.getX();
            float startX = x + startDeltaX;
            float endX = x + endDeltaX;

            target.setAlpha(startAlpha);
            target.setX(startX);
            target.setVisibility(View.VISIBLE);

            x(endX);
            alpha(endAlpha);
        });
    }

    //endregion


    private enum Property {
        X,
        Y,
        TRANSLATION_X,
        TRANSLATION_Y,
        SCALE,
        ALPHA,
        ROTATION,
    }
}
