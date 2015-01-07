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

public class LineGraphDrawable extends GraphDrawable {
    private final Paint linePaint = new Paint();
    private final Path linePath = new Path();
    private final Path fillPath = new Path();

    private final float topLineHeight;
    private final Drawable fillDrawable;


    public LineGraphDrawable(@NonNull Resources resources) {
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

            if (sectionCount == 1 && adapterCache.getSectionCount(0) == 1) {
                linePath.moveTo(0f, minY);
                linePath.lineTo(width, minY);

                fillPath.addRect(0f, minY + halfOfTopLine, width, height + bottomInset, Path.Direction.CW);
            } else {
                fillPath.moveTo(0f, minY + height + bottomInset);

                float sectionWidth = width / sectionCount;
                for (int section = 0; section < sectionCount; section++) {
                    int pointCount = adapterCache.getSectionCount(section);
                    if (pointCount == 0)
                        continue;

                    float segmentWidth = sectionWidth / (float) pointCount;
                    for (int position = 0; position < pointCount; position++) {
                        float segmentX = adapterCache.calculateSegmentX(sectionWidth, segmentWidth, section, position);
                        float segmentY = minY + adapterCache.calculateSegmentY(height, section, position);

                        if (section == 0 && position == 0) {
                            linePath.moveTo(segmentX, segmentY);
                        } else {
                            linePath.lineTo(segmentX, segmentY);
                        }
                        fillPath.lineTo(segmentX, segmentY - halfOfTopLine);

                        if (section == sectionCount - 1 && position == pointCount - 1) {
                            fillPath.lineTo(segmentX + halfOfTopLine, minY + height + bottomInset);
                            fillPath.lineTo(0f, minY + height + bottomInset);
                        }
                    }
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
