package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Styles;

public class TimelineBarView extends View {
    //region Drawing

    private final Paint backgroundPaint = new Paint();
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

    public TimelineBarView(@NonNull Context context) {
        this(context, null);
    }

    public TimelineBarView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimelineBarView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        Resources resources = context.getResources();
        this.inset = resources.getDimensionPixelSize(R.dimen.timeline_bar_inset);
        this.dividerWidth = resources.getDimensionPixelSize(R.dimen.divider_size);
        this.emptyBackgroundColor = resources.getColor(R.color.background);
        this.scoreBackgroundColor = resources.getColor(R.color.light_accent_extra_dimmed);

        int dividerColor = resources.getColor(R.color.border);
        dividerPaint.setColor(dividerColor);

        setBackgroundColor(Color.WHITE);
    }

    //endregion


    //region Drawing

    @Override
    protected void onDraw(Canvas canvas) {
        int minX = inset,
            maxX = canvas.getWidth();
        int minY = 0,
            maxY = canvas.getHeight();

        if (empty) {
            canvas.drawRect(minX, minY, maxX, maxY, backgroundPaint);
        } else {
            float percentage = sleepDepth / 100f;
            float maxWidth = maxX - (inset * 2);
            float fillMaxX = minX + (maxWidth * percentage);
            canvas.drawRect(minX, minY, fillMaxX, maxY, fillPaint);
            canvas.drawRect(fillMaxX, minY, maxX, maxY, backgroundPaint);
        }

        canvas.drawRect(inset - dividerWidth, minY, inset, maxY, dividerPaint);
    }

    //endregion


    //region Properties

    public void setSleepDepth(int sleepDepth) {
        this.sleepDepth = sleepDepth;
        this.empty = false;

        int colorRes = Styles.getSleepDepthColorRes(sleepDepth, false);
        fillPaint.setColor(getResources().getColor(colorRes));
        backgroundPaint.setColor(scoreBackgroundColor);

        invalidate();
    }

    public void setEmpty() {
        this.empty = true;
        this.sleepDepth = 0;
        backgroundPaint.setColor(emptyBackgroundColor);
        invalidate();
    }

    //endregion
}
