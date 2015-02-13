package is.hello.sense.ui.widget;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import is.hello.sense.R;

public class TimelineTooltipDrawable extends Drawable {
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path shapePath = new Path();
    private final RectF rect = new RectF();

    private final float cornerRadius;
    private final int triangleWidthHalf;
    private final int triangleHeight;

    public TimelineTooltipDrawable(@NonNull Resources resources) {
        int backgroundColor = resources.getColor(R.color.light_accent);
        fillPaint.setColor(backgroundColor);

        this.cornerRadius = resources.getDimension(R.dimen.button_corner_radius);
        this.triangleHeight = resources.getDimensionPixelSize(R.dimen.timeline_triangle_height);
        this.triangleWidthHalf = resources.getDimensionPixelSize(R.dimen.timeline_triangle_width) / 2;
    }

    @Override
    public void draw(Canvas canvas) {
        int width = canvas.getWidth(),
            height = canvas.getHeight();
        int minX = 0,
            maxX = width + minX,
            midX = maxX / 2;
        int minY = 0,
            maxY = (height + minY) - triangleHeight;

        shapePath.reset();

        rect.set(minX, minY, maxX, maxY);
        shapePath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW);

        shapePath.moveTo(midX - triangleWidthHalf, maxY);
        shapePath.lineTo(midX, maxY + triangleHeight);
        shapePath.lineTo(midX + triangleWidthHalf, maxY);

        canvas.drawPath(shapePath, fillPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        fillPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        fillPaint.setColorFilter(cf);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
