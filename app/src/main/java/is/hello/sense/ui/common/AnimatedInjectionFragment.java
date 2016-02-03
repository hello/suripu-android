package is.hello.sense.ui.common;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.TimeInterpolator;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;

import is.hello.sense.ui.widget.util.Views;

/**
 * A fragment which has complex enter and exit animations.
 */
public abstract class AnimatedInjectionFragment extends InjectionFragment {
    private static final String SAVED_HAS_ANIMATED = AnimatedInjectionFragment.class.getName() + ".SAVED_HAS_ANIMATED";

    private boolean hasAnimated = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.hasAnimated = (savedInstanceState != null &&
                savedInstanceState.getBoolean(SAVED_HAS_ANIMATED));
    }

    @Override
    public final Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        if (enter) {
            final AnimatorSet placeholder = new AnimatorSet();
            if (hasAnimated) {
                onSkipEnterAnimator();
            } else {
                final View view = getView();
                assert(view != null);
                view.setVisibility(View.INVISIBLE);
                placeholder.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // The placeholder animation runs before the root view
                        // is attached to the window, somehow. So we do this
                        // on the next run loop cycle to work-around that.
                        Views.runWhenLaidOut(view, () -> {
                            final Animator animator = onProvideEnterAnimator();
                            animator.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    onEnterAnimatorEnd();
                                }
                            });
                            animator.start();

                            view.setVisibility(View.VISIBLE);
                        });
                    }
                });

                this.hasAnimated = true;
            }
            return placeholder;
        } else {
            final NoTargetAnimator exitAnimator = new NoTargetAnimator(onProvideExitAnimator());
            exitAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    onExitAnimatorEnd();
                }
            });
            return exitAnimator;
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
     * Called when the enter animator finishes.
     */
    protected void onEnterAnimatorEnd() {
        // Do nothing.
    }

    /**
     * Called when the exit animator finishes.
     * <p>
     * This is the best place to clear fields created in
     * {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * <p>
     * This method will not be called if the fragment exits
     * without the transition (e.g. on rotation.)
     */
    protected void onExitAnimatorEnd() {
        // Do nothing.
    }

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

        @Override
        public void pause() {
            wrappedAnimator.pause();
        }

        @Override
        public void resume() {
            wrappedAnimator.resume();
        }

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

        @Override
        public void addPauseListener(AnimatorPauseListener listener) {
            wrappedAnimator.addPauseListener(listener);
        }

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

    /**
     * Marks a field as being used in either {@link #onProvideEnterAnimator()}
     * or {@link #onProvideExitAnimator()}. Fields marked with this annotation should
     * be cleared in {@link #onExitAnimatorEnd()} instead of {@code onDestroyView()}.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    protected @interface UsedInTransition {
    }
}
