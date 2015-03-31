package is.hello.sense.ui.widget;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.support.annotation.NonNull;

import is.hello.sense.R;

public class TabsBackgroundDrawable extends SelectorView.SelectionAwareDrawable {
    private final Paint paint = new Paint();

    private final int lineHeight;
    private final int dividerHeight;

    private final int backgroundColor;
    private final int fillColor;
    private final int dividerColor;

    private float positionOffset = 0f;

    public TabsBackgroundDrawable(@NonNull Resources resources, int numberOfItems) {
        this.lineHeight = resources.getDimensionPixelSize(R.dimen.divider_size);
        this.dividerHeight = resources.getDimensionPixelSize(R.dimen.divider_size);

        this.backgroundColor = Color.WHITE;
        this.fillColor = resources.getColor(R.color.light_accent);
        this.dividerColor = resources.getColor(R.color.border_tabs);

        setNumberOfItems(numberOfItems);
    }


    //region Drawing

    @Override
    public void draw(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        paint.setColor(backgroundColor);
        canvas.drawRect(0, 0, width, height, paint);

        paint.setColor(dividerColor);
        canvas.drawRect(0, height - dividerHeight, width, height, paint);

        if (isSelectionValid()) {
            int itemWidth = width / numberOfItems;
            float itemOffset = (itemWidth * selectedIndex) + (itemWidth * positionOffset);
            paint.setColor(fillColor);
            canvas.drawRect(itemOffset, height - lineHeight, itemOffset + itemWidth, height, paint);
        }
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public boolean getPadding(Rect padding) {
        padding.bottom = lineHeight;
        return true;
    }

    //endregion

    //region Attributes

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    public void setPositionOffset(float positionOffset) {
        this.positionOffset = positionOffset;
        invalidateSelf();
    }

    //endregion
}
