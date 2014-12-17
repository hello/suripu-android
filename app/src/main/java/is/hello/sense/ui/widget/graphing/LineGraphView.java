package is.hello.sense.ui.widget.graphing;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import is.hello.sense.R;
import is.hello.sense.ui.animation.Animations;
import is.hello.sense.ui.animation.PropertyAnimatorProxy;
import is.hello.sense.ui.widget.Styles;
import is.hello.sense.ui.widget.Views;

public final class LineGraphView extends FrameLayout implements GraphAdapter.ChangeObserver {
    private @Nullable OnValueHighlightedListener onValueHighlightedListener;
    private StyleableGraphAdapter adapter;
    private int numberOfLines = 0;
    private Drawable fillDrawable;
    private Drawable gridDrawable;
    private boolean wantsHeaders = true;
    private boolean wantsFooters = true;

    private final GraphAdapterCache adapterCache = new GraphAdapterCache(GraphAdapterCache.Type.STYLEABLE);

    private float topLineHeight;

    private final Paint topLinePaint = new Paint();
    private final Paint headerTextPaint = new Paint();
    private final Paint footerTextPaint = new Paint();

    private final Path fillPath = new Path();
    private final Path markersPath = new Path();

    private final Rect textRect = new Rect();

    private int headerFooterPadding;

    private boolean isValueHighlighted;
    private HighlightedValueView highlightedValue;

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

        Styles.applyGraphLineParameters(topLinePaint);

        headerTextPaint.setAntiAlias(true);
        headerTextPaint.setSubpixelText(true);
        footerTextPaint.setAntiAlias(true);
        footerTextPaint.setSubpixelText(true);

        setHeaderTextSize(resources.getDimensionPixelOffset(R.dimen.text_size_section_heading));
        setHeaderTypeface(Typeface.createFromAsset(resources.getAssets(), Styles.TYPEFACE_HEAVY));

        setFooterTextSize(resources.getDimensionPixelOffset(R.dimen.text_size_body));
        setFooterTypeface(Typeface.createFromAsset(resources.getAssets(), Styles.TYPEFACE_LIGHT));

        if (attrs != null) {
            TypedArray styles = getContext().obtainStyledAttributes(attrs, R.styleable.LineGraphView, defStyleAttr, 0);
            setTopLineHeight(styles.getDimensionPixelOffset(R.styleable.LineGraphView_topLineHeight, resources.getDimensionPixelSize(R.dimen.view_line_graph_line_size)));
            setFillDrawable(styles.getDrawable(R.styleable.LineGraphView_fill));

            this.numberOfLines = styles.getInt(R.styleable.LineGraphView_lineCount, 0);
            setGridDrawable(styles.getDrawable(R.styleable.LineGraphView_gridDrawable));

            this.wantsHeaders = styles.getBoolean(R.styleable.LineGraphView_wantsHeaders, true);
            this.wantsFooters = styles.getBoolean(R.styleable.LineGraphView_wantsFooters, true);
        } else {
            setTopLineHeight(resources.getDimensionPixelSize(R.dimen.view_line_graph_line_size));
        }

        this.headerFooterPadding = getResources().getDimensionPixelSize(R.dimen.gap_medium);

        this.highlightedValue = new HighlightedValueView(getContext());
        addView(highlightedValue);

