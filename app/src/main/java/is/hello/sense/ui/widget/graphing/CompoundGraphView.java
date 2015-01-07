package is.hello.sense.ui.widget.graphing;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import is.hello.sense.R;
import is.hello.sense.ui.widget.graphing.adapters.GraphAdapter;
import is.hello.sense.ui.widget.graphing.adapters.GraphAdapterCache;
import is.hello.sense.ui.widget.graphing.drawables.GraphDrawable;
import is.hello.sense.ui.widget.util.Styles;

public class CompoundGraphView extends View {
    private @Nullable GraphDrawable graphDrawable;
    private int numberOfLines = 0;
    private boolean wantsHeaders = true;
    private boolean wantsFooters = true;
    private @Nullable Drawable gridDrawable;
    private @Nullable HeaderFooterProvider headerFooterProvider;

    private final Paint headerTextPaint = new Paint();
    private final Paint footerTextPaint = new Paint();

    private final Rect textRect = new Rect();

    private int headerFooterPadding;


    public CompoundGraphView(Context context) {
        super(context);
        initialize(null, 0);
    }

    public CompoundGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(attrs, 0);
    }

    public CompoundGraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(attrs, defStyleAttr);
    }


    protected void initialize(@Nullable AttributeSet attrs, int defStyleAttr) {
        Resources resources = getResources();

        this.headerFooterPadding = resources.getDimensionPixelSize(R.dimen.gap_medium);

        headerTextPaint.setAntiAlias(true);
        headerTextPaint.setSubpixelText(true);
        footerTextPaint.setAntiAlias(true);
        footerTextPaint.setSubpixelText(true);

        setHeaderTextSize(resources.getDimensionPixelOffset(R.dimen.text_size_section_heading));
        setHeaderTypeface(Typeface.createFromAsset(resources.getAssets(), Styles.TYPEFACE_HEAVY));

        setFooterTextSize(resources.getDimensionPixelOffset(R.dimen.text_size_body));
        setFooterTypeface(Typeface.createFromAsset(resources.getAssets(), Styles.TYPEFACE_LIGHT));

        if (attrs != null) {
            TypedArray styles = getContext().obtainStyledAttributes(attrs, R.styleable.CompoundGraphView, defStyleAttr, 0);

            this.numberOfLines = styles.getInt(R.styleable.CompoundGraphView_senseNumberOfLines, 0);
            setGridDrawable(styles.getDrawable(R.styleable.CompoundGraphView_senseGridDrawable));

            this.wantsHeaders = styles.getBoolean(R.styleable.CompoundGraphView_senseWantsHeaders, true);
            this.wantsFooters = styles.getBoolean(R.styleable.CompoundGraphView_senseWantsFooters, true);

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

    @Override
    protected void onDraw(Canvas canvas) {
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

        if (graphDrawable != null) {
            graphDrawable.setBounds(minX, minY, width, height);
            graphDrawable.draw(canvas);
        }

        if (headerFooterProvider != null) {
            int sectionCount = headerFooterProvider.getSectionCount();
            if (sectionCount > 0) {
                int headerHeight = calculateHeaderHeight(),
                    footerHeight = calculateFooterHeight();

                float sectionWidth = width / sectionCount;

                if (wantsHeaders) {
                    height -= headerHeight;
                    minY += headerHeight;
                }

                if (wantsFooters) {
                    height -= footerHeight;
                }

                for (int section = 0; section < sectionCount; section++) {
                    if (wantsHeaders) {
                        headerTextPaint.setColor(headerFooterProvider.getSectionTextColor(section));

                        String text = headerFooterProvider.getSectionHeader(section);
                        headerTextPaint.getTextBounds(text, 0, text.length(), textRect);

                        float sectionMidX = (sectionWidth * section) + (sectionWidth / 2);
                        float textX = Math.round(sectionMidX - textRect.centerX());
                        float textY = Math.round((headerHeight / 2) - textRect.centerY());
                        canvas.drawText(text, textX, textY, headerTextPaint);
                    }

                    if (wantsFooters) {
                        footerTextPaint.setColor(headerFooterProvider.getSectionTextColor(section));

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
    }

    protected void updateDrawableInsets() {
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
        }
    }

    //endregion


    //region Properties

    public void setGraphDrawable(@Nullable GraphDrawable graphDrawable) {
        if (this.graphDrawable != null) {
            this.graphDrawable.setCallback(null);
        }

        this.graphDrawable = graphDrawable;
        updateDrawableInsets();

        if (graphDrawable != null) {
            graphDrawable.setCallback(this);
        }

        invalidate();
    }

    public void setNumberOfLines(int numberOfLines) {
        this.numberOfLines = numberOfLines;
        invalidate();
    }

    public void setWantsHeaders(boolean wantsHeaders) {
        this.wantsHeaders = wantsHeaders;
        updateDrawableInsets();
        invalidate();
    }

    public void setWantsFooters(boolean wantsFooters) {
        this.wantsFooters = wantsFooters;
        updateDrawableInsets();
        invalidate();
    }

    public void setGridDrawable(@Nullable Drawable gridDrawable) {
        this.gridDrawable = gridDrawable;
        invalidate();
    }

    public void setHeaderTypeface(@NonNull Typeface typeface) {
        headerTextPaint.setTypeface(typeface);
        updateDrawableInsets();
        invalidate();
    }

    public void setFooterTypeface(@NonNull Typeface typeface) {
        footerTextPaint.setTypeface(typeface);
        updateDrawableInsets();
        invalidate();
    }

    public void setHeaderTextSize(int size) {
        headerTextPaint.setTextSize(size);
        updateDrawableInsets();
        invalidate();
    }

    public void setFooterTextSize(int size) {
        footerTextPaint.setTextSize(size);
        updateDrawableInsets();
        invalidate();
    }

    public void setAdapter(@Nullable GraphAdapter adapter) {
        if (graphDrawable == null) {
            throw new IllegalStateException("Cannot set the adapter on a compound graph view without specifying a drawable first");
        }

        graphDrawable.setAdapter(adapter);
        invalidate();
    }

    public void setHeaderFooterProvider(@Nullable HeaderFooterProvider headerFooterProvider) {
        this.headerFooterProvider = headerFooterProvider;
        invalidate();
    }

    protected GraphAdapterCache getAdapterCache() {
        if (graphDrawable == null) {
            throw new IllegalStateException();
        }

        return graphDrawable.getAdapterCache();
    }

    //endregion


    //region Event Handling

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (graphDrawable == null || graphDrawable.getAdapter() == null) {
            return false;
        }

        return true;
    }

    //endregion


    public interface HeaderFooterProvider {
        int getSectionCount();
        int getSectionTextColor(int section);
        @NonNull String getSectionHeader(int section);
        @NonNull String getSectionFooter(int section);
    }
}
