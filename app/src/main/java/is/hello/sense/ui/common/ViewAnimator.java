package is.hello.sense.ui.common;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.support.annotation.AnimatorRes;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;

import is.hello.sense.util.AnimatorSetHandler;

public class ViewAnimator {

    private @Nullable final Interpolator interpolator;
    private @Nullable View animatedView;
    private @Nullable AnimatorSetHandler animatorSetHandler;
    private @Nullable AnimatorSet set;
    private final long callbackDelay;
    private int repeatCount;

    public ViewAnimator() {
        this(800);
    }

    public ViewAnimator(final long callbackDelay){
        this(callbackDelay, null);
    }

    public ViewAnimator(final long callbackDelay, @Nullable final Interpolator interpolator){
        this.callbackDelay = callbackDelay;
        this.interpolator = interpolator;
        this.repeatCount = AnimatorSetHandler.LOOP_ANIMATION;
    }

    public void setAnimatedView(@NonNull final View animatedView){
        this.animatedView = animatedView;
    }

    public void setRepeatCount(final int repeatCount){
        this.repeatCount = repeatCount;
    }

    public View inflateView(@NonNull final LayoutInflater inflater,
                            @NonNull final ViewGroup container,
                            @LayoutRes final int layoutRes,
                            @IdRes final int animatedViewResId) {
        final View view = inflater.inflate(layoutRes, container, false);
        this.animatedView = view.findViewById(animatedViewResId);

        return view;
    }

    public void onViewCreated(@NonNull final Context context, @AnimatorRes final int animatorRes) {
        set = (AnimatorSet) AnimatorInflater.loadAnimator(context, animatorRes);
        setTarget(set, animatedView);
        setInterpolator(set, interpolator);
    }

    /**
     * @param animatorSet not required to set {@link this#animatedView} if provided animatorSet
     * has existing target.
     */
    public void onViewCreated(@NonNull final AnimatorSet animatorSet) {
        set = animatorSet;
        setTarget(set, animatedView);
        setInterpolator(set, interpolator);
    }

    public void onResume() {
        resumeAnimation();
    }

    public void onPause() {
        pauseAnimation();
    }

    public void onDestroyView() {
        this.animatorSetHandler = null;
        if(animatedView != null) {
            this.animatedView.clearAnimation();
            this.animatedView = null;
        }
        if(set != null) {
            this.set.end();
            this.set.removeAllListeners();
            this.set = null;
        }
    }

    public void resetAnimation(@NonNull final AnimatorSet animatorSet){
        onPause();
        onDestroyView();
        onViewCreated(animatorSet);
        onResume();
    }

    private void pauseAnimation() {
        if(animatorSetHandler != null) {
            this.animatorSetHandler.removeCallbacks();
        }
    }

    private void resumeAnimation(){
        if(set == null){
            Log.d(ViewAnimator.class.getName(), "resumeAnimation: AnimatorSet set is null so no animation will play.");
            return;
        }
        if(animatorSetHandler == null) {
            this.animatorSetHandler = new AnimatorSetHandler(callbackDelay, repeatCount, set);
        }
        animatorSetHandler.start();
    }

    private void setTarget(@NonNull final AnimatorSet set, @Nullable final View target) {
        if(target != null){
            set.setTarget(target);
        }
    }

    private void setInterpolator(@NonNull final AnimatorSet set, @Nullable final Interpolator interpolator){
        if(interpolator != null){
            set.setInterpolator(interpolator);
        }
    }

}
