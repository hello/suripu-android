package is.hello.sense.util;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

public class Animation {
    public static final long DEFAULT_DURATION = 250;
    public static final Interpolator DEFAULT_INTERPOLATOR = new AccelerateDecelerateInterpolator();

    public static final class Properties {
        public long duration = DEFAULT_DURATION;
        public Interpolator interpolator = DEFAULT_INTERPOLATOR;
        public long startDelay = 0;

        public static @NonNull Properties create() {
            return new Properties();
        }

        public static @NonNull Properties createWithDelay(long delay) {
            Properties properties = create();
            properties.startDelay = delay;
            return properties;
        }


        public <T extends Animator> T apply(@NonNull T animator) {
            animator.setDuration(duration);
            animator.setInterpolator(interpolator);
            animator.setStartDelay(startDelay);

            return animator;
        }

        public PropertyAnimatorProxy apply(@NonNull PropertyAnimatorProxy animator) {
            return animator.setDuration(duration)
                           .setInterpolator(interpolator)
                           .setStartDelay(startDelay);
        }


        public @NonNull PropertyAnimatorProxy toPropertyAnimator(@NonNull View forView) {
            return apply(new PropertyAnimatorProxy(forView));
        }
    }

    public static @NonNull PropertyAnimatorProxy animate(@NonNull View forView) {
        return new PropertyAnimatorProxy(forView);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static final class PropertyAnimatorProxy implements Animator.AnimatorListener {
        private final View view;
        private final ViewPropertyAnimator animator;

        private boolean animationStarted = false;
        private boolean animationEnded = false;
        private boolean animationCanceled = false;

        private Runnable onAnimationStarted;
        private OnAnimationCompleted onAnimationCompleted;

        public PropertyAnimatorProxy(View view) {
            this.view = view;
            this.animator = view.animate();
            animator.setDuration(DEFAULT_DURATION);
            animator.setInterpolator(DEFAULT_INTERPOLATOR);
            animator.setListener(this);
        }

        //region Delegation

        public PropertyAnimatorProxy setDuration(long duration) {
            animator.setDuration(duration);
            return this;
        }

        public long getDuration() {
            return animator.getDuration();
        }

        public PropertyAnimatorProxy setStartDelay(long startDelay) {
            animator.setStartDelay(startDelay);
            return this;
        }

        public long getStartDelay() {
            return animator.getStartDelay();
        }

        public PropertyAnimatorProxy setInterpolator(TimeInterpolator interpolator) {
            animator.setInterpolator(interpolator);
            return this;
        }

        public TimeInterpolator getInterpolator() {
            return animator.getInterpolator();
        }

        public PropertyAnimatorProxy translationYBy(float value) {
            animator.translationYBy(value);
            return this;
        }

        public PropertyAnimatorProxy scaleY(float value) {
            animator.scaleY(value);
            return this;
        }

        public PropertyAnimatorProxy yBy(float value) {
            animator.yBy(value);
            return this;
        }

        public PropertyAnimatorProxy x(float value) {
            animator.x(value);
            return this;
        }

        public PropertyAnimatorProxy alphaBy(float value) {
            animator.alphaBy(value);
            return this;
        }

        public PropertyAnimatorProxy scaleYBy(float value) {
            animator.scaleYBy(value);
            return this;
        }

        public PropertyAnimatorProxy scaleX(float value) {
            animator.scaleX(value);
            return this;
        }

        public PropertyAnimatorProxy rotationY(float value) {
            animator.rotationY(value);
            return this;
        }

        public PropertyAnimatorProxy y(float value) {
            animator.y(value);
            return this;
        }

        public PropertyAnimatorProxy xBy(float value) {
            animator.xBy(value);
            return this;
        }

        public PropertyAnimatorProxy rotationYBy(float value) {
            animator.rotationYBy(value);
            return this;
        }

        public PropertyAnimatorProxy translationXBy(float value) {
            animator.translationXBy(value);
            return this;
        }

        public PropertyAnimatorProxy translationY(float value) {
            animator.translationY(value);
            return this;
        }

        public PropertyAnimatorProxy translationX(float value) {
            animator.translationX(value);
            return this;
        }

        public PropertyAnimatorProxy rotationXBy(float value) {
            animator.rotationXBy(value);
            return this;
        }

        public PropertyAnimatorProxy alpha(float value) {
            animator.alpha(value);
            return this;
        }

        public PropertyAnimatorProxy rotation(float value) {
            animator.rotation(value);
            return this;
        }

        public PropertyAnimatorProxy rotationBy(float value) {
            animator.rotationBy(value);
            return this;
        }

        public PropertyAnimatorProxy withLayer() {
            animator.withLayer();
            return this;
        }

        public PropertyAnimatorProxy scaleXBy(float value) {
            animator.scaleXBy(value);
            return this;
        }

        public PropertyAnimatorProxy rotationX(float value) {
            animator.rotationX(value);
            return this;
        }

        public void start() {
            animator.start();
        }

        public void cancel() {
            animator.cancel();
        }

        //endregion


        //region Properties

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

            if (onAnimationCompleted != null)
                onAnimationCompleted.onAnimationCompleted(!animationCanceled);
        }

        @Override
        public void onAnimationCancel(Animator animator) {
            if (animationCanceled)
                return;

            animationCanceled = true;
        }

        @Override
        public void onAnimationRepeat(Animator animator) {}

        //endregion


        public interface OnAnimationCompleted {
            void onAnimationCompleted(boolean finished);
        }
    }
}
