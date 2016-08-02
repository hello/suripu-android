package is.hello.sense.ui.common;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.support.annotation.AnimatorRes;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.util.AnimatorSetHandler;

public class ViewAnimator {

    private @Nullable View animatedView;
    private @Nullable AnimatorSetHandler animatorSetHandler;
    private @Nullable AnimatorSet set;
    private final long callbackDelay;

    public ViewAnimator() {
        this(800);
    }

    public ViewAnimator(final long callbackDelay){
        this.callbackDelay = callbackDelay;
    }

    public void setAnimatedView(@NonNull final View animatedView){
        this.animatedView = animatedView;
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
    }

    /**
     * @param animatorSet not required to set {@link this#animatedView} if provided animatorSet
     * has existing target.
     */
    public void onViewCreated(@NonNull final AnimatorSet animatorSet) {
        set = animatorSet;
        setTarget(set, animatedView);
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
        if(animatorSetHandler == null && set != null) {
            this.animatorSetHandler = new AnimatorSetHandler(callbackDelay, set);
        }

        if(animatorSetHandler != null){
            animatorSetHandler.start();
        }
    }

    private void setTarget(@NonNull final AnimatorSet set, @Nullable final View target) {
        if(target != null){
            set.setTarget(target);
        }
    }

}
