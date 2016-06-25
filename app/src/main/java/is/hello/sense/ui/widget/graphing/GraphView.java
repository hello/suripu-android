package is.hello.sense.ui.widget.graphing;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
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
import android.os.Message;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import is.hello.go99.Anime;
import is.hello.sense.R;
import is.hello.sense.ui.widget.graphing.adapters.GraphAdapter;
import is.hello.sense.ui.widget.graphing.adapters.GraphAdapterCache;
import is.hello.sense.ui.widget.graphing.drawables.GraphDrawable;
import is.hello.sense.ui.widget.util.Views;

public class GraphView extends View implements GraphAdapter.ChangeObserver {
    private static final int TOUCH_DELAY_MS = 50;
    private static final int MSG_TOUCH_DELAY = 0x12;

    private @Nullable GraphDrawable graphDrawable;
    private int numberOfLines = 0;
    private boolean wantsHeaders = true;
    private boolean wantsFooters = true;
    private @Nullable Drawable gridDrawable;
    private @Nullable HeaderFooterProvider headerFooterProvider;
    private @Nullable HighlightListener highlightListener;
    private @Nullable OnDrawListener onDrawListener;

    private final Paint headerTextPaint = new Paint();
    private final Paint footerTextPaint = new Paint();

    private final Rect textRect = new Rect();

    private int headerFooterPadding;

    private float markerPointHalf;

    private final Paint highlightPaint = new Paint();
    private final RectF pointBounds = new RectF();

    private int highlightedSection = GraphAdapterCache.NOT_FOUND;
    private int highlightedSegment = GraphAdapterCache.NOT_FOUND;
    private boolean ignoreTouchUntilEnd = false;
    private int touchSlop;
    private float startEventX = 0f, startEventY = 0f;
    private boolean trackingTouchEvents = false;
    private int offscreenInset = getResources().getDimensionPixelSize(R.dimen.series_graph_offscreen_inset); // Extra spacing so graph doesn't fall below visible portion of the screen.

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


    public GraphView(@NonNull Context context) {
        this(context, null);
    }

    public GraphView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GraphView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final Resources resources = getResources();

        this.markerPointHalf = resources.getDimensionPixelSize(R.dimen.graph_point_size) / 2f;

        this.headerFooterPadding = resources.getDimensionPixelSize(R.dimen.gap_medium);

        this.touchSlop = ViewConfiguration.get(getContext()).getScaledPagingTouchSlop();

        headerTextPaint.setAntiAlias(true);
        headerTextPaint.setSubpixelText(true);
        footerTextPaint.setAntiAlias(true);
        footerTextPaint.setSubpixelText(true);

        setHeaderTextSize(resources.getDimensionPixelOffset(R.dimen.text_size_graph_header));
        setHeaderTypeface(Typeface.create("sans-serif", Typeface.NORMAL));

