package is.hello.sense.ui.widget.timeline;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import is.hello.sense.R;

/**
 * Roughly identical to an empty timeline fragment.
 */
public class TimelinePlaceholderDrawable extends Drawable {
    private final Drawable header;
    private final Drawable contents;
    private final Paint dividerPaint = new Paint();
    private final Rect dividerRect = new Rect();

    private final int headerHeight;
    private final int dividerHeight;

    public TimelinePlaceholderDrawable(@NonNull Resources resources) {
        this.header = resources.getDrawable(R.drawable.background_timeline_header);
        this.contents = resources.getDrawable(R.drawable.background_timeline_segment);

        dividerPaint.setColor(resources.getColor(R.color.timeline_header_border));

        this.headerHeight = resources.getDimensionPixelSize(R.dimen.timeline_header_estimated_height);
        this.dividerHeight = resources.getDimensionPixelSize(R.dimen.divider_size);
    }


    @Override
    public void draw(Canvas canvas) {
        header.draw(canvas);
        contents.draw(canvas);
        canvas.drawRect(dividerRect, dividerPaint);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        header.setBounds(0, 0, bounds.right, headerHeight);
        contents.setBounds(0, headerHeight + dividerHeight, bounds.right, bounds.bottom);
        dividerRect.set(0, headerHeight, bounds.right, headerHeight + dividerHeight);
    }

    @Override
    public void setAlpha(int alpha) {
        header.setAlpha(alpha);
        contents.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        header.setColorFilter(cf);
        contents.setColorFilter(cf);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
