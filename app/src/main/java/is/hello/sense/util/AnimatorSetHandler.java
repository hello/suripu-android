package is.hello.sense.util;

import android.animation.AnimatorSet;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.lang.ref.WeakReference;

public class AnimatorSetHandler extends Handler {
    private final long callbackDelay;
    private final Runnable callback;

    /**
     * @param animatorSet should only be used when duration is set
     */
    public AnimatorSetHandler(@NonNull final AnimatorSet animatorSet){
        this(animatorSet.getDuration() + animatorSet.getStartDelay(), animatorSet);
    }

    public AnimatorSetHandler(final long callbackDelay, @NonNull final AnimatorSet animatorSet){
        super(Looper.getMainLooper());
        final WeakReference<AnimatorSet> animatorSetWeakReference = new WeakReference<>(animatorSet);
        final WeakReference<AnimatorSetHandler> handlerWeakReference = new WeakReference<>(this);
        this.callbackDelay = callbackDelay;
        this.callback = () -> {
            if(animatorSetWeakReference.get() != null
                    && !animatorSetWeakReference.get().isStarted()) {
                animatorSetWeakReference.get().start();
            }
            if(handlerWeakReference.get() != null) {
                handlerWeakReference.get().start();
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
