package is.hello.sense.ui.widget;

import android.animation.IntEvaluator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import is.hello.sense.R;

public final class PageDots extends LinearLayout implements ViewPager.OnPageChangeListener {
    //region Styles

    public static final int STYLE_WHITE = 0;
    public static final int STYLE_BLUE = 1;

    @IntDef({STYLE_WHITE, STYLE_BLUE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DotStyle {}

    //endregion


    //region Constants

    private final LayoutParams dotLayout;
    private final int unselectedDotSizeHalf;
    private final int selectedDotSizeHalf;
    private final IntEvaluator focusEvaluator = new IntEvaluator();

    //endregion


    //region Properties

    private int color;

    private int count = 0;
    private int selection = 0;

    private boolean updateOnSelection = true;

    //endregion


    //region Lifecycle

    public PageDots(@NonNull Context context) {
        this(context, null);
    }

    public PageDots(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageDots(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);


        Resources resources = getResources();
        int padding = resources.getDimensionPixelSize(R.dimen.gap_medium);
        setPadding(padding, padding, padding, padding);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);


        this.dotLayout = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        int dotMargin = resources.getDimensionPixelSize(R.dimen.page_dot_margin);
        dotLayout.setMargins(dotMargin, 0, dotMargin, 0);

        this.unselectedDotSizeHalf = resources.getDimensionPixelSize(R.dimen.page_dot_unselected_size) / 2;
        this.selectedDotSizeHalf = resources.getDimensionPixelSize(R.dimen.page_dot_selected_size) / 2;


        if (attrs != null) {
            TypedArray styles = getContext().obtainStyledAttributes(attrs, R.styleable.PageDots, defStyleAttr, 0);

            @DotStyle int dotStyle = styles.getInt(R.styleable.PageDots_dotStyle, STYLE_WHITE);
            setDotStyle(dotStyle);

            styles.recycle();
        } else {
            setDotStyle(STYLE_WHITE);
        }
    }

    //endregion


    //region Internal

    private ImageView createDotView() {
        ImageView dotView = new ImageView(getContext());
        dotView.setScaleType(ImageView.ScaleType.CENTER);

        DotDrawable dotDrawable = new DotDrawable();
        dotDrawable.setColor(color);
        dotView.setImageDrawable(dotDrawable);

        return dotView;
    }

    private DotDrawable getDotDrawableAt(int position) {
        ImageView dotView = (ImageView) getChildAt(position);
        return (DotDrawable) dotView.getDrawable();
    }

    private void updateCount(int newCount) {
        if (newCount > getChildCount()) {
            int delta = newCount - getChildCount();
            for (int i = 0; i < delta; i++) {
                addView(createDotView(), dotLayout);
            }
        } else if (newCount < getChildCount()) {
            removeViews(newCount - 1, getChildCount());
        }
    }

    private void syncSelection() {
        for (int i = 0; i < count; i++) {
            DotDrawable dot = getDotDrawableAt(i);
            if (i == selection) {
                dot.setFocusAmount(1f);
            } else {
                dot.setFocusAmount(0f);
            }
        }
    }

    //endregion


    //region Properties

    public void setColor(int color) {
        this.color = color;

        for (int i = 0; i < count; i++) {
            DotDrawable dot = getDotDrawableAt(i);
            dot.setColor(color);
        }
    }

    public void setDotStyle(@DotStyle int dotStyle) {
        switch (dotStyle) {
            case STYLE_WHITE: {
                setColor(Color.WHITE);
                break;
            }

            case STYLE_BLUE: {
                setColor(getResources().getColor(R.color.light_accent));
                break;
            }

            default: {
                throw new IllegalArgumentException("Unknown style " + dotStyle);
            }
        }
    }

    public void setCount(int count) {
        this.count = count;

        updateCount(count);
        syncSelection();
    }

    public void setSelection(int selection) {
        this.selection = selection;

        syncSelection();
    }

    public void attach(@NonNull ViewPager viewPager) {
        viewPager.addOnPageChangeListener(this);
        PagerAdapter adapter = viewPager.getAdapter();
        if (adapter == null) {
            throw new IllegalStateException("Cannot attach to a view pager without an adapter.");
        }

        adapter.registerDataSetObserver(new AdapterChangeObserver(adapter));
        setCount(adapter.getCount());
    }

    //endregion


    //region OnPageChangeListener

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if (position < count - 1) {
            DotDrawable current = getDotDrawableAt(position);
            current.setFocusAmount(1f - positionOffset);

            DotDrawable upcoming = getDotDrawableAt(position + 1);
            upcoming.setFocusAmount(positionOffset);
        }
    }

    @Override
    public void onPageSelected(int position) {
        if (updateOnSelection) {
            setSelection(position);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        this.updateOnSelection = (state == ViewPager.SCROLL_STATE_IDLE);
    }

    //endregion


    private class AdapterChangeObserver extends DataSetObserver {
        private final PagerAdapter adapter;

        private AdapterChangeObserver(@NonNull PagerAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public void onChanged() {
            setCount(adapter.getCount());
        }

        @Override
        public void onInvalidated() {
            setCount(0);
        }
    }

    private class DotDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private float focusAmount = 1f;


        //region Drawing

        @Override
        public void draw(Canvas canvas) {
            int halfWidth = canvas.getWidth() / 2,
                halfHeight = canvas.getHeight() / 2;

            int halfDotSize = focusEvaluator.evaluate(focusAmount, unselectedDotSizeHalf, selectedDotSizeHalf);
            rect.set(halfWidth - halfDotSize, halfHeight - halfDotSize,
                    halfWidth + halfDotSize, halfHeight + halfDotSize);
            canvas.drawOval(rect, paint);
        }

        //endregion


        //region Attributes

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            paint.setColorFilter(cf);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public int getIntrinsicWidth() {
            return selectedDotSizeHalf * 2;
        }

        @Override
        public int getIntrinsicHeight() {
            return selectedDotSizeHalf * 2;
        }

        public void setColor(int color) {
            int savedAlpha = paint.getAlpha();
            paint.setColor(color);
            paint.setAlpha(savedAlpha);
            invalidateSelf();
        }

        public void setFocusAmount(float focusAmount) {
            this.focusAmount = focusAmount;
            setAlpha(focusEvaluator.evaluate(focusAmount, 0x88, 0xFF));
            invalidateSelf();
        }

        //endregion
    }
}
