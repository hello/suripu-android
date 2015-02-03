package is.hello.sense.ui.widget.graphing;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import is.hello.sense.R;
import is.hello.sense.ui.animation.Animations;
import is.hello.sense.ui.widget.graphing.adapters.GraphAdapter;
import is.hello.sense.ui.widget.graphing.adapters.GraphAdapterCache;
import is.hello.sense.ui.widget.graphing.drawables.GraphDrawable;
import is.hello.sense.ui.widget.util.Views;

public class GraphView extends View implements GraphAdapter.ChangeObserver {
    private static final int NONE = -1;

    private @Nullable GraphDrawable graphDrawable;
    private int numberOfLines = 0;
    private boolean wantsHeaders = true;
    private boolean wantsFooters = true;
    private @Nullable Drawable gridDrawable;
    private @Nullable HeaderFooterProvider headerFooterProvider;
    private @Nullable HighlightListener highlightListener;

    private final Paint headerTextPaint = new Paint();
    private final Paint footerTextPaint = new Paint();

    private final Rect textRect = new Rect();

    private int headerFooterPadding;

    private float markerPointHalf;

    private final Paint highlightPaint = new Paint();
    private final RectF pointBounds = new RectF();

    private int highlightedSection = NONE, highlightedSegment = NONE;
    private boolean ignoreTouchUntilEnd = false;

    private final Drawable.Callback DRAWABLE_CALLBACK = new Drawable.Callback() {
        @Override
        public void invalidateDrawable(Drawable drawable) {
            invalidate();
        }

        @Override
        public void scheduleDrawable(Drawable drawable, Runnable what, long when) {
            GraphView.this.scheduleDrawable(drawable, what, when);
        }

        @Override
        public void unscheduleDrawable(Drawable drawable, Runnable what) {
            GraphView.this.unscheduleDrawable(drawable, what);
        }
    };


