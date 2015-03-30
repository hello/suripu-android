package is.hello.sense.ui.widget;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;

import is.hello.sense.R;

public class TimelineHeaderDrawable extends SelectorView.SelectionAwareDrawable {
    //region Drawing Constants

    private final int barHeight;
    private final int gradientHeight;
    private final int triangleHeight;
    private final int triangleWidth;
    private final int stroke;

    //endregion


    //region Drawing Objects

    private final Path fillPath = new Path();
    private final Paint fillPaint = new Paint();

    private final Path strokePath = new Path();
    private final Paint strokePaint = new Paint();

    private final GradientDrawable topGradient;

    //endregion


    public TimelineHeaderDrawable(@NonNull Resources resources) {
        this.barHeight = resources.getDimensionPixelSize(R.dimen.timeline_header_tab_height);
        this.gradientHeight = resources.getDimensionPixelSize(R.dimen.timeline_header_gradient_height);
        this.triangleHeight = resources.getDimensionPixelSize(R.dimen.timeline_triangle_height);
        this.triangleWidth = resources.getDimensionPixelSize(R.dimen.timeline_triangle_width);
        this.stroke = resources.getDimensionPixelSize(R.dimen.timeline_header_stroke_width);

        fillPaint.setAntiAlias(true);
        fillPaint.setColor(Color.WHITE);

        strokePaint.setAntiAlias(true);
        strokePaint.setStrokeWidth(stroke);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(resources.getColor(R.color.timeline_tabs_border));

        this.topGradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {
            resources.getColor(R.color.timeline_tabs_gradient_start),
            resources.getColor(R.color.timeline_tabs_gradient_end),
        });
    }


    @Override
    public void draw(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        int minX = 0,
            maxX = minX + width;
        int minY = height - barHeight,
            maxY = minY + barHeight;

        fillPath.reset();
        strokePath.reset();

        strokePath.moveTo(minX, minY);
        fillPath.moveTo(minX, minY);

        if (isSelectionValid()) {
            int itemWidth = (width / numberOfItems);
            int triangleCenter = (itemWidth * (selectedIndex + 1)) - (itemWidth / 2);
            int triangleStart = triangleCenter - triangleWidth / 2;
            int triangleEnd = triangleCenter + triangleWidth / 2;
            fillPath.lineTo(triangleStart, minY);
            fillPath.lineTo(triangleCenter, minY + triangleHeight);
            fillPath.lineTo(triangleEnd, minY);
        }

        fillPath.lineTo(maxX, minY);
        strokePath.set(fillPath);

        fillPath.lineTo(maxX, maxY);
        fillPath.lineTo(minX, maxY);
        fillPath.lineTo(minX, minY);

        topGradient.setBounds(minX, minY - gradientHeight, maxX, minY + triangleHeight);
        topGradient.draw(canvas);

        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(strokePath, strokePaint);
        canvas.drawRect(minX, maxY - stroke, maxX, maxY, strokePaint);
    }


    //region Properties

    @Override
    public void setAlpha(int alpha) {
        fillPaint.setAlpha(alpha);
        strokePaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        fillPaint.setColorFilter(cf);
        strokePaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    //endregion
}
