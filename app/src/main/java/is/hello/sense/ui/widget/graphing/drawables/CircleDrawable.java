package is.hello.sense.ui.widget.graphing.drawables;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

public class CircleDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CircleDrawable(final int color) {
        this.paint.setColor(color);
    }

    @Override
    public void draw(final Canvas canvas) {
        canvas.drawCircle(canvas.getWidth() / 2, canvas.getHeight() / 2, canvas.getHeight() / 2, paint);
    }

    @Override
    public void setAlpha(final int alpha) {

    }

    @Override
    public void setColorFilter(final ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return 0;
    }
}