package is.hello.sense.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import is.hello.sense.R;

public class RoundedRelativeLayout extends RelativeLayout {
    private final Path clippingPath = new Path();
    private final RectF clippingRect = new RectF();

    private float[] cornerRadii;

    public RoundedRelativeLayout(Context context) {
        this(context, null);
    }

    public RoundedRelativeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundedRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        float radius = context.getResources().getDimension(R.dimen.raised_item_corner_radius);
        cornerRadii = new float[] {
                radius, radius, radius, radius,
                0f, 0f, 0f, 0f
        };

        setWillNotDraw(false);
    }


    @Override
    public void draw(@NonNull Canvas canvas) {
        canvas.save();
        canvas.clipPath(clippingPath);
        super.draw(canvas);
        canvas.restore();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);

        clippingPath.reset();
        clippingRect.set(0f, 0f, w, h);
        clippingPath.addRoundRect(clippingRect, cornerRadii, Path.Direction.CW);
    }


    public void setCornerRadii(@NonNull float[] cornerRadii) {
        this.cornerRadii = cornerRadii;
        invalidate();
    }
}
