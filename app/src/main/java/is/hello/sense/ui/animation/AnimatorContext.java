package is.hello.sense.ui.animation;

import android.animation.Animator;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import rx.functions.Action1;

public class AnimatorContext implements Animator.AnimatorListener {
    private static final boolean DEBUG = false;

    private static final int MSG_IDLE = 0;

    private final String name;
    private final List<Listener> listeners = new ArrayList<>();

    private int activeAnimationCount = 0;

    private final Handler idleHandler = new Handler(Looper.getMainLooper(), message -> {
        if (message.what == MSG_IDLE) {
            for (Listener listener : listeners) {
                listener.onContextIdle();
            }

            return true;
        }

        return false;
    });

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
        if (DEBUG) {
            printTrace("runWhenIdle");
        }

        if (activeAnimationCount == 0) {
            runnable.run();
        } else {
            addListener(new Listener() {
                boolean consumed = false;

                @Override
                public void onContextIdle() {
                    if (!consumed) {
                        runnable.run();
                        idleHandler.post(() -> listeners.remove(this));
                        this.consumed = true;
                    }
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

    //endregion


    //region Active Animations

    private void printTrace(@NonNull String traceName) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        int size = Math.min(stackTrace.length - 3, 4);
        String[] partialTraceComponents = new String[size];
        for (int i = 0; i < size; i++) {
            StackTraceElement stackTraceElement = stackTrace[i + 3];
            partialTraceComponents[i] = stackTraceElement.getClassName() + "#" + stackTraceElement.getMethodName();
        }
        String partialTrace = TextUtils.join(" -> ", partialTraceComponents);
        Log.i(getClass().getSimpleName(), traceName + ": " + partialTrace);
    }

    public void beginAnimation() {
        idleHandler.removeMessages(MSG_IDLE);

        this.activeAnimationCount++;

        if (DEBUG) {
            printTrace("beginAnimation [" + activeAnimationCount + "]");
        }
    }

    public void endAnimation() {
        if (activeAnimationCount == 0) {
            throw new IllegalStateException("No active animations to end in " + toString());
        }

        this.activeAnimationCount--;

        if (DEBUG) {
            printTrace("endAnimation [" + activeAnimationCount + "]");
        }

        if (activeAnimationCount == 0) {
            idleHandler.removeMessages(MSG_IDLE);
            idleHandler.sendEmptyMessage(MSG_IDLE);
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
     * @param properties    An optional animation properties to apply to each animator.
     * @param options       The options to apply to the transaction
     * @param animations    A callback that describes the animations to run against a given facade.
     * @param onCompleted   An optional listener to invoke when the longest animation completes.
     */
    public void transaction(@Nullable AnimatorConfig properties,
                            @TransactionOptions int options,
                            @NonNull Action1<TransactionFacade> animations,
                            @Nullable PropertyAnimatorProxy.OnAnimationCompleted onCompleted) {
        List<PropertyAnimatorProxy> animators = new ArrayList<>(2);

        TransactionFacade facade = view -> {
            PropertyAnimatorProxy animator = PropertyAnimatorProxy.animate(view, this);
            if (properties != null) {
                properties.apply(animator);
            }
            animators.add(animator);
            return animator;
        };
        animations.call(facade);

        if ((options & OPTION_START_ON_IDLE) == OPTION_START_ON_IDLE) {
            runWhenIdle(() -> startTransaction(animators, onCompleted));
        } else {
            startTransaction(animators, onCompleted);
        }
    }

    /**
     * Internal. Second half of {@link #transaction(AnimatorConfig, int, Action1, PropertyAnimatorProxy.OnAnimationCompleted)}.
     */
    private void startTransaction(@NonNull List<PropertyAnimatorProxy> animators,
                                  @Nullable PropertyAnimatorProxy.OnAnimationCompleted onCompleted) {
        PropertyAnimatorProxy longestAnimator = null;
        for (PropertyAnimatorProxy animator : animators) {
            if (longestAnimator == null || animator.getTotalDuration() >= longestAnimator.getTotalDuration()) {
                longestAnimator = animator;
            }

            animator.start();
        }

        if (longestAnimator != null && onCompleted != null) {
            longestAnimator.addOnAnimationCompleted(onCompleted);
        }
    }

    /**
     * Short-hand provided for common use-case.
     *
     * @see #transaction(AnimatorConfig, int, Action1, PropertyAnimatorProxy.OnAnimationCompleted)
     */
    public void transaction(@NonNull Action1<TransactionFacade> animations,
                            @Nullable PropertyAnimatorProxy.OnAnimationCompleted onCompleted) {
        transaction(null, AnimatorContext.OPTIONS_DEFAULT, animations, onCompleted);
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
     * @see #transaction(AnimatorConfig, int, Action1, PropertyAnimatorProxy.OnAnimationCompleted)
     */
    public interface TransactionFacade {
        /**
         * Create a property animator proxy for a given view,
         * applying any properties provided, and queuing it
         * to be executed with any other animators in the
         * containing transaction.
         * <p/>
         * No external references to the returned animator should be made.
         */
        PropertyAnimatorProxy animate(@NonNull View view);
    }

    /**
     * The transaction should start when the animator context is idle.
     */
    public static final int OPTION_START_ON_IDLE = (1 << 1);

    /**
     * Use the default transaction options.
     */
    public static final int OPTIONS_DEFAULT = (OPTION_START_ON_IDLE);

    @IntDef(flag = true, value = {
            OPTION_START_ON_IDLE,
            OPTIONS_DEFAULT,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface TransactionOptions {}


    public interface Scene {
        @NonNull AnimatorContext getAnimatorContext();
    }
}
