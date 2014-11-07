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
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.ui.animation.PropertyAnimatorProxy;
import is.hello.sense.ui.common.ViewUtil;

public final class LineGraphView extends FrameLayout {
    private Adapter adapter;
    private int numberOfLines = 0;
    private Drawable fillDrawable;
    private Drawable gridDrawable;
    private boolean wantsHeaders = true;
    private boolean wantsFooters = true;

    private float cachedPeakMagnitude = 0f;
    private final List<Integer> cachedSectionCounts = new ArrayList<>();

    private float topLineHeight;

    private final Paint topLinePaint = new Paint();
    private final Paint textPaint = new Paint();

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

        textPaint.setAntiAlias(true);
        textPaint.setSubpixelText(true);
        setTextSize(resources.getDimensionPixelOffset(R.dimen.text_size_small));
        setTypeface(Typeface.createFromAsset(getResources().getAssets(), "fonts/AvenirLTCom-Light.ttf"));
        setTextColor(resources.getColor(R.color.text_dark));

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
        return getMeasuredWidth() / cachedSectionCounts.size();
    }

    private float getSegmentWidth(int section) {
        return getSectionWidth() / cachedSectionCounts.get(section);
    }

    private float absoluteSegmentX(float sectionWidth, float segmentWidth, int section, int position) {
        return (sectionWidth * section) + (segmentWidth * position);
    }

    private float absoluteSegmentY(float height, int section, int position) {
        return Math.round(height - (height * (adapter.getMagnitudeAt(section, position) / this.cachedPeakMagnitude)));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int sectionCount = cachedSectionCounts.size();
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

            int headerFooterHeight = (int) textPaint.getTextSize() + (headerFooterPadding * 2);
            if (wantsHeaders) {
                minY += headerFooterHeight;
                height -= headerFooterHeight;
            }

            if (wantsFooters) {
                height -= headerFooterHeight;
            }

            float sectionWidth = width / sectionCount;
            for (int section = 0; section < sectionCount; section++) {
                int pointCount = cachedSectionCounts.get(section);
                if (pointCount == 0)
                    continue;

                float segmentWidth = sectionWidth / (float) pointCount;

                if (wantsHeaders) {
                    String text = adapter.getSectionHeader(section);
                    textPaint.getTextBounds(text, 0, text.length(), textRect);

                    float sectionMidX = (sectionWidth * section) + (sectionWidth / 2);
                    float textX = Math.round(sectionMidX - textRect.centerX());
                    float textY = Math.round((headerFooterHeight / 2) - textRect.centerY());
                    canvas.drawText(text, textX, textY, textPaint);
                }

                if (wantsFooters) {
                    String text = adapter.getSectionFooter(section);
                    textPaint.getTextBounds(text, 0, text.length(), textRect);

                    float sectionMidX = (sectionWidth * section) + (sectionWidth / 2);
                    float textX = Math.round(sectionMidX - textRect.centerX());
                    float textY = Math.round((minY + height) + ((headerFooterHeight / 2) - textRect.centerY()));
                    canvas.drawText(text, textX, textY, textPaint);
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
        this.cachedSectionCounts.clear();

        if (adapter != null) {
            this.cachedPeakMagnitude = adapter.getPeakMagnitude();
            for (int section = 0, sections = adapter.getSectionCount(); section < sections; section++) {
                int count = adapter.getSectionPointCount(section);
                cachedSectionCounts.add(count);
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

    public void setTypeface(@NonNull Typeface typeface) {
        textPaint.setTypeface(typeface);
        invalidate();
    }

    public void setTextSize(int size) {
        textPaint.setTextSize(size);
        invalidate();
    }

    public void setTextColor(int color) {
        textPaint.setColor(color);
        invalidate();
    }

    //endregion


    //region Event Handling

    private int getSectionAtX(float x) {
        int limit = cachedSectionCounts.size();
        return (int) Math.min(limit - 1, Math.floor(x / getSectionWidth()));
    }

    private int getSegmentAtX(int section, float x) {
        float sectionMinX = getSectionWidth() * section;
        float segmentWidth = getSegmentWidth(section);
        float xInSection = x - sectionMinX;
        return (int) (xInSection / segmentWidth);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (adapter == null || cachedSectionCounts.size() == 0)
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
        int getPeakMagnitude();
        int getSectionCount();
        int getSectionPointCount(int section);
        float getMagnitudeAt(int section, int position);
        @NonNull CharSequence getFormattedMagnitudeAt(int section, int position);
        String getSectionHeader(int section);
        String getSectionFooter(int section);
    }
}
