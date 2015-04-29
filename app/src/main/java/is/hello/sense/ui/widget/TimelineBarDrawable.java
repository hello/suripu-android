package is.hello.sense.ui.widget;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.ui.widget.util.Styles;

public class TimelineBarDrawable extends Drawable {
    //region Drawing

    private final Resources resources;

    private final Paint emptySpacePaint = new Paint();
    private final Paint fillPaint = new Paint();
    private final Paint dividerPaint = new Paint();

    private final int inset;
    private final int dividerWidth;
    private final int emptyBackgroundColor;
    private final int scoreBackgroundColor;

    //endregion


    //region Properties

    private int sleepDepth;
    private boolean empty;

    //endregion


    //region Lifecycle

    public TimelineBarDrawable(@NonNull Resources resources) {
        this.resources = resources;

        this.inset = resources.getDimensionPixelSize(R.dimen.timeline_bar_inset);
        this.dividerWidth = resources.getDimensionPixelSize(R.dimen.divider_size);
        this.emptyBackgroundColor = resources.getColor(R.color.background);
        this.scoreBackgroundColor = resources.getColor(R.color.light_accent_extra_dimmed);

        int dividerColor = resources.getColor(R.color.border);
        dividerPaint.setColor(dividerColor);
    }


    //endregion



    @Override
    public void draw(Canvas canvas) {
        int minX = inset,
            maxX = canvas.getWidth();
        int minY = 0,
            maxY = canvas.getHeight();

        if (empty) {
            canvas.drawRect(minX, minY, maxX, maxY, emptySpacePaint);
        } else {
            float percentage = sleepDepth / 100f;
            float maxWidth = maxX - (inset * 2);
            float fillMaxX = minX + (maxWidth * percentage);
            canvas.drawRect(minX, minY, fillMaxX, maxY, fillPaint);
            canvas.drawRect(fillMaxX, minY, maxX, maxY, emptySpacePaint);
        }

        canvas.drawRect(inset - dividerWidth, minY, inset, maxY, dividerPaint);
    }

    //region Properties

    @Override
    public void setAlpha(int alpha) {
        emptySpacePaint.setAlpha(alpha);
        dividerPaint.setAlpha(alpha);
        fillPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        emptySpacePaint.setColorFilter(cf);
        dividerPaint.setColorFilter(cf);
        fillPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

    public void setSleepDepth(int sleepDepth) {
        this.sleepDepth = sleepDepth;
        this.empty = false;

        int colorRes = Styles.getSleepDepthColorRes(sleepDepth, false);
        fillPaint.setColor(resources.getColor(colorRes));
        emptySpacePaint.setColor(scoreBackgroundColor);

        invalidateSelf();
    }

    public void setEmpty() {
        this.empty = true;
        this.sleepDepth = 0;
        emptySpacePaint.setColor(emptyBackgroundColor);
        invalidateSelf();
    }

    public void bind(@NonNull TimelineSegment segment) {
        if (segment.isBeforeSleep()) {
            setEmpty();
        } else {
            setSleepDepth(segment.getSleepDepth());
        }
    }

    //endregion
}
