package is.hello.sense.ui.widget.graphing.drawables;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Logger;

public class HistogramGraphDrawable extends GraphDrawable {
    private final Paint linePaint = new Paint();
    private final Path linePath = new Path();
    private final Path fillPath = new Path();

    private final float topLineHeight;
    private final Drawable fillDrawable;

    public HistogramGraphDrawable(@NonNull Resources resources) {
        this.topLineHeight = resources.getDimensionPixelSize(R.dimen.series_graph_line_size);

        Styles.applyGraphLineParameters(linePaint);
        linePaint.setStrokeWidth(topLineHeight);

        linePaint.setColor(resources.getColor(R.color.graph_fill_color));
        this.fillDrawable = Styles.createGraphFillDrawable(resources);
    }


    @Override
    public void draw(Canvas canvas) {
        int sectionCount = adapterCache.getNumberSections();
        if (sectionCount > 0) {
            float halfOfTopLine = topLineHeight / 2f;

            int minY = Math.round(halfOfTopLine) + topInset;
            int width = canvas.getWidth();
            int height = canvas.getHeight() - minY - bottomInset;

            fillPath.reset();
            linePath.reset();

            fillPath.moveTo(0, minY + height + bottomInset);

            float sectionWidth = width / sectionCount;
            for (int section = 0; section < sectionCount; section++) {
                int pointCount = adapterCache.getSectionCount(section);
                if (pointCount != 1) {
                    Logger.error(getClass().getSimpleName(), "Invalid adapter given to histogram graph drawable! Each section must have exactly one point.");
                    continue;
                }

                float sectionX = adapterCache.calculateSegmentX(sectionWidth, 0, section, 0);
                float sectionY = minY + adapterCache.calculateSegmentY(height, section, 0);

                if (section == 0) {
                    linePath.moveTo(sectionX, sectionY);
                } else {
                    linePath.lineTo(sectionX, sectionY);
                }

                linePath.lineTo(sectionX + sectionWidth, sectionY);

                fillPath.lineTo(sectionX, sectionY - halfOfTopLine);
                fillPath.lineTo(sectionX + sectionWidth, sectionY - halfOfTopLine);

                if (section == sectionCount - 1) {
                    fillPath.lineTo(sectionX + sectionWidth + halfOfTopLine, minY + height + bottomInset);
                    fillPath.lineTo(0, minY + height + bottomInset);
                }
            }

            canvas.save();
            {
                canvas.clipPath(fillPath);
                fillDrawable.setBounds(0, minY, width, minY + height + bottomInset);
                fillDrawable.draw(canvas);
            }
            canvas.restore();

            canvas.drawPath(linePath, linePaint);
        }
    }


    //region Properties

    @Override
    public void setAlpha(int alpha) {
        linePaint.setAlpha(alpha);
        fillDrawable.setAlpha(alpha);

        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        linePaint.setColorFilter(colorFilter);
        fillDrawable.setColorFilter(colorFilter);

        invalidateSelf();
    }

    //endregion
}
