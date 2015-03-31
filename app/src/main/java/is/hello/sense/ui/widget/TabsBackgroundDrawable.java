package is.hello.sense.ui.widget;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;

import is.hello.sense.R;

public class TabsBackgroundDrawable extends SelectorView.SelectionAwareDrawable {
    private final Paint paint = new Paint();

    private final int selectionHeight;
    private final int dividerHeight;

    private final int backgroundColor;
    private final int selectionColor;
    private final int dividerColor;

    private float positionOffset = 0f;

    public TabsBackgroundDrawable(@NonNull Resources resources, @NonNull Style style) {
        this.selectionHeight = resources.getDimensionPixelSize(style.selectionHeightRes);
        this.dividerHeight = resources.getDimensionPixelSize(style.dividerHeightRes);

        this.backgroundColor = Color.WHITE;
        this.selectionColor = resources.getColor(style.selectionColorRes);
        this.dividerColor = resources.getColor(style.dividerColorRes);
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
            paint.setColor(selectionColor);
            canvas.drawRect(itemOffset, height - selectionHeight, itemOffset + itemWidth, height, paint);
        }
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public boolean getPadding(Rect padding) {
        padding.bottom = selectionHeight;
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


    public static enum Style {
        UNDERSIDE(R.dimen.bottom_line, R.dimen.bottom_line, R.color.light_accent, R.color.border_underside_tabs),
        INLINE(R.dimen.bottom_line, R.dimen.divider_size, R.color.light_accent, R.color.border);

        public final @DimenRes int selectionHeightRes;
        public final @DimenRes int dividerHeightRes;

        public final @ColorRes int selectionColorRes;
        public final @ColorRes int dividerColorRes;

        private Style(@DimenRes int selectionHeightRes,
                      @DimenRes int dividerHeightRes,
                      @ColorRes int selectionColorRes,
                      @ColorRes int dividerColorRes) {
            this.selectionHeightRes = selectionHeightRes;
            this.dividerHeightRes = dividerHeightRes;
            this.selectionColorRes = selectionColorRes;
            this.dividerColorRes = dividerColorRes;
        }
    }
}
