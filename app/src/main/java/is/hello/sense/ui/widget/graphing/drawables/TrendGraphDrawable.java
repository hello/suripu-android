package is.hello.sense.ui.widget.graphing.drawables;


import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.api.model.v2.Graph;

public abstract class TrendGraphDrawable extends Drawable {
    protected Graph graph;
    protected float valueScaleFactor = -.6f;
    protected final float[] scaleFactorForward = new float[]{-.6f, -.5f, -.4f, -.3f, -.2f, -.1f, 0, .1f, .2f, .3f, .4f, .5f, .6f, .7f, .8f, .9f, 1f};
    protected final float[] scaleFactorBackward = new float[]{1f, .9f, .8f, .7f, .6f, .5f, .4f, .3f, .2f, .1f, 0, -.1f, -.2f, -.3f, -.4f, -.5f, -.6f};
    protected final float[] animationScaleFactorsForward = new float[]{0, .1f, .2f, .3f, .4f, .5f, .6f, .7f, .8f, .9f, 1f};
    protected final Resources resources;
    protected final AnimatorContext animatorContext;
    protected final Context context;

    public TrendGraphDrawable(@NonNull Graph graph, @NonNull Context context, @NonNull AnimatorContext animatorContext) {
        this.context = context;
        this.resources = context.getResources();
        this.graph = graph;
        this.animatorContext = animatorContext;
    }

    public void showGraphAnimation() {
        ValueAnimator animator = ValueAnimator.ofFloat(scaleFactorForward);
        animator.setDuration(Anime.DURATION_NORMAL);
        animator.setInterpolator(Anime.INTERPOLATOR_DEFAULT);
        animator.addUpdateListener(a -> {
            setScaleFactor((float) a.getAnimatedValue());
        });
        animatorContext.startWhenIdle(animator);

    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }

    @Override
    public int getOpacity() {
        return 0;
    }

    public void setScaleFactor(float scale) {
        valueScaleFactor = scale;
        invalidateSelf();
    }

    public abstract void updateGraph(@NonNull Graph graph);


}
