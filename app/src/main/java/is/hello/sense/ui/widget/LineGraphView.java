package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import is.hello.sense.R;

public final class LineGraphView extends View {
    private Adapter adapter;
    private int numberOfVerticalLines = 0;
    private int numberOfHorizontalLines = 0;
    private float pointMarkerSize;
    private Drawable fillDrawable;
    private boolean wantsMarkers = true;

    private float cachedPeakY = 0f, cachedTotalX = 0f;
    private float topLineHeight;

    private final Paint gridPaint = new Paint();
    private final Paint markersPaint = new Paint();
    private final Paint topLinePaint = new Paint();

    private final Path topLinePath = new Path();
    private final Path fillPath = new Path();
    private final Path markersPath = new Path();

    private final RectF markerRect = new RectF();

    @SuppressWarnings("UnusedDeclaration")
    public LineGraphView(Context context) {
        super(context);
        initialize(null, 0);
    }

    @SuppressWarnings("UnusedDeclaration")
    public LineGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(attrs, 0);
    }

    @SuppressWarnings("UnusedDeclaration")
    public LineGraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(attrs, defStyleAttr);
    }

    protected void initialize(@Nullable AttributeSet attrs, int defStyleAttr) {
        float density = getResources().getDisplayMetrics().density;
        this.topLineHeight = 3f * density;

        topLinePaint.setStyle(Paint.Style.STROKE);
        topLinePaint.setAntiAlias(true);
        topLinePaint.setStrokeCap(Paint.Cap.ROUND);
        topLinePaint.setStrokeWidth(topLineHeight);

        gridPaint.setAntiAlias(true);

        markersPaint.setAntiAlias(true);

        if (attrs != null) {
            TypedArray styles = getContext().obtainStyledAttributes(attrs, R.styleable.LineGraphView, defStyleAttr, 0);
            setTopLineColor(styles.getColor(R.styleable.LineGraphView_topLineColor, Color.GRAY));
            setGridColor(styles.getColor(R.styleable.LineGraphView_gridColor, Color.LTGRAY));
            setMarkerColor(styles.getColor(R.styleable.LineGraphView_markerColor, Color.DKGRAY));
            int drawableRes = styles.getInt(R.styleable.LineGraphView_fill, -1);
            if (drawableRes != -1) {
                this.fillDrawable = getResources().getDrawable(drawableRes);
            } else {
                this.fillDrawable = new ColorDrawable(Color.WHITE);
            }
            this.pointMarkerSize = styles.getFloat(R.styleable.LineGraphView_markerSize, 5f * density);

            this.numberOfVerticalLines = styles.getInt(R.styleable.LineGraphView_verticalLines, 0);
            this.numberOfHorizontalLines = styles.getInt(R.styleable.LineGraphView_horizontalLines, 0);
            this.wantsMarkers = styles.getBoolean(R.styleable.LineGraphView_wantsMarkers, true);
        } else {
            topLinePaint.setColor(Color.GRAY);
            gridPaint.setColor(Color.LTGRAY);
            markersPaint.setColor(Color.DKGRAY);
            this.pointMarkerSize = 5f * density;
            this.fillDrawable = new ColorDrawable(Color.WHITE);
        }
    }


    //region Drawing

    private float absoluteSegmentX(float sectionWidth, float segmentWidth, int section, int position) {
        return (sectionWidth * section) + (segmentWidth * position);
    }

    private float absoluteSegmentY(float height, int section, int position) {
        return height - (height * (adapter.getPointY(section, position) / this.cachedPeakY));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        topLinePath.reset();
        fillPath.reset();
        markersPath.reset();

        int minX = 0, minY = 0;
        int height = getMeasuredHeight(), width = getMeasuredWidth();

        if (numberOfVerticalLines > 0) {
            float lineDistance = height / numberOfVerticalLines;
            float lineOffset = lineDistance;
            for (int line = 1; line < numberOfVerticalLines; line++) {
                canvas.drawLine(minX, lineOffset, width, lineOffset, gridPaint);
                lineOffset += lineDistance;
            }
        }

        if (numberOfHorizontalLines > 0) {
            float lineDistance = width / numberOfHorizontalLines;
            float lineOffset = lineDistance;
            for (int line = 1; line < numberOfHorizontalLines; line++) {
                canvas.drawLine(lineOffset, minY, lineOffset, height, gridPaint);
                lineOffset += lineDistance;
            }
        }

        if (adapter != null && adapter.getSectionCount() > 0) {
            int sections = adapter.getSectionCount();
            float segmentWidth = width / cachedTotalX;
            float sectionWidth = width / sections;
            float halfPointMarkerArea = pointMarkerSize / 2;

            float firstPointY = absoluteSegmentY(height, 0, 0);
            fillPath.moveTo(minX, firstPointY);
            topLinePath.moveTo(minX, firstPointY);

            for (int section = 0; section < sections; section++) {
                int pointCount = adapter.getPointCount(section);

                for (int position = 0; position < pointCount; position++) {
                    float segmentX = absoluteSegmentX(sectionWidth, segmentWidth, section, position);
                    float segmentY = absoluteSegmentY(height, section, position);

                    topLinePath.lineTo(segmentX, segmentY);
                    fillPath.lineTo(segmentX, segmentY - topLineHeight / 2f);

                    if (wantsMarkers && adapter.wantsMarkerAt(section, position)) {
                        markerRect.set(segmentX - halfPointMarkerArea, segmentY - halfPointMarkerArea,
                                segmentX + halfPointMarkerArea, segmentY + halfPointMarkerArea);
                        markersPath.addOval(markerRect, Path.Direction.CW);
                    }
                }
            }

            fillPath.lineTo(width, height);
            fillPath.lineTo(minX, height);

            if (fillDrawable != null) {
                canvas.save();
                {
                    canvas.clipPath(fillPath);
                    fillDrawable.setBounds(minX, minY, width, height);
                    fillDrawable.draw(canvas);
                }
                canvas.restore();
            }
            canvas.drawPath(topLinePath, topLinePaint);

            if (wantsMarkers) {
                canvas.drawPath(markersPath, markersPaint);
            }
        }
    }

    //endregion


    //region Properties


    public Adapter getAdapter() {
        return adapter;
    }

    public void setAdapter(Adapter adapter) {
        this.adapter = adapter;
        notifyDataChanged();
    }

    public void notifyDataChanged() {
        if (adapter != null) {
            this.cachedPeakY = adapter.getPeakY();
            this.cachedTotalX = 0f;
            for (int section = 0, sections = adapter.getSectionCount(); section < sections; section++) {
                cachedTotalX += adapter.getPointCount(section);
            }
        }

        postInvalidate();
    }

    public void setNumberOfVerticalLines(int numberOfVerticalLines) {
        this.numberOfVerticalLines = numberOfVerticalLines;
        postInvalidate();
    }

    public void setNumberOfHorizontalLines(int numberOfHorizontalLines) {
        this.numberOfHorizontalLines = numberOfHorizontalLines;
        postInvalidate();
    }

    public void setGridColor(int color) {
        gridPaint.setColor(color);
        postInvalidate();
    }

    public void setTopLineColor(int color) {
        topLinePaint.setColor(color);
    }

    public void setMarkerColor(int color) {
        markersPaint.setColor(color);
    }

    public void setFillDrawable(Drawable fillDrawable) {
        this.fillDrawable = fillDrawable;
        postInvalidate();
    }

    public void setPointMarkerSize(float pointMarkerSize) {
        this.pointMarkerSize = pointMarkerSize;
        postInvalidate();
    }

    //endregion


    public interface Adapter {
        int getPeakY();
        int getSectionCount();

        int getPointCount(int section);
        float getPointX(int section, int position);
        float getPointY(int section, int position);
        boolean wantsMarkerAt(int section, int position);
    }
}
