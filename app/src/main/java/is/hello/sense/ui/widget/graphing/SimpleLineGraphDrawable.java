package is.hello.sense.ui.widget.graphing;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.R;
import is.hello.sense.ui.widget.Styles;

public class SimpleLineGraphDrawable extends Drawable implements GraphAdapter.ChangeObserver {
    private final GraphAdapterCache adapterCache = new GraphAdapterCache(GraphAdapterCache.Type.PLAIN);

    private final Paint linePaint = new Paint();
    private final Path linePath = new Path();
    private final Path fillPath = new Path();

    private final float topLineHeight;

    private @Nullable Drawable fillDrawable;


    public SimpleLineGraphDrawable(@NonNull Resources resources) {
        this.topLineHeight = resources.getDimensionPixelSize(R.dimen.view_line_graph_line_size);

        Styles.applyGraphLineParameters(linePaint);
        linePaint.setStrokeWidth(topLineHeight);

        setLineColor(resources.getColor(R.color.graph_fill_color));
        setFillDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {
                resources.getColor(R.color.graph_fill_color_dimmed),
                Color.TRANSPARENT,
        }));
    }


    @Override
    public void draw(Canvas canvas) {
        int sectionCount = adapterCache.getNumberSections();
        if (sectionCount > 0) {
            float halfOfTopLine = topLineHeight / 2f;

            int width = canvas.getWidth();
            int height = (int) (canvas.getHeight() - halfOfTopLine);

            fillPath.reset();
            linePath.reset();

            fillPath.moveTo(0, height);

            float sectionWidth = width / sectionCount;
            for (int section = 0; section < sectionCount; section++) {
                int pointCount = adapterCache.getSectionCount(section);
                if (pointCount == 0)
                    continue;

                float segmentWidth = sectionWidth / (float) pointCount;
                for (int position = 0; position < pointCount; position++) {
                    float segmentX = adapterCache.calculateSegmentX(sectionWidth, segmentWidth, section, position);
                    float segmentY = adapterCache.calculateSegmentY(height, section, position);

                    if (section == 0 && position == 0) {
                        linePath.moveTo(segmentX, segmentY);
                    } else {
                        linePath.lineTo(segmentX, segmentY);
                    }
                    fillPath.lineTo(segmentX, segmentY - halfOfTopLine);

                    if (section == sectionCount - 1 && position == pointCount - 1) {
                        fillPath.lineTo(segmentX + halfOfTopLine, height);
                        fillPath.lineTo(0, height);
                    }
                }
            }

            if (fillDrawable != null) {
                canvas.save();
                {
                    canvas.clipPath(fillPath);
                    fillDrawable.setBounds(0, 0, width, height);
                    fillDrawable.draw(canvas);
                }
                canvas.restore();
            }

            canvas.drawPath(linePath, linePaint);
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
        if (adapterCache.getAdapter() != null) {
            adapterCache.getAdapter().unregisterObserver(this);
        }

        adapterCache.setAdapter(adapter);
        if (adapter != null) {
            adapter.registerObserver(this);
        }

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

    @Override
    public void onGraphAdapterChanged() {
        adapterCache.rebuild();
        invalidateSelf();
    }
}