        setWillNotDraw(false);
    }


    //region Drawing

    private int calculateHeaderHeight() {
        return (int) headerTextPaint.getTextSize() + (headerFooterPadding * 2);
    }

    private int calculateFooterHeight() {
        return (int) footerTextPaint.getTextSize() + (headerFooterPadding * 2);
    }

    private float getSectionWidth() {
        return getMeasuredWidth() / adapterCache.getNumberSections();
    }

    private float getSegmentWidth(int section) {
        return getSectionWidth() / adapterCache.getSectionCount(section);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int sectionCount = adapterCache.getNumberSections();
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
            fillPath.reset();
            markersPath.reset();

            fillPath.moveTo(0, height);

            float halfOfTopLine = topLineHeight / 2f;

            int headerHeight = calculateHeaderHeight();
            if (wantsHeaders) {
                minY += headerHeight;
                height -= headerHeight;
            }

            int footerHeight = calculateFooterHeight();
            if (wantsFooters) {
                height -= footerHeight;
            }

            float sectionWidth = width / sectionCount;
            for (int section = 0; section < sectionCount; section++) {
                int pointCount = adapterCache.getSectionCount(section);
                if (pointCount == 0)
                    continue;

                float segmentWidth = sectionWidth / (float) pointCount;

                if (wantsHeaders && !isValueHighlighted) {
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

                Path sectionPath = adapterCache.getSectionLinePath(section);
                sectionPath.reset();
                for (int position = 0; position < pointCount; position++) {
                    float segmentX = adapterCache.calculateSegmentX(sectionWidth, segmentWidth, section, position);
                    float segmentY = minY + adapterCache.calculateSegmentY(height, section, position);

                    if (position == 0) {
                        fillPath.moveTo(minX, segmentY);
                        sectionPath.moveTo(segmentX, segmentY);
                    } else {
                        sectionPath.lineTo(segmentX, segmentY);
                    }
                    fillPath.lineTo(segmentX, segmentY - halfOfTopLine);

                    if (section == sectionCount - 1 && position == pointCount - 1) {
                        fillPath.lineTo(segmentX + halfOfTopLine, height);
                        fillPath.lineTo(0, height);
                    }
                }

                if (section < sectionCount - 1) {
                    float closingSegmentY = minY + adapterCache.calculateSegmentY(height, section + 1, 0);
                    sectionPath.lineTo(sectionWidth * (section + 1), closingSegmentY);
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

            for (int section = 0; section < sectionCount; section++) {
                Path sectionPath = adapterCache.getSectionLinePath(section);
                topLinePaint.setColor(adapter.getSectionLineColor(section));
                canvas.drawPath(sectionPath, topLinePaint);
            }
        }
    }

    //endregion


    //region Properties

    public @Nullable StyleableGraphAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(@Nullable StyleableGraphAdapter adapter) {
        if (this.adapter != null) {
            this.adapter.unregisterObserver(this);
        }

        this.adapter = adapter;
        if (adapter != null) {
            adapter.registerObserver(this);
        }

        adapterCache.setAdapter(adapter);
        invalidate();
    }

    public void setOnValueHighlightedListener(@Nullable OnValueHighlightedListener onValueHighlightedListener) {
        this.onValueHighlightedListener = onValueHighlightedListener;
    }

    @Override
    public void onGraphAdapterChanged() {
        adapterCache.rebuild();
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

    public void setTopLineHeight(int height) {
        this.topLineHeight = height;
        topLinePaint.setStrokeWidth(topLineHeight);
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
        int limit = adapterCache.getNumberSections();
        return (int) Math.min(limit - 1, Math.floor(x / getSectionWidth()));
    }

    private int getSegmentAtX(int section, float x) {
        int limit = adapterCache.getSectionCount(section);
        float sectionMinX = getSectionWidth() * section;
        float segmentWidth = getSegmentWidth(section);
        float xInSection = x - sectionMinX;
        return (int) Math.min(limit - 1, xInSection / segmentWidth);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (adapter == null || adapterCache.getNumberSections() == 0) {
            return false;
        }

        float x = Views.getNormalizedX(event);
        int section = getSectionAtX(x);
        int position = getSegmentAtX(section, x);
        highlightedValue.display(section, position);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                if (onValueHighlightedListener != null) {
                    onValueHighlightedListener.onGraphHighlightBegin();
                    onValueHighlightedListener.onGraphValueHighlighted(section, position);
                }

                PropertyAnimatorProxy.animate(highlightedValue)
                        .setDuration(Animations.DURATION_MINIMUM)
                        .fadeIn()
                        .start();

                this.isValueHighlighted = true;
                invalidate();

                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (onValueHighlightedListener != null) {
                    onValueHighlightedListener.onGraphValueHighlighted(section, position);
                }

                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                if (onValueHighlightedListener != null) {
                    onValueHighlightedListener.onGraphHighlightEnd();
                }

                PropertyAnimatorProxy.animate(highlightedValue)
                        .setDuration(Animations.DURATION_MINIMUM)
                        .fadeOut(GONE)
                        .start();

                this.isValueHighlighted = false;
                invalidate();

                break;
            }
        }
        return true;
    }

    //endregion


    class HighlightedValueView extends View {
        final Paint backgroundPaint = new Paint();
        final RectF pointBounds = new RectF();
        final float pointAreaHalf;

        int section = -1;
        int segment = -1;

        HighlightedValueView(Context context) {
            super(context);

            this.pointAreaHalf = getResources().getDimensionPixelSize(R.dimen.view_line_graph_point_size) / 2f;
            backgroundPaint.setAntiAlias(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (section != -1 && segment != -1) {
                int minY = 0;
                int height = getMeasuredHeight();

                int headerHeight = calculateHeaderHeight();
                if (wantsHeaders) {
                    minY += headerHeight;
                    height -= headerHeight;
                }

                int footerHeight = calculateFooterHeight();
                if (wantsFooters) {
                    height -= footerHeight;
                }

                float segmentX = adapterCache.calculateSegmentX(getSectionWidth(), getSegmentWidth(section), section, segment);
                float segmentY = adapterCache.calculateSegmentY(height, section, segment);

                pointBounds.set(segmentX - pointAreaHalf,
                                minY + (segmentY - pointAreaHalf),
                                segmentX + pointAreaHalf,
                                minY + (segmentY + pointAreaHalf));
                canvas.drawOval(pointBounds, backgroundPaint);

                canvas.drawRect(pointBounds.centerX(), 0f, pointBounds.centerX() + 1f, minY + height, backgroundPaint);
            }
        }

        void display(int section, int segment) {
            if (adapter != null) {
                backgroundPaint.setColor(adapter.getSectionLineColor(section));

                this.section = section;
                this.segment = segment;
            } else {
                this.section = -1;
                this.segment = -1;
            }

            invalidate();
        }
    }


    public interface OnValueHighlightedListener {
        void onGraphHighlightBegin();
        void onGraphValueHighlighted(int section, int position);
        void onGraphHighlightEnd();
    }
}
