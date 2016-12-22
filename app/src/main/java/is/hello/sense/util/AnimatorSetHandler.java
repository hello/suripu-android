package is.hello.sense.util;

import android.animation.AnimatorSet;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.lang.ref.WeakReference;

public class AnimatorSetHandler extends Handler {
    public static final int LOOP_ANIMATION = -1;
    private final long callbackDelay;
    private final Runnable callback;
    private int repeatCount;

    /**
     * @param animatorSet should only be used when duration is set
     */
    public AnimatorSetHandler(@NonNull final AnimatorSet animatorSet){
        this(animatorSet.getDuration() + animatorSet.getStartDelay(), animatorSet);
    }

    public AnimatorSetHandler(final long callbackDelay, @NonNull final AnimatorSet animatorSet){
        this(callbackDelay, LOOP_ANIMATION, animatorSet);
    }

    public AnimatorSetHandler(final long callbackDelay,
                              final int repeatCount,
                              @NonNull final AnimatorSet animatorSet){
        super(Looper.getMainLooper());
        final WeakReference<AnimatorSet> animatorSetWeakReference = new WeakReference<>(animatorSet);
        final WeakReference<AnimatorSetHandler> handlerWeakReference = new WeakReference<>(this);
        this.callbackDelay = callbackDelay;
        this.repeatCount = repeatCount;
        this.callback = () -> {
            final AnimatorSet animatorSetRef = animatorSetWeakReference.get();
            if(animatorSetRef != null && !animatorSetRef.isStarted()) {
                animatorSetRef.start();
            }
            final AnimatorSetHandler handlerRef = handlerWeakReference.get();
            if(handlerRef != null && handlerRef.repeatCount != 0) {
                handlerRef.start();
                if(handlerRef.repeatCount != LOOP_ANIMATION) {
                    handlerRef.repeatCount--;
                }
            }
        };
    }

    public void removeCallbacks(){
        removeCallbacks(callback);
    }

    public void start(){
        this.postDelayed(callback, callbackDelay);
    }
}
