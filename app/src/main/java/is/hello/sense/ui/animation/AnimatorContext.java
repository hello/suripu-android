package is.hello.sense.ui.animation;

import android.animation.Animator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import rx.functions.Action1;

public class AnimatorContext implements Animator.AnimatorListener {
    private final String name;
    private final List<Listener> listeners = new ArrayList<>();

    private int activeAnimationCount = 0;

    public AnimatorContext(@NonNull String name) {
        this.name = name;
    }


    //region Listeners

    /**
     * Posts a unit of work to run when the context is idle.
     * <p/>
     * The runnable will be immediately executed if
     * the animation context is currently idle.
     */
    public void runWhenIdle(@NonNull Runnable runnable) {
        if (activeAnimationCount == 0) {
            runnable.run();
        } else {
            addListener(new Listener() {
                @Override
                public void onContextIdle() {
                    runnable.run();
                    removeListener(this);
                }
            });
        }
    }

    public void addListener(@NonNull Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    private void onAllAnimationsEnded() {
        for (Listener listener : listeners) {
            listener.onContextIdle();
        }
    }

    //endregion


    //region Active Animations

    public void beginAnimation() {
        this.activeAnimationCount++;
    }

    public void endAnimation() {
        if (activeAnimationCount == 0) {
            throw new IllegalStateException("No active animations to end");
        }

        this.activeAnimationCount--;

        if (activeAnimationCount == 0) {
            onAllAnimationsEnded();
        }
    }

    //endregion


    //region Listener

    @Override
    public void onAnimationStart(Animator animation) {
        beginAnimation();
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        endAnimation();
        animation.removeListener(this);
    }

    @Override
    public void onAnimationCancel(Animator animation) {
        endAnimation();
        animation.removeListener(this);
    }

    @Override
    public void onAnimationRepeat(Animator animation) {
    }


    //endregion


    //region Transactions

    /**
     * Executes a series of animations within the animation context.
     * Example usage:
     * <pre>
     *     AnimatorContext context = ...;
     *     context.transaction(f -> {
     *         f.animate(oldView).fadeOut(View.GONE);
     *         f.animate(newView).fadeIn();
     *     });
     * </pre>
     * <p/>
     * The callback will be passed an instance of {@see #Facade}. This
     * facade should then be used to construct animators. The callback
     * <em>should not</em> call start on the animators, this will be
     * done automatically by the animator context.
     *
     * @param properties    An optional animation properties to apply to each animator.
     * @param animations    A callback that describes the animations to run against a given facade.
     */
    public void transaction(@Nullable AnimatorConfig properties, @NonNull Action1<Facade> animations) {
        List<PropertyAnimatorProxy> animators = new ArrayList<>(2);

        Facade facade = view -> {
            PropertyAnimatorProxy animator = PropertyAnimatorProxy.animate(view, this);
            if (properties != null) {
                properties.apply(animator);
            }
            animators.add(animator);
            return animator;
        };
        animations.call(facade);

        for (PropertyAnimatorProxy animator : animators) {
            animator.start();
        }
    }

    /**
     * Short-hand provided for common use-case.
     *
     * @see #transaction(AnimatorConfig, rx.functions.Action1)
     */
    public void transaction(@NonNull Action1<Facade> animations) {
        transaction(null, animations);
    }

    //endregion


    @Override
    public String toString() {
        return "AnimationSystem{" +
                "name='" + name + '\'' +
                '}';
    }


    /**
     * An object that is interested in listening to state changes in the context.
     */
    public interface Listener {
        void onContextIdle();
    }

    /**
     * Used for transaction callbacks to specify animations against views.
     *
     * @see #transaction(AnimatorConfig, rx.functions.Action1)
     */
    public interface Facade {
        /**
         * Create a property animator proxy for a given view,
         * applying any properties provided, and queuing it
         * to be executed with any other animators in the
         * containing transaction.
         */
        PropertyAnimatorProxy animate(@NonNull View view);
    }

    public interface Scene {
        @NonNull AnimatorContext getAnimatorContext();
    }
}
