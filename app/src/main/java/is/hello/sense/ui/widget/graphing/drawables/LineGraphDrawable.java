package is.hello.sense.ui.widget.graphing.drawables;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Styles;

public class LineGraphDrawable extends GraphDrawable {
    private final Resources resources;

    private final Paint linePaint = new Paint();
    private final Path linePath = new Path();
    private final Path fillPath = new Path();

    private final float topLineHeight;
    private final Drawable fillDrawable;

    private final float pointSizeHalf;
    private @Nullable MarkerState markers;


    public LineGraphDrawable(@NonNull Resources resources, @NonNull Drawable fillDrawable) {
        this.resources = resources;

        this.topLineHeight = resources.getDimensionPixelSize(R.dimen.series_graph_line_size);
        this.pointSizeHalf = resources.getDimensionPixelSize(R.dimen.graph_point_size) / 2f;

        Styles.applyGraphLineParameters(linePaint);
        //noinspection SuspiciousNameCombination
        linePaint.setStrokeWidth(topLineHeight);
        linePaint.setColor(resources.getColor(R.color.graph_stroke_color));
        this.fillDrawable = fillDrawable;
    }

    public LineGraphDrawable(@NonNull Resources resources) {
        this(resources, Styles.createGraphFillGradientDrawable(resources));
    }


    @Override
    public void draw(Canvas canvas) {
        int sectionCount = adapterCache.getNumberSections();
        float halfOfTopLine = topLineHeight / 2f;

        int minY = (int) Math.ceil(halfOfTopLine) + topInset;
        int width = canvas.getWidth();
        int height = canvas.getHeight() - minY - bottomInset;

        if (markers != null) {
            minY += pointSizeHalf;
            height -= pointSizeHalf;
        }

        if (sectionCount > 0) {
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
                            linePath.lineTo(width, segmentY);

                            fillPath.lineTo(width, segmentY - halfOfTopLine);
                            fillPath.lineTo(width, minY + height + bottomInset);
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

            if (markers != null) {
                markers.draw(canvas, minY, width, height);
            }
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

    public void setMarkers(@Nullable Marker[] markers) {
        if (markers != null && markers.length > 0) {
            this.markers = new MarkerState(resources, markers);
        } else {
            this.markers = null;
        }
        invalidateSelf();
    }

    //endregion


    private final class MarkerState {
        private final Marker[] points;

        private final float edgeInset;

        private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        private final RectF markerRect = new RectF();
        private final Rect textBounds = new Rect();
        private final Paint.FontMetrics fontMetrics = new Paint.FontMetrics();

        private MarkerState(@NonNull Resources resources, @NonNull Marker[] points) {
            this.points = points;

            this.edgeInset = resources.getDimensionPixelSize(R.dimen.gap_tiny);

            int shadowRadius = resources.getDimensionPixelSize(R.dimen.series_graph_shadow_radius);
            textPaint.setShadowLayer(shadowRadius, 0f, 0f, Color.WHITE);
            textPaint.setTextSize(resources.getDimensionPixelSize(R.dimen.text_size_graph_footer));
            textPaint.getFontMetrics(fontMetrics);
        }

        private void draw(@NonNull Canvas canvas, int minY, int width, int height) {
            float sectionWidth = adapterCache.calculateSectionWidth(width);
            for (Marker marker : points) {
                float segmentWidth = adapterCache.calculateSegmentWidth(width, marker.section);

                float segmentX = adapterCache.calculateSegmentX(sectionWidth, segmentWidth, marker.section, marker.segment);
                float segmentY = minY + adapterCache.calculateSegmentY(height, marker.section, marker.segment);

                markerPaint.setColor(marker.color);

                markerRect.set(segmentX - pointSizeHalf, segmentY - pointSizeHalf,
                        segmentX + pointSizeHalf, segmentY + pointSizeHalf);
                canvas.drawOval(markerRect, markerPaint);

                if (!TextUtils.isEmpty(marker.value)) {
                    textPaint.getTextBounds(marker.value, 0, marker.value.length(), textBounds);

                    float textX = segmentX - textBounds.centerX();
                    float textY = segmentY - textBounds.height();
                    if (textX < 0f) {
                        textX = edgeInset + pointSizeHalf;
                        textY = segmentY + textBounds.centerY();
                    } else if (textX > width) {
                        textX = width - edgeInset - textBounds.width();
                    }

                    if (textY < 0f) {
                        float fontSpacing = fontMetrics.leading + fontMetrics.bottom + fontMetrics.descent;
                        textY = segmentY + textBounds.height() + pointSizeHalf + fontSpacing;
                    }

                    textPaint.setColor(marker.color);
                    canvas.drawText(marker.value, textX, textY, textPaint);
                }
            }
        }
    }

    public static class Marker {
        public final int section;
        public final int segment;
        public final int color;
        public final @Nullable String value;

        public Marker(int section,
                      int segment,
                      int color,
                      @Nullable String value) {
            this.section = section;
            this.segment = segment;
            this.color = color;
            this.value = value;
        }

        @Override
        public String toString() {
            return "Marker{" +
                    "section=" + section +
                    ", segment=" + segment +
                    ", color=" + color +
                    ", value='" + value + '\'' +
                    '}';
        }
    }
}
