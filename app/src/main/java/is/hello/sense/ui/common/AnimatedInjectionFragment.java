package is.hello.sense.ui.common;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.TimeInterpolator;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import java.util.ArrayList;

/**
 * A fragment which has complex enter and exit animations.
 */
public abstract class AnimatedInjectionFragment extends InjectionFragment {
    protected static final String SAVED_HAS_ANIMATED = AnimatedInjectionFragment.class.getName() + ".SAVED_HAS_ANIMATED";

    private boolean hasAnimated = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.hasAnimated = (savedInstanceState != null && savedInstanceState.getBoolean(SAVED_HAS_ANIMATED));
    }

    @Override
    public final Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        if (enter) {
            AnimatorSet placeholder = new AnimatorSet();
            if (hasAnimated) {
                onSkipEnterAnimator();
            } else {
                View view = getView();
                assert(view != null);
                view.setVisibility(View.INVISIBLE);
                placeholder.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // The placeholder animation runs before the root view
                        // is attached to the window, somehow. So we do this
                        // on the next run loop cycle to work-around that.
                        view.post(() -> {
                            view.setVisibility(View.VISIBLE);

                            Animator animator = onProvideEnterAnimator();
                            animator.start();
                        });
                    }
                });

                this.hasAnimated = true;
            }
            return placeholder;
        } else {
            return new NoTargetAnimator(onProvideExitAnimator());
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(SAVED_HAS_ANIMATED, hasAnimated);
    }

    /**
     * Called when the fragment's root view has been attached and laid out.
     * <p />
     * This method will only be called if {@link #hasAnimated()} returns false.
     */
    protected abstract Animator onProvideEnterAnimator();

    /**
     * Called before the fragment's root view has been laid out and attached
     * when the fragment enter animation is going to be omitted.
     */
    protected abstract void onSkipEnterAnimator();

    /**
     * Called when the fragment is being removed.
     */
    protected abstract Animator onProvideExitAnimator();

    /**
     * Returns whether or not the fragment has already animated its entrance.
     * <p/>
     * Persistent across state restorations.
     */
    protected final boolean hasAnimated() {
        return hasAnimated;
    }


    /**
     * Wraps a given animator to prevent its target from being
     * set by the system's fragment transition implementation.
     */
    private static class NoTargetAnimator extends Animator {
        private final Animator wrappedAnimator;

        private NoTargetAnimator(@NonNull Animator wrappedAnimator) {
            this.wrappedAnimator = wrappedAnimator;
        }

        @Override
        public void start() {
            wrappedAnimator.start();
        }

        @Override
        public void cancel() {
            wrappedAnimator.cancel();
        }

        @Override
        public void end() {
            wrappedAnimator.end();
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        public void pause() {
            wrappedAnimator.pause();
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        public void resume() {
            wrappedAnimator.resume();
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        public boolean isPaused() {
            return wrappedAnimator.isPaused();
        }

        @Override
        public long getStartDelay() {
            return wrappedAnimator.getStartDelay();
        }

        @Override
        public void setStartDelay(long startDelay) {
            wrappedAnimator.setStartDelay(startDelay);
        }

        @Override
        public Animator setDuration(long duration) {
            return wrappedAnimator.setDuration(duration);
        }

        @Override
        public long getDuration() {
            return wrappedAnimator.getDuration();
        }

        @Override
        public void setInterpolator(TimeInterpolator value) {
            wrappedAnimator.setInterpolator(value);
        }

        @Override
        public TimeInterpolator getInterpolator() {
            return wrappedAnimator.getInterpolator();
        }

        @Override
        public boolean isRunning() {
            return wrappedAnimator.isRunning();
        }

        @Override
        public boolean isStarted() {
            return wrappedAnimator.isStarted();
        }

        @Override
        public void addListener(AnimatorListener listener) {
            wrappedAnimator.addListener(listener);
        }

        @Override
        public void removeListener(AnimatorListener listener) {
            wrappedAnimator.removeListener(listener);
        }

        @Override
        public ArrayList<AnimatorListener> getListeners() {
            return wrappedAnimator.getListeners();
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        public void addPauseListener(AnimatorPauseListener listener) {
            wrappedAnimator.addPauseListener(listener);
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        public void removePauseListener(AnimatorPauseListener listener) {
            wrappedAnimator.removePauseListener(listener);
        }

        @Override
        public void removeAllListeners() {
            wrappedAnimator.removeAllListeners();
        }

        @Override
        public void setupStartValues() {
            wrappedAnimator.setupStartValues();
        }

        @Override
        public void setupEndValues() {
            wrappedAnimator.setupEndValues();
        }

        @Override
        public final void setTarget(Object target) {
            // Always do nothing.
        }
    }
}
