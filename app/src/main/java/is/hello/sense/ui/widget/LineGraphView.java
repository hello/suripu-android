package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.animation.PropertyAnimatorProxy;
import is.hello.sense.ui.common.ViewUtil;
import is.hello.sense.util.Constants;

public final class LineGraphView extends FrameLayout {
    private Adapter adapter;
    private int numberOfLines = 0;
    private Drawable fillDrawable;
    private Drawable gridDrawable;
    private boolean wantsHeaders = true;
    private boolean wantsFooters = true;

    private float baseMagnitude = 0f;
    private float peakMagnitude = 0f;
    private final SparseIntArray sectionCounts = new SparseIntArray();

    private float topLineHeight;

    private final Paint topLinePaint = new Paint();
    private final Paint headerTextPaint = new Paint();
    private final Paint footerTextPaint = new Paint();

    private final Path topLinePath = new Path();
    private final Path fillPath = new Path();
    private final Path markersPath = new Path();

    private final Rect textRect = new Rect();

    private int headerFooterPadding;

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
        Resources resources = getResources();

        topLinePaint.setStyle(Paint.Style.STROKE);
        topLinePaint.setAntiAlias(true);
        topLinePaint.setStrokeJoin(Paint.Join.ROUND);
        topLinePaint.setStrokeCap(Paint.Cap.ROUND);
        topLinePaint.setStrokeWidth(topLineHeight);

        headerTextPaint.setAntiAlias(true);
        headerTextPaint.setSubpixelText(true);
        footerTextPaint.setAntiAlias(true);
        footerTextPaint.setSubpixelText(true);

        setHeaderTextSize(resources.getDimensionPixelOffset(R.dimen.text_size_small));
        setHeaderTypeface(Typeface.createFromAsset(getResources().getAssets(), Constants.TYPEFACE_HEAVY));

        setFooterTextSize(resources.getDimensionPixelOffset(R.dimen.text_size_medium));
        setFooterTypeface(Typeface.createFromAsset(getResources().getAssets(), Constants.TYPEFACE_LIGHT));

        if (attrs != null) {
            TypedArray styles = getContext().obtainStyledAttributes(attrs, R.styleable.LineGraphView, defStyleAttr, 0);
            setTopLineColor(styles.getColor(R.styleable.LineGraphView_topLineColor, resources.getColor(R.color.grey)));
            setTopLineHeight(styles.getDimensionPixelOffset(R.styleable.LineGraphView_topLineHeight, resources.getDimensionPixelSize(R.dimen.divider_height)));
            setFillDrawable(styles.getDrawable(R.styleable.LineGraphView_fill));

            this.numberOfLines = styles.getInt(R.styleable.LineGraphView_lineCount, 0);
            setGridDrawable(styles.getDrawable(R.styleable.LineGraphView_gridDrawable));

            this.wantsHeaders = styles.getBoolean(R.styleable.LineGraphView_wantsHeaders, true);
            this.wantsFooters = styles.getBoolean(R.styleable.LineGraphView_wantsFooters, true);
        } else {
            setTopLineColor(resources.getColor(R.color.grey));
            setTopLineHeight(resources.getDimensionPixelSize(R.dimen.divider_height));
        }

        this.headerFooterPadding = getResources().getDimensionPixelSize(R.dimen.gap_medium);

