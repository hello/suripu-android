package is.hello.sense.ui.widget.graphing.drawables;


import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.ui.widget.graphing.TrendGraphView;

public abstract class TrendGraphDrawable extends Drawable {
    protected final float maxScaleFactor = 1f;
    protected final float minScaleFactor = -.6f;
    protected final float maxAnimationFactor = 1f;
    protected final float minAnimationFactor = 0;
    protected Graph graph;
    protected float valueScaleFactor = -.6f;
    protected final Resources resources;
    protected final AnimatorContext animatorContext;
    protected final Context context;

    public TrendGraphDrawable(@NonNull Context context, @NonNull Graph graph, @NonNull AnimatorContext animatorContext) {
        this.context = context;
        this.resources = context.getResources();
        this.graph = graph;
        this.animatorContext = animatorContext;
    }

    public TrendGraphDrawable(@NonNull Context context, @NonNull AnimatorContext animatorContext) {
        this.context = context;
        this.resources = context.getResources();
        this.animatorContext = animatorContext;
    }

    public void showGraphAnimation() {
        ValueAnimator animator = ValueAnimator.ofFloat(minScaleFactor, maxScaleFactor);
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
        return PixelFormat.TRANSLUCENT;
    }

    public void setScaleFactor(float scale) {
        valueScaleFactor = scale;
        invalidateSelf();
    }

    public Graph getGraph(){
        return graph;
    }

    public abstract void updateGraph(@NonNull Graph graph);


}
