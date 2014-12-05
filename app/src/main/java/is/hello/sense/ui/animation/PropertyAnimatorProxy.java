package is.hello.sense.ui.animation;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import is.hello.sense.ui.common.ViewUtil;

public final class PropertyAnimatorProxy implements Animator.AnimatorListener {
    private final View view;
    private final HashMap<String, Float> properties = new HashMap<>();
    private final List<OnAnimationCompleted> onAnimationCompletedListeners = new ArrayList<>();
    private PropertyAnimatorProxy previousInChain;

    private boolean animationStarted = false;
    private boolean animationEnded = false;
    private boolean animationCanceled = false;

    private long duration = Animation.DURATION_DEFAULT;
    private long startDelay = 0;
    private TimeInterpolator interpolator = Animation.INTERPOLATOR_DEFAULT;
    private boolean applyChangesToView = false;
    private Runnable onAnimationWillStart;
    private Runnable onAnimationStarted;


    //region Creation

    public PropertyAnimatorProxy(@NonNull View view) {
        this.view = view;
    }

    public static @NonNull PropertyAnimatorProxy animate(@NonNull View forView) {
        return new PropertyAnimatorProxy(forView);
    }

    public static void stop(@NonNull View... forViews) {
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

    public PropertyAnimatorProxy setStartDelay(long startDelay) {
        this.startDelay = startDelay;
        return this;
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
        if (onAnimationWillStart != null)
            onAnimationWillStart.run();

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
    }

    public void start() {
        if (previousInChain != null) {
            previousInChain.start();
        } else {
            buildAndStart();
        }
    }

    public void startAfterLayout() {
        ViewUtil.observeNextLayout(view).subscribe(ignored -> start());
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

    public PropertyAnimatorProxy setOnAnimationStarted(@NonNull Runnable onAnimationStarted) {
        this.onAnimationStarted = onAnimationStarted;
        return this;
    }

    public PropertyAnimatorProxy addOnAnimationCompleted(@NonNull OnAnimationCompleted onAnimationCompleted) {
        this.onAnimationCompletedListeners.add(onAnimationCompleted);
        return this;
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

        for (OnAnimationCompleted listener : onAnimationCompletedListeners)
            listener.onAnimationCompleted(!animationCanceled);
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
        nextAnimation.setDuration(duration);
        nextAnimation.setInterpolator(interpolator);
        addOnAnimationCompleted(finished -> {
            if (finished)
                nextAnimation.buildAndStart();
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
        }).alpha(1f).setApplyChangesToView(true);
    }

    public PropertyAnimatorProxy simplePop() {
        return setDuration(Animation.DURATION_MINIMUM / 2)
                .setInterpolator(new AccelerateInterpolator())
                .scale(1.1f)
                .andThen()
                .setInterpolator(new DecelerateInterpolator())
                .scale(1.0f);
    }

    public PropertyAnimatorProxy zoomInFromNothing() {
        return setOnAnimationWillStart(() -> {
            view.setAlpha(0f);
            view.setScaleX(0f);
            view.setScaleY(0f);
            view.setVisibility(View.VISIBLE);
        }).setApplyChangesToView(true).scale(1f).alpha(1f);
    }

    public PropertyAnimatorProxy fadeOut(int targetVisibility) {
        return alpha(0f).addOnAnimationCompleted(finished -> {
            if (finished) {
                view.setVisibility(targetVisibility);
            }
        });
    }

    //endregion


    public interface OnAnimationCompleted {
        void onAnimationCompleted(boolean finished);
    }
}
