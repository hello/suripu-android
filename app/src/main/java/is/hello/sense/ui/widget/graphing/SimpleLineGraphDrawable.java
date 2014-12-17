package is.hello.sense.ui.widget.graphing;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.R;
import is.hello.sense.ui.widget.Styles;

public class SimpleLineGraphDrawable extends Drawable {
    private final GraphAdapterCache adapterCache = new GraphAdapterCache();

    private final Paint linePaint = new Paint();
    private final Path fillPath = new Path();

    private final float topLineHeight;

    private @Nullable Drawable fillDrawable;


    public SimpleLineGraphDrawable(@NonNull Resources resources) {
        this.topLineHeight = resources.getDimensionPixelSize(R.dimen.divider_height);

        Styles.applyGraphLineParameters(linePaint);
        linePaint.setStrokeWidth(topLineHeight);
    }


    @Override
    public void draw(Canvas canvas) {
        int sectionCount = adapterCache.getNumberSections();
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        if (sectionCount > 0) {
            fillPath.reset();

            float sectionWidth = width / sectionCount;
            for (int section = 0; section < sectionCount; section++) {
                int pointCount = adapterCache.getSectionCount(section);
                if (pointCount == 0)
                    continue;

                float segmentWidth = sectionWidth / (float) pointCount;

                Path sectionPath = adapterCache.getSectionLinePath(section);
                sectionPath.reset();
                for (int position = 0; position < pointCount; position++) {
                    float segmentX = adapterCache.calculateSegmentX(sectionWidth, segmentWidth, section, position);
                    float segmentY = adapterCache.calculateSegmentY(height, section, position);

                    if (position == 0) {
                        fillPath.moveTo(0, segmentY);
                        sectionPath.moveTo(segmentX, segmentY);
                    } else {
                        sectionPath.lineTo(segmentX, segmentY);
                    }
                    fillPath.lineTo(segmentX, segmentY - topLineHeight / 2f);
                }

                if (section < sectionCount - 1) {
                    float closingSegmentY = adapterCache.calculateSegmentY(height, section + 1, 0);
                    sectionPath.lineTo(sectionWidth * (section + 1), closingSegmentY);
                }
            }

            fillPath.lineTo(width, height);
            fillPath.lineTo(0, height);

            if (fillDrawable != null) {
                canvas.save();
                {
                    canvas.clipPath(fillPath);
                    fillDrawable.setBounds(0, 0, width, height);
                    fillDrawable.draw(canvas);
                }
                canvas.restore();
            }

            for (int section = 0; section < sectionCount; section++) {
                Path sectionPath = adapterCache.getSectionLinePath(section);
                canvas.drawPath(sectionPath, linePaint);
            }
        }
    }


    //region Properties

    @Override
    public void setAlpha(int alpha) {
        linePaint.setAlpha(alpha);

        if (fillDrawable != null) {
            fillDrawable.setAlpha(alpha);
        }

        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        linePaint.setColorFilter(colorFilter);

        if (fillDrawable != null) {
            fillDrawable.setColorFilter(colorFilter);
        }

        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }


    public void setAdapter(@Nullable GraphAdapter adapter) {
        adapterCache.setAdapter(adapter);
        invalidateSelf();
    }

    public void setLineColor(int color) {
        linePaint.setColor(color);
        invalidateSelf();
    }

    public void setFillDrawable(@Nullable Drawable fillDrawable) {
        this.fillDrawable = fillDrawable;
        invalidateSelf();
    }

    //endregion
}
