package is.hello.sense.util;

import android.animation.AnimatorSet;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

public class AnimatorSetHandler extends Handler {
    private final long callbackDelay;
    private final Runnable callback;

    public AnimatorSetHandler(@NonNull final AnimatorSet animatorSet){
        this(animatorSet.getDuration() + animatorSet.getStartDelay(), animatorSet);
    }

    public AnimatorSetHandler(final long callbackDelay, @NonNull final AnimatorSet animatorSet){
        super(Looper.getMainLooper());
        this.callbackDelay = callbackDelay;
        this.callback = () -> {
            if(animatorSet != null && !animatorSet.isStarted()) {
                animatorSet.start();
            }
            start();
        };
    }

    public void removeCallbacks(){
        removeCallbacks(callback);
    }

    public void start(){
        this.postDelayed(callback, callbackDelay);
    }
}