        setFooterTextSize(resources.getDimensionPixelOffset(R.dimen.text_size_graph_footer));
        setFooterTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));

        highlightPaint.setAntiAlias(true);
        highlightPaint.setAlpha(0);

        if (attrs != null) {
            final TypedArray styles = getContext().obtainStyledAttributes(attrs,
                                                                          R.styleable.GraphView,
                                                                          defStyleAttr,
                                                                          0);

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
        return (highlightedSection != GraphAdapterCache.NOT_FOUND &&
                highlightedSegment != GraphAdapterCache.NOT_FOUND);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int minY = getDrawingMinY();

        int width = getDrawingWidth(),
            height = getDrawingHeight() - offscreenInset;

        if (gridDrawable != null && numberOfLines > 0) {
            final int lineDistance = width / numberOfLines;
            int lineOffset = lineDistance;
            for (int line = 1; line < numberOfLines; line++) {
                gridDrawable.setBounds(lineOffset, minY,
                                       lineOffset + 1, height);
                gridDrawable.draw(canvas);
                lineOffset += lineDistance;
            }
        }

        if (graphDrawable != null) {
            graphDrawable.draw(canvas);
        }

        final int headerHeight = calculateHeaderHeight(),
                  footerHeight = calculateFooterHeight();

        if (wantsHeaders) {
            height -= headerHeight;
            minY += headerHeight;
        } else {
            final int topSpacing = (int) Math.ceil(markerPointHalf);
            height -= topSpacing;
            minY += topSpacing;
        }

        if (wantsFooters) {
            height -= footerHeight;
        }

        if (headerFooterProvider != null) {
            final int sectionCount = headerFooterProvider.getSectionHeaderFooterCount();
            if (sectionCount > 0) {
                final float sectionWidth = width / sectionCount;

                for (int section = 0; section < sectionCount; section++) {
                    if (wantsHeaders) {
                        final int savedAlpha = headerTextPaint.getAlpha();
                        headerTextPaint.setColor(headerFooterProvider.getSectionHeaderTextColor(section));
                        headerTextPaint.setAlpha(savedAlpha);

                        final String text = headerFooterProvider.getSectionHeader(section);
                        headerTextPaint.getTextBounds(text, 0, text.length(), textRect);

                        final float sectionMidX = (sectionWidth * section) + (sectionWidth / 2);
                        final float textX = Math.round(sectionMidX - textRect.centerX());
                        final float textY = Math.round((headerHeight / 2) - textRect.centerY());
                        canvas.drawText(text, textX, textY, headerTextPaint);
                    }

                    if (wantsFooters) {
                        final int savedAlpha = footerTextPaint.getAlpha();
                        footerTextPaint.setColor(headerFooterProvider.getSectionFooterTextColor(section));
                        footerTextPaint.setAlpha(savedAlpha);

                        final String text = headerFooterProvider.getSectionFooter(section);
                        footerTextPaint.getTextBounds(text, 0, text.length(), textRect);

                        final float sectionMidX = (sectionWidth * section) + (sectionWidth / 2);
                        final float textX = Math.round(sectionMidX - textRect.centerX());
                        final float textY =
                                Math.round((minY + height) + ((footerHeight / 2) - textRect.centerY()));
                        canvas.drawText(text, textX, textY, footerTextPaint);
                    }
                }
            }
        }

        if (isHighlighted()) {
            final GraphAdapterCache adapterCache = getAdapterCache();
            final float sectionWidth = adapterCache.calculateSectionWidth(width);
            final float segmentWidth = adapterCache.calculateSegmentWidth(width, highlightedSection);
            final float segmentX = adapterCache.calculateSegmentX(sectionWidth, segmentWidth,
                                                                  highlightedSection,
                                                                  highlightedSegment);
            final float segmentY = adapterCache.calculateSegmentY(height,
                                                                  highlightedSection,
                                                                  highlightedSegment);

            pointBounds.set(segmentX - markerPointHalf, minY + (segmentY - markerPointHalf),
                            segmentX + markerPointHalf, minY + (segmentY + markerPointHalf));
            canvas.drawOval(pointBounds, highlightPaint);

            canvas.drawRect(pointBounds.centerX(), 0f,
                            pointBounds.centerX() + 1f, getDrawingHeight(),
                            highlightPaint);
        }

        if (onDrawListener != null) {
            post(onDrawListener::onGraphDrawCompleted);
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
                graphDrawable.setBottomInset(offscreenInset);
            }

            graphDrawable.setBounds(getDrawingMinX(), getDrawingMinY(),
                                    getDrawingWidth(), getDrawingHeight());
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
            throw new IllegalStateException("Cannot set the adapter on a compound graph view " +
                                                    "without specifying a drawable first");
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

    public void setTintColor(@ColorInt int color) {
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

    public void setGridDrawable(@DrawableRes int drawableRes) {
        if (drawableRes != 0) {
            setGridDrawable(ResourcesCompat.getDrawable(getResources(), drawableRes, null));
        } else {
            setGridDrawable(null);
        }
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

    public void setOnDrawListener(@Nullable OnDrawListener onDrawListener) {
        this.onDrawListener = onDrawListener;
    }

    //endregion


    //region Highlight Support

    protected void setHighlightedValue(int section, int segment) {
        this.highlightedSection = section;
        this.highlightedSegment = segment;

        invalidate();
    }

    protected void animateHighlightAlphaTo(int alpha, @Nullable Runnable onCompletion) {
        final ValueAnimator alphaAnimator = ValueAnimator.ofInt(highlightPaint.getAlpha(), alpha);
        alphaAnimator.setInterpolator(Anime.INTERPOLATOR_DEFAULT);
        alphaAnimator.setDuration(Anime.DURATION_FAST);

        alphaAnimator.addUpdateListener(a -> {
            final int newAlpha = (int) a.getAnimatedValue();
            highlightPaint.setAlpha(newAlpha);

            final int invertedNewAlpha = 255 - newAlpha;
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

    private boolean shouldBeginTouchInteraction(float eventX, float eventY) {
        return (Math.abs(eventX - startEventX) > touchSlop &&
                Math.abs(eventX - startEventX) > Math.abs(eventY - startEventY));
    }

    private void beginTouchInteraction(float eventX) {
        final GraphAdapterCache adapterCache = getAdapterCache();
        final int width = getDrawingWidth();
        final int section = adapterCache.findSectionAtX(width, eventX);
        if (section == GraphAdapterCache.NOT_FOUND) {
            this.trackingTouchEvents = false;
            this.ignoreTouchUntilEnd = true;
            return;
        }

        final int segment = adapterCache.findSegmentAtX(width, section, eventX);
        if (segment == GraphAdapterCache.NOT_FOUND) {
            this.trackingTouchEvents = false;
            this.ignoreTouchUntilEnd = true;
            return;
        }

        setHighlightedValue(section, segment);
        if (highlightListener != null) {
            highlightListener.onGraphHighlightBegin();
            highlightListener.onGraphValueHighlighted(section, segment);
        }

        animateHighlightAlphaTo(255, null);

        this.trackingTouchEvents = true;
    }

    private void cancelTouchInteraction() {
        getHandler().removeMessages(MSG_TOUCH_DELAY);

        if (highlightListener != null) {
            highlightListener.onGraphHighlightEnd();
        }

        highlightPaint.setAlpha(0);
        headerTextPaint.setAlpha(255);
        setHighlightedValue(GraphAdapterCache.NOT_FOUND,
                            GraphAdapterCache.NOT_FOUND);
    }

    private Message obtainBeginTouchInteractionMessage(float eventX) {
        final Message message = Message.obtain(getHandler(), () -> {
            beginTouchInteraction(eventX);
        });
        message.what = MSG_TOUCH_DELAY;
        return message;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (ignoreTouchUntilEnd) {
            getHandler().removeMessages(MSG_TOUCH_DELAY);

            final int action = event.getAction();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                this.ignoreTouchUntilEnd = false;
                this.trackingTouchEvents = false;
            }

            return false;
        }

        if (!hasData() || highlightListener == null) {
            return false;
        }

        final GraphAdapterCache adapterCache = getAdapterCache();
        final int width = getDrawingWidth();
        final float eventX = Views.getNormalizedX(event),
                    eventY = Views.getNormalizedY(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                this.startEventX = eventX;
                this.startEventY = eventY;

                this.trackingTouchEvents = false;

                final Message message = obtainBeginTouchInteractionMessage(eventX);
                getHandler().sendMessageDelayed(message, TOUCH_DELAY_MS);

                return true;
            }

            case MotionEvent.ACTION_MOVE: {
                if (trackingTouchEvents) {
                    getHandler().removeMessages(MSG_TOUCH_DELAY);
                    final int section = adapterCache.findSectionAtX(width, eventX);
                    final int segment = adapterCache.findSegmentAtX(width, section, eventX);

                    setHighlightedValue(section, segment);
                    if (highlightListener != null) {
                        highlightListener.onGraphValueHighlighted(section, segment);
                    }
                } else if (shouldBeginTouchInteraction(eventX, eventY)) {
                    getHandler().removeMessages(MSG_TOUCH_DELAY);
                    beginTouchInteraction(eventX);
                }

                break;
            }

            case MotionEvent.ACTION_UP: {
                getHandler().removeMessages(MSG_TOUCH_DELAY);
                if (trackingTouchEvents) {
                    if (highlightListener != null) {
                        highlightListener.onGraphHighlightEnd();
                    }

                    animateHighlightAlphaTo(0, () -> {
                        setHighlightedValue(GraphAdapterCache.NOT_FOUND,
                                            GraphAdapterCache.NOT_FOUND);
                    });
                }
                this.trackingTouchEvents = false;

                return true;
            }

            case MotionEvent.ACTION_CANCEL: {
                getHandler().removeMessages(MSG_TOUCH_DELAY);
                if (trackingTouchEvents) {
                    cancelTouchInteraction();
                }
                this.trackingTouchEvents = false;

                return true;
            }
        }

        return trackingTouchEvents;
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
        @ColorInt int getSectionHeaderTextColor(int section);
        @ColorInt int getSectionFooterTextColor(int section);
        @NonNull String getSectionHeader(int section);
        @NonNull String getSectionFooter(int section);
    }

    public interface HighlightListener {
        void onGraphHighlightBegin();
        void onGraphValueHighlighted(int section, int position);
        void onGraphHighlightEnd();
    }

    public interface OnDrawListener {
        /**
         * Informs the listener that the graph view has completed
         * rendering its contents. Runs on the next looper cycle.
         */
        void onGraphDrawCompleted();
    }
}
