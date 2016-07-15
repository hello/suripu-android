package is.hello.sense.ui.common;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.support.annotation.AnimatorRes;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.util.AnimatorSetHandler;

public class ViewAnimator {

    View animatedView;
    private AnimatorSetHandler animatorSetHandler;
    private AnimatorSet set;

    public ViewAnimator() {
    }

    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @NonNull final ViewGroup container,
                             @LayoutRes final int layoutRes,
                             @IdRes final int animatedViewResId) {
        final View view = inflater.inflate(layoutRes, container, false);
        this.animatedView = view.findViewById(animatedViewResId);

        return view;
    }

    public void onViewCreated(@NonNull final Context context, @AnimatorRes final int animatorRes) {
        set = (AnimatorSet) AnimatorInflater.loadAnimator(context, animatorRes);
        set.setTarget(animatedView);
    }

    public void onResume() {
        resumeAnimation();
    }

    public void onPause() {
        pauseAnimation();
    }

    public void onDestroyView() {
        this.animatorSetHandler = null;
        this.animatedView.clearAnimation();
        this.animatedView = null;
        this.set = null;
    }

    private void pauseAnimation() {
        this.animatorSetHandler.removeCallbacks();
    }

    private void resumeAnimation(){
        if(animatorSetHandler == null) {
            this.animatorSetHandler = new AnimatorSetHandler(800, set);
        }
        animatorSetHandler.start();
    }
}
