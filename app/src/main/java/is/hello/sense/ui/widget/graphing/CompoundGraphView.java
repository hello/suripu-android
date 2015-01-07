package is.hello.sense.ui.widget.graphing;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import is.hello.sense.R;
import is.hello.sense.ui.widget.graphing.adapters.GraphAdapter;
import is.hello.sense.ui.widget.graphing.drawables.AdapterGraphDrawable;

public class CompoundGraphView extends View {
    private AdapterGraphDrawable graphDrawable;
    private int numberOfLines = 0;
    private boolean wantsHeaders = true;
    private boolean wantsFooters = true;
    private Drawable gridDrawable;

    private final Paint headerTextPaint = new Paint();
    private final Paint footerTextPaint = new Paint();

    private final Rect textRect = new Rect();

    private int headerFooterPadding;


    public CompoundGraphView(Context context) {
        super(context);
        initialize();
    }

    public CompoundGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public CompoundGraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }


    protected void initialize() {
        this.headerFooterPadding = getResources().getDimensionPixelSize(R.dimen.gap_medium);
    }


    //region Drawing

    protected int calculateHeaderHeight() {
        return (int) headerTextPaint.getTextSize() + (headerFooterPadding * 2);
    }

    protected int calculateFooterHeight() {
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

        if (wantsHeaders) {
            int headerHeight = calculateHeaderHeight();

            minY += headerHeight;
            height -= headerHeight;
        }

        if (graphDrawable != null) {
            graphDrawable.setBounds(minX, minY, width, height);
            graphDrawable.draw(canvas);
        }
    }


    //endregion


    //region Properties

    public void setGraphDrawable(AdapterGraphDrawable graphDrawable) {
        this.graphDrawable = graphDrawable;
        invalidate();
    }

    public void setNumberOfLines(int numberOfLines) {
        this.numberOfLines = numberOfLines;
        invalidate();
    }

    public void setWantsHeaders(boolean wantsHeaders) {
        this.wantsHeaders = wantsHeaders;
        invalidate();
    }

    public void setWantsFooters(boolean wantsFooters) {
        this.wantsFooters = wantsFooters;
        invalidate();
    }

    public void setGridDrawable(@Nullable Drawable gridDrawable) {
        this.gridDrawable = gridDrawable;
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

    public void setAdapter(@Nullable GraphAdapter adapter) {
        if (graphDrawable == null) {
            throw new IllegalStateException("Cannot set the adapter on a compound graph view without specifying a drawable first");
        }
        graphDrawable.setAdapter(adapter);
        invalidate();
    }

    //endregion
}
