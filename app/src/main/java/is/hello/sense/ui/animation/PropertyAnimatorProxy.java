package is.hello.sense.ui.animation;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewPropertyAnimator;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("UnusedDeclaration")
public final class PropertyAnimatorProxy implements Animator.AnimatorListener {
    private final View view;
    private final HashMap<String, Float> properties = new HashMap<>();
    private PropertyAnimatorProxy previousInChain;

    private boolean animationStarted = false;
    private boolean animationEnded = false;
    private boolean animationCanceled = false;

    private long duration = Animation.DURATION_DEFAULT;
    private long startDelay = 0;
    private TimeInterpolator interpolator = Animation.INTERPOLATOR_DEFAULT;
    private boolean applyChangesToView = false;
    private Runnable onAnimationStarted;
    private OnAnimationCompleted onAnimationCompleted;


    //region Creation

    public PropertyAnimatorProxy(View view) {
        this.view = view;
    }

    public static @NonNull PropertyAnimatorProxy animate(@NonNull View forView) {
        return new PropertyAnimatorProxy(forView);
    }

    public static void stopAnimating(@NonNull View... forViews) {
        for (View forView : forViews) {
            forView.animate().cancel();
            forView.clearAnimation();
        }
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

    public long getStartDelay() {
        return startDelay;
    }

    public PropertyAnimatorProxy setInterpolator(TimeInterpolator interpolator) {
        this.interpolator = interpolator;
        return this;
    }

    public TimeInterpolator getInterpolator() {
        return interpolator;
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
        ViewPropertyAnimator animator = view.animate();
        animator.setDuration(duration);
        animator.setInterpolator(interpolator);
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
    }

    public void start() {
        if (previousInChain != null)
            previousInChain.start();
        else
            buildAndStart();
    }

    public void cancel() {
        view.animate().cancel();
    }

    //endregion


    //region Properties

    public View getView() {
        return view;
    }

    public Runnable getOnAnimationStarted() {
        return onAnimationStarted;
    }

    public PropertyAnimatorProxy setOnAnimationStarted(Runnable onAnimationStarted) {
        this.onAnimationStarted = onAnimationStarted;
        return this;
    }

    public OnAnimationCompleted getOnAnimationCompleted() {
        return onAnimationCompleted;
    }

    public PropertyAnimatorProxy setOnAnimationCompleted(OnAnimationCompleted onAnimationCompleted) {
        this.onAnimationCompleted = onAnimationCompleted;
        return this;
    }

    public boolean isApplyChangesToView() {
        return applyChangesToView;
    }

    public PropertyAnimatorProxy setApplyChangesToView(boolean applyChangesToView) {
        this.applyChangesToView = applyChangesToView;
        return this;
    }

    //endregion


    //region Listener

    @Override
    public void onAnimationStart(Animator animator) {
        if (animationStarted)
            return;

        animationStarted = true;

        if (onAnimationStarted != null)
            onAnimationStarted.run();
    }

    @Override
    public void onAnimationEnd(Animator animator) {
        if (animationEnded)
            return;

        animationEnded = true;

        if (applyChangesToView) {
            for (Map.Entry<String, Float> property : properties.entrySet()) {
                switch (property.getKey()) {
                    case "x":
                        getView().setX(property.getValue());
                        break;

                    case "y":
                        getView().setY(property.getValue());
                        break;

                    case "scaleX":
                        getView().setScaleX(property.getValue());
                        break;

                    case "scaleY":
                        getView().setScaleY(property.getValue());
                        break;

                    case "translationX":
                        getView().setTranslationX(property.getValue());
                        break;

                    case "translationY":
                        getView().setTranslationY(property.getValue());
                        break;

                    case "rotation":
                        getView().setRotation(property.getValue());
                        break;

                    case "alpha":
                        getView().setAlpha(property.getValue());
                        break;

                    default:
                        throw new IllegalStateException("Unknown animation property " + property.getKey());
                }
            }
        }

        if (onAnimationCompleted != null)
            onAnimationCompleted.onAnimationCompleted(!animationCanceled);
    }

    @Override
    public void onAnimationCancel(Animator animator) {
        if (animationCanceled || !animationStarted)
            return;

        animationCanceled = true;
    }

    @Override
    public void onAnimationRepeat(Animator animator) {}

    //endregion


    //region Chaining

    public PropertyAnimatorProxy andThen() {
        PropertyAnimatorProxy nextAnimation = animate(getView());
        setOnAnimationCompleted(finished -> {
            if (finished)
                nextAnimation.buildAndStart();
        });
        nextAnimation.previousInChain = this;
        return nextAnimation;
    }

    //endregion

    public interface OnAnimationCompleted {
        void onAnimationCompleted(boolean finished);
    }
}