        this.highlightedValueText = new TextView(getContext());
        highlightedValueText.setGravity(Gravity.CENTER);
        highlightedValueText.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelOffset(R.dimen.text_size_small));
        highlightedValueText.setBackgroundResource(R.drawable.timestamp_background);
        highlightedValueText.setTextColor(Color.WHITE);
        int padding = getResources().getDimensionPixelSize(R.dimen.gap_small);
        highlightedValueText.setPadding(padding, padding, padding, padding);
        highlightedValueText.setVisibility(INVISIBLE);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        int margin = getResources().getDimensionPixelSize(R.dimen.gap_small);
        layoutParams.setMargins(margin, margin, margin, margin);
        addView(highlightedValueText, layoutParams);

        setWillNotDraw(false);
    }


    //region Drawing

    private float getSectionWidth() {
        return getMeasuredWidth() / sectionCounts.size();
    }

    private float getSegmentWidth(int section) {
        return getSectionWidth() / sectionCounts.get(section);
    }

    private float absoluteSegmentX(float sectionWidth, float segmentWidth, int section, int position) {
        return (sectionWidth * section) + (segmentWidth * position);
    }

    private float absoluteSegmentY(float height, int section, int position) {
        float magnitude = adapter.getMagnitudeAt(section, position);
        float percentage = (magnitude - baseMagnitude) / (peakMagnitude - baseMagnitude);
        return Math.round(height * percentage);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int sectionCount = sectionCounts.size();
        int minX = 0, minY = 0;
        int height = getMeasuredHeight() - minY, width = getMeasuredWidth() - minX;

        if (gridDrawable != null && numberOfLines > 0) {
            int lineDistance = width / numberOfLines;
            int lineOffset = lineDistance;
            for (int line = 1; line < numberOfLines; line++) {
                gridDrawable.setBounds(lineOffset, minY, lineOffset + 1, height);
                gridDrawable.draw(canvas);
                lineOffset += lineDistance;
            }
        }

        if (adapter != null && sectionCount > 0) {
            topLinePath.reset();
            fillPath.reset();
            markersPath.reset();

            int headerHeight = (int) headerTextPaint.getTextSize() + (headerFooterPadding * 2);
            if (wantsHeaders) {
                minY += headerHeight;
                height -= headerHeight;
            }

            int footerHeight = (int) footerTextPaint.getTextSize() + (headerFooterPadding * 2);
            if (wantsFooters) {
                height -= footerHeight;
            }

            float sectionWidth = width / sectionCount;
            for (int section = 0; section < sectionCount; section++) {
                int pointCount = sectionCounts.get(section);
                if (pointCount == 0)
                    continue;

                float segmentWidth = sectionWidth / (float) pointCount;

                if (wantsHeaders) {
                    headerTextPaint.setColor(adapter.getSectionTextColor(section));

                    String text = adapter.getSectionHeader(section);
                    headerTextPaint.getTextBounds(text, 0, text.length(), textRect);

                    float sectionMidX = (sectionWidth * section) + (sectionWidth / 2);
                    float textX = Math.round(sectionMidX - textRect.centerX());
                    float textY = Math.round((headerHeight / 2) - textRect.centerY());
                    canvas.drawText(text, textX, textY, headerTextPaint);
                }

                if (wantsFooters) {
                    footerTextPaint.setColor(adapter.getSectionTextColor(section));

                    String text = adapter.getSectionFooter(section);
                    footerTextPaint.getTextBounds(text, 0, text.length(), textRect);

                    float sectionMidX = (sectionWidth * section) + (sectionWidth / 2);
                    float textX = Math.round(sectionMidX - textRect.centerX());
                    float textY = Math.round((minY + height) + ((footerHeight / 2) - textRect.centerY()));
                    canvas.drawText(text, textX, textY, footerTextPaint);
                }

                for (int position = 0; position < pointCount; position++) {
                    if (section == 0 && position == 0) {
                        float firstPointY = minY + absoluteSegmentY(height, section, position);
                        fillPath.moveTo(minX, firstPointY);
                        topLinePath.moveTo(minX, firstPointY);
                    }

                    float segmentX = absoluteSegmentX(sectionWidth, segmentWidth, section, position);
                    float segmentY = minY + absoluteSegmentY(height, section, position);

                    topLinePath.lineTo(segmentX, segmentY);
                    fillPath.lineTo(segmentX, segmentY - topLineHeight / 2f);
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
        this.sectionCounts.clear();

        if (adapter != null) {
            this.baseMagnitude = adapter.getBaseMagnitude();
            this.peakMagnitude = adapter.getPeakMagnitude();
            for (int section = 0, sections = adapter.getSectionCount(); section < sections; section++) {
                int count = adapter.getSectionPointCount(section);
                sectionCounts.append(section, count);
            }
        }

        invalidate();
    }

    public void setNumberOfLines(int numberOfLines) {
        this.numberOfLines = numberOfLines;
        invalidate();
    }

    public void setGridDrawable(Drawable gridDrawable) {
        this.gridDrawable = gridDrawable;
        invalidate();
    }

    public void setTopLineColor(int color) {
        topLinePaint.setColor(color);
    }

    public void setTopLineHeight(int height) {
        this.topLineHeight = height;
        invalidate();
    }

    public void setFillDrawable(Drawable fillDrawable) {
        this.fillDrawable = fillDrawable;
        invalidate();
    }

    public void setHeaderTypeface(@NonNull Typeface typeface) {
        headerTextPaint.setTypeface(typeface);
        invalidate();
    }

    public void setFooterTypeface(@NonNull Typeface typeface) {
        footerTextPaint.setTypeface(typeface);
        invalidate();
    }

    public void setHeaderTextSize(int size) {
        headerTextPaint.setTextSize(size);
        invalidate();
    }

    public void setFooterTextSize(int size) {
        footerTextPaint.setTextSize(size);
        invalidate();
    }

    //endregion


    //region Event Handling

    private int getSectionAtX(float x) {
        int limit = sectionCounts.size();
        return (int) Math.min(limit - 1, Math.floor(x / getSectionWidth()));
    }

    private int getSegmentAtX(int section, float x) {
        int limit = sectionCounts.get(section);
        float sectionMinX = getSectionWidth() * section;
        float segmentWidth = getSegmentWidth(section);
        float xInSection = x - sectionMinX;
        return (int) Math.min(limit - 1, xInSection / segmentWidth);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (adapter == null || sectionCounts.size() == 0)
            return false;

        float x = ViewUtil.getNormalizedX(event);
        int section = getSectionAtX(x);
        int segment = getSegmentAtX(section, x);
        highlightedValueText.setText(adapter.getFormattedMagnitudeAt(section, segment));

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                highlightedValueText.setAlpha(0f);
                highlightedValueText.setScaleX(0f);
                highlightedValueText.setScaleY(0f);
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
                        .scaleX(0f)
                        .scaleY(0f)
                        .setApplyChangesToView(true)
                        .addOnAnimationCompleted(finished -> highlightedValueText.setVisibility(INVISIBLE))
                        .start();
                break;
        }
        return true;
    }


    //endregion

    public interface Adapter {
        float getBaseMagnitude();
        float getPeakMagnitude();
        int getSectionCount();
        int getSectionPointCount(int section);
        int getSectionTextColor(int section);
        float getMagnitudeAt(int section, int position);
        @NonNull CharSequence getFormattedMagnitudeAt(int section, int position);
        String getSectionHeader(int section);
        String getSectionFooter(int section);
    }
}
