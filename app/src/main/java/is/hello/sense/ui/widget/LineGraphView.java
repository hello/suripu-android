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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.ui.animation.PropertyAnimatorProxy;

public final class LineGraphView extends FrameLayout {
    private Adapter adapter;
    private int numberOfVerticalLines = 0;
    private int numberOfHorizontalLines = 0;
    private float pointMarkerSize;
    private Drawable fillDrawable;
    private boolean wantsMarkers = true;

    private float cachedPeakMagnitude = 0f, cachedTotalSegmentCount = 0f;
    private final List<Integer> cachedSectionCounts = new ArrayList<>();

    private float topLineHeight;

    private final Paint gridPaint = new Paint();
    private final Paint markersPaint = new Paint();
    private final Paint topLinePaint = new Paint();

    private final Path topLinePath = new Path();
    private final Path fillPath = new Path();
    private final Path markersPath = new Path();

    private final RectF markerRect = new RectF();

    private TextView highlightedValueText;

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

        this.highlightedValueText = new TextView(getContext());
        highlightedValueText.setGravity(Gravity.CENTER);
        highlightedValueText.setMinimumWidth((int) (50f * density));
        highlightedValueText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        highlightedValueText.setBackgroundResource(R.drawable.timestamp_background);
        int padding = getResources().getDimensionPixelSize(R.dimen.gap_medium);
        highlightedValueText.setPadding(padding, padding, padding, padding);
        highlightedValueText.setVisibility(INVISIBLE);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        int margin = getResources().getDimensionPixelSize(R.dimen.gap_medium);
        layoutParams.setMargins(margin, margin, margin, margin);
        addView(highlightedValueText, layoutParams);

        setWillNotDraw(false);
    }


    //region Drawing

    private float getSectionWidth() {
        if (adapter != null) {
            return getMeasuredWidth() / adapter.getSectionCount();
        } else {
            return 0f;
        }
    }

    private float getSegmentWidth() {
        if (adapter != null) {
            return getMeasuredWidth() / cachedTotalSegmentCount;
        } else {
            return 0f;
        }
    }

    private float absoluteSegmentX(float sectionWidth, float segmentWidth, int section, int position) {
        return (sectionWidth * section) + (segmentWidth * position);
    }

    private float absoluteSegmentY(float height, int section, int position) {
        return height - (height * (adapter.getMagnitudeAt(section, position) / this.cachedPeakMagnitude));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int minX = 0, minY = 0;
        int height = getMeasuredHeight() - minY, width = getMeasuredWidth() - minX;

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

        if (adapter != null && cachedSectionCounts.size() > 0) {
            topLinePath.reset();
            fillPath.reset();
            markersPath.reset();

            if (adapter != null && cachedSectionCounts.size() > 0) {
                int sections = cachedSectionCounts.size();
                float segmentWidth = width / cachedTotalSegmentCount;
                float sectionWidth = width / sections;
                float halfPointMarkerArea = pointMarkerSize / 2;
                for (int section = 0; section < sections; section++) {
                    int pointCount = cachedSectionCounts.get(section);

                    for (int position = 0; position < pointCount; position++) {
                        if (section == 0 && position == 0) {
                            float firstPointY = absoluteSegmentY(height, section, position);
                            fillPath.moveTo(minX, firstPointY);
                            topLinePath.moveTo(minX, firstPointY);
                        }

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
            }

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
        this.cachedSectionCounts.clear();

        if (adapter != null) {
            this.cachedPeakMagnitude = adapter.getPeakMagnitude();
            this.cachedTotalSegmentCount = 0f;
            for (int section = 0, sections = adapter.getSectionCount(); section < sections; section++) {
                int count = adapter.getSectionPointCount(section);
                cachedTotalSegmentCount += count;
                cachedSectionCounts.add(count);
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


    //region Event Handling

    private int calculateSegmentCountInRange(int start, int end) {
        int count = 0;
        for (int section = start; section < end; section++) {
            count += cachedSectionCounts.get(section);
        }
        return count;
    }

    private int getSectionAtX(float x) {
        int limit = cachedSectionCounts.size();
        return Math.min(limit - 1, Math.round(x / getSectionWidth()));
    }

    private int getSegmentAtX(int section, float x) {
        int sectionMax =  cachedSectionCounts.get(section) - 1;
        return Math.min(sectionMax, Math.round(x / getSegmentWidth()) - calculateSegmentCountInRange(0, section - 1));
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (adapter == null || cachedSectionCounts.size() == 0)
            return false;

        float x = event.getX();
        int section = getSectionAtX(x);
        int segment = getSegmentAtX(section, x);
        highlightedValueText.setText(adapter.getFormattedMagnitudeAt(section, segment));

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                highlightedValueText.setAlpha(0f);
                highlightedValueText.setScaleX(0.5f);
                highlightedValueText.setScaleY(0.5f);
                highlightedValueText.setVisibility(VISIBLE);
                PropertyAnimatorProxy.animate(highlightedValueText)
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setApplyChangesToView(true)
                        .start();
                break;

            case MotionEvent.ACTION_MOVE:
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                PropertyAnimatorProxy.animate(highlightedValueText)
                        .alpha(0f)
                        .scaleX(0.5f)
                        .scaleY(0.5f)
                        .setApplyChangesToView(true)
                        .setOnAnimationCompleted(finished -> highlightedValueText.setVisibility(INVISIBLE))
                        .start();
                break;
        }
        return true;
    }


    //endregion

    public interface Adapter {
        int getPeakMagnitude();
        int getSectionCount();
        int getSectionPointCount(int section);
        float getMagnitudeAt(int section, int position);
        @NonNull CharSequence getFormattedMagnitudeAt(int section, int position);
        boolean wantsMarkerAt(int section, int position);
    }
}
