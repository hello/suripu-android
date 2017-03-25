package is.hello.sense.ui.handholding;

import android.animation.Animator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import is.hello.sense.R;

public class InteractionView extends View {
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF ovalRect = new RectF();

    private
    @Nullable
    Animator currentAnimation;

    //region Lifecycle

    public InteractionView(@NonNull final Context context) {
        this(context, null);
    }

    public InteractionView(@NonNull final Context context,
                           @Nullable final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public InteractionView(@NonNull final Context context,
                           @Nullable final AttributeSet attrs,
                           final int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final Resources resources = getResources();

        final int color = ContextCompat.getColor(context, R.color.tutorial_interaction_view);
        fillPaint.setColor(color);
        fillPaint.setAlpha(60);

        borderPaint.setColor(color);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(resources.getDimensionPixelSize(R.dimen.view_interaction_stroke));

        final int area = resources.getDimensionPixelSize(R.dimen.view_interaction_area);
        setMinimumWidth(area);
        setMinimumHeight(area);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        stopAnimation();
    }

    //endregion


    //region Drawing

    @Override
    protected void onDraw(final Canvas canvas) {
        final int width = canvas.getWidth();
        final int height = canvas.getHeight();

        ovalRect.set(0f, 0f, width, height);
        canvas.drawOval(ovalRect, fillPaint);

        float inset = borderPaint.getStrokeWidth() / 2f;
        ovalRect.inset(inset, inset);
        canvas.drawOval(ovalRect, borderPaint);
    }

    //endregion


    //region Animation

    public void playTutorial(@NonNull final Tutorial tutorial) {
        startAnimation(tutorial.createAnimation(this));
    }

    public void startAnimation(@NonNull final Animator animation) {
        stopAnimation();

        this.currentAnimation = animation;
        currentAnimation.start();
    }

    public void stopAnimation() {
        if (currentAnimation != null) {
            currentAnimation.cancel();
            this.currentAnimation = null;
        }
    }

    //endregion
}