    public GraphView(Context context) {
        super(context);
        initialize(null, 0);
    }

    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(attrs, 0);
    }

    public GraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(attrs, defStyleAttr);
    }


    protected void initialize(@Nullable AttributeSet attrs, int defStyleAttr) {
        Resources resources = getResources();

        this.markerPointHalf = resources.getDimensionPixelSize(R.dimen.graph_point_size) / 2f;

        this.headerFooterPadding = resources.getDimensionPixelSize(R.dimen.gap_medium);

        headerTextPaint.setAntiAlias(true);
        headerTextPaint.setSubpixelText(true);
        footerTextPaint.setAntiAlias(true);
        footerTextPaint.setSubpixelText(true);

        setHeaderTextSize(resources.getDimensionPixelOffset(R.dimen.text_size_graph_header));
        setHeaderTypeface(Typeface.create("sans-serif", Typeface.BOLD));

        setFooterTextSize(resources.getDimensionPixelOffset(R.dimen.text_size_graph_footer));
        setFooterTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));

        highlightPaint.setAntiAlias(true);
        highlightPaint.setAlpha(0);

        if (attrs != null) {
            TypedArray styles = getContext().obtainStyledAttributes(attrs, R.styleable.GraphView, defStyleAttr, 0);

            this.numberOfLines = styles.getInt(R.styleable.GraphView_senseNumberOfLines, 0);
            setGridDrawable(styles.getDrawable(R.styleable.GraphView_senseGridDrawable));

            setWantsHeaders(styles.getBoolean(R.styleable.GraphView_senseWantsHeaders, true));
            setWantsFooters(styles.getBoolean(R.styleable.GraphView_senseWantsFooters, true));

            styles.recycle();
        } else {
            this.gridDrawable = new ColorDrawable(Color.GRAY);
        }
    }


    //region Drawing

    public int calculateHeaderHeight() {
        return (int) headerTextPaint.getTextSize() + (headerFooterPadding * 2);
    }

    public int calculateFooterHeight() {
        return (int) footerTextPaint.getTextSize() + (headerFooterPadding * 2);
    }

    protected int getDrawingMinX() {
        return 0;
    }

    protected int getDrawingMinY() {
        return 0;
    }

    protected int getDrawingWidth() {
        return getMeasuredWidth() - getDrawingMinX();
    }

    protected int getDrawingHeight() {
        return getMeasuredHeight() - getDrawingMinY();
    }

    protected boolean isHighlighted() {
        return (highlightedSection != NONE && highlightedSegment != NONE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int minY = getDrawingMinY();

        int width = getDrawingWidth(),
            height = getDrawingHeight();

        if (gridDrawable != null && numberOfLines > 0) {
            int lineDistance = width / numberOfLines;
            int lineOffset = lineDistance;
            for (int line = 1; line < numberOfLines; line++) {
                gridDrawable.setBounds(lineOffset, minY, lineOffset + 1, height);
                gridDrawable.draw(canvas);
                lineOffset += lineDistance;
            }
        }

        if (graphDrawable != null) {
            graphDrawable.draw(canvas);
        }

        if (headerFooterProvider != null) {
            int sectionCount = headerFooterProvider.getSectionHeaderFooterCount();
            if (sectionCount > 0) {
                int headerHeight = calculateHeaderHeight(),
                    footerHeight = calculateFooterHeight();

                float sectionWidth = width / sectionCount;

                if (wantsHeaders) {
                    height -= headerHeight;
                    minY += headerHeight;
                } else {
                    int topSpacing = (int) Math.ceil(markerPointHalf);
                    height -= topSpacing;
                    minY += topSpacing;
                }

                if (wantsFooters) {
                    height -= footerHeight;
                }

                for (int section = 0; section < sectionCount; section++) {
                    if (wantsHeaders) {
                        int savedAlpha = headerTextPaint.getAlpha();
                        headerTextPaint.setColor(headerFooterProvider.getSectionHeaderTextColor(section));
                        headerTextPaint.setAlpha(savedAlpha);

                        String text = headerFooterProvider.getSectionHeader(section);
                        headerTextPaint.getTextBounds(text, 0, text.length(), textRect);

                        float sectionMidX = (sectionWidth * section) + (sectionWidth / 2);
                        float textX = Math.round(sectionMidX - textRect.centerX());
                        float textY = Math.round((headerHeight / 2) - textRect.centerY());
                        canvas.drawText(text, textX, textY, headerTextPaint);
                    }

                    if (wantsFooters) {
                        int savedAlpha = footerTextPaint.getAlpha();
                        footerTextPaint.setColor(headerFooterProvider.getSectionFooterTextColor(section));
                        footerTextPaint.setAlpha(savedAlpha);

                        String text = headerFooterProvider.getSectionFooter(section);
                        footerTextPaint.getTextBounds(text, 0, text.length(), textRect);

                        float sectionMidX = (sectionWidth * section) + (sectionWidth / 2);
                        float textX = Math.round(sectionMidX - textRect.centerX());
                        float textY = Math.round((minY + height) + ((footerHeight / 2) - textRect.centerY()));
                        canvas.drawText(text, textX, textY, footerTextPaint);
                    }
                }
            }
        }

        if (isHighlighted()) {
            GraphAdapterCache adapterCache = getAdapterCache();
            float sectionWidth = adapterCache.calculateSectionWidth(width);
            float segmentWidth = adapterCache.calculateSegmentWidth(width, highlightedSection);
            float segmentX = adapterCache.calculateSegmentX(sectionWidth, segmentWidth, highlightedSection, highlightedSegment);
            float segmentY = adapterCache.calculateSegmentY(height, highlightedSection, highlightedSegment);

            pointBounds.set(segmentX - markerPointHalf, minY + (segmentY - markerPointHalf),
                            segmentX + markerPointHalf, minY + (segmentY + markerPointHalf));
            canvas.drawOval(pointBounds, highlightPaint);

            canvas.drawRect(pointBounds.centerX(), 0f, pointBounds.centerX() + 1f, getDrawingHeight(), highlightPaint);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);

        updateDrawableMetrics();
    }

    protected void updateDrawableMetrics() {
        if (graphDrawable != null) {
            if (wantsHeaders) {
                graphDrawable.setTopInset(calculateHeaderHeight());
            } else {
                graphDrawable.setTopInset(0);
            }

            if (wantsFooters) {
                graphDrawable.setBottomInset(calculateFooterHeight());
            } else {
                graphDrawable.setBottomInset(0);
            }

            graphDrawable.setBounds(getDrawingMinX(), getDrawingMinY(), getDrawingWidth(), getDrawingHeight());
        }
    }

    //endregion


    //region Properties

    public boolean hasData() {
        return (graphDrawable != null && getAdapterCache().getNumberSections() > 0);
    }

    public void setGraphDrawable(@Nullable GraphDrawable graphDrawable) {
        if (this.graphDrawable != null) {
            this.graphDrawable.setCallback(null);
            this.graphDrawable.setChangeObserver(null);
        }

        this.graphDrawable = graphDrawable;
        updateDrawableMetrics();

        if (graphDrawable != null) {
            graphDrawable.setCallback(DRAWABLE_CALLBACK);
            graphDrawable.setChangeObserver(this);
        }

        invalidate();
    }

    public void setAdapter(@Nullable GraphAdapter adapter) {
        if (graphDrawable == null) {
            throw new IllegalStateException("Cannot set the adapter on a compound graph view without specifying a drawable first");
        }

        if (isHighlighted()) {
            cancelTouchInteraction();
            this.ignoreTouchUntilEnd = true;
        }

        graphDrawable.setAdapter(adapter);
        invalidate();
    }

    protected GraphAdapterCache getAdapterCache() {
        if (graphDrawable == null) {
            throw new IllegalStateException();
        }

        return graphDrawable.getAdapterCache();
    }

    public void setTintColor(int color) {
        if (graphDrawable == null) {
            throw new IllegalStateException();
        }

        graphDrawable.setTintColor(color);
        highlightPaint.setColor(color);
    }

    public void setNumberOfLines(int numberOfLines) {
        this.numberOfLines = numberOfLines;
        invalidate();
    }

    public void setWantsHeaders(boolean wantsHeaders) {
        this.wantsHeaders = wantsHeaders;
        updateDrawableMetrics();
        invalidate();
    }

    public void setWantsFooters(boolean wantsFooters) {
        this.wantsFooters = wantsFooters;
        updateDrawableMetrics();
        invalidate();
    }

    public void setGridDrawable(@Nullable Drawable gridDrawable) {
        this.gridDrawable = gridDrawable;
        invalidate();
    }

    public void setHeaderTypeface(@NonNull Typeface typeface) {
        headerTextPaint.setTypeface(typeface);
        updateDrawableMetrics();
        invalidate();
    }

    public void setFooterTypeface(@NonNull Typeface typeface) {
        footerTextPaint.setTypeface(typeface);
        updateDrawableMetrics();
        invalidate();
    }

    public void setHeaderTextSize(int size) {
        headerTextPaint.setTextSize(size);
        updateDrawableMetrics();
        invalidate();
    }

    public void setFooterTextSize(int size) {
        footerTextPaint.setTextSize(size);
        updateDrawableMetrics();
        invalidate();
    }

    public void setHeaderFooterProvider(@Nullable HeaderFooterProvider headerFooterProvider) {
        this.headerFooterProvider = headerFooterProvider;
        invalidate();
    }

    public void setHighlightListener(@Nullable HighlightListener highlightListener) {
        this.highlightListener = highlightListener;
    }

    //endregion


    //region Highlight Support

    protected void setHighlightedValue(int section, int segment) {
        this.highlightedSection = section;
        this.highlightedSegment = segment;

        invalidate();
    }

    protected void animateHighlightAlphaTo(int alpha, @Nullable Runnable onCompletion) {
        ValueAnimator alphaAnimator = ValueAnimator.ofInt(highlightPaint.getAlpha(), alpha);
        alphaAnimator.setInterpolator(Animations.INTERPOLATOR_DEFAULT);
        alphaAnimator.setDuration(Animations.DURATION_MINIMUM);

        alphaAnimator.addUpdateListener(a -> {
            int newAlpha = (int) a.getAnimatedValue();
            highlightPaint.setAlpha(newAlpha);

            int invertedNewAlpha = 255 - newAlpha;
            headerTextPaint.setAlpha(invertedNewAlpha);
            footerTextPaint.setAlpha(invertedNewAlpha);

            invalidate();
        });

        if (onCompletion != null) {
            alphaAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    onCompletion.run();
                }
            });
        }

        alphaAnimator.start();
    }

    private void cancelTouchInteraction() {
        if (highlightListener != null) {
            highlightListener.onGraphHighlightEnd();
        }

        highlightPaint.setAlpha(0);
        headerTextPaint.setAlpha(255);
        setHighlightedValue(NONE, NONE);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (ignoreTouchUntilEnd) {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                this.ignoreTouchUntilEnd = false;
            }

            return false;
        }

        if (!hasData() || highlightListener == null) {
            return false;
        }

        GraphAdapterCache adapterCache = getAdapterCache();
        int width = getDrawingWidth();
        float x = Views.getNormalizedX(event);
        int section = adapterCache.findSectionAtX(width, x);
        int segment = adapterCache.findSegmentAtX(width, section, x);
        setHighlightedValue(section, segment);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                if (highlightListener != null) {
                    highlightListener.onGraphHighlightBegin();
                    highlightListener.onGraphValueHighlighted(section, segment);
                }

                animateHighlightAlphaTo(255, null);

                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (highlightListener != null) {
                    highlightListener.onGraphValueHighlighted(section, segment);
                }

                break;
            }

            case MotionEvent.ACTION_UP: {
                if (highlightListener != null) {
                    highlightListener.onGraphHighlightEnd();
                }

                animateHighlightAlphaTo(0, () -> setHighlightedValue(NONE, NONE));

                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                cancelTouchInteraction();

                break;
            }
        }

        return true;
    }

    @Override
    public void onGraphAdapterChanged() {
        if (isHighlighted()) {
            cancelTouchInteraction();
            this.ignoreTouchUntilEnd = true;
        }
    }

    //endregion


    public interface HeaderFooterProvider {
        int getSectionHeaderFooterCount();
        int getSectionHeaderTextColor(int section);
        int getSectionFooterTextColor(int section);
        @NonNull String getSectionHeader(int section);
        @NonNull String getSectionFooter(int section);
    }

    public interface HighlightListener {
        void onGraphHighlightBegin();
        void onGraphValueHighlighted(int section, int position);
        void onGraphHighlightEnd();
    }

}
