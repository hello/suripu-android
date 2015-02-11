package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
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

    //endregion

    //region Properties

    private Drawable selectedDrawable;
    private Drawable unselectedDrawable;

    private int count = 0;
    private int selection = 0;

    private @Nullable ViewPager.OnPageChangeListener onPageChangeListener;

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


        int dotSize = resources.getDimensionPixelSize(R.dimen.page_dot_size);
        this.dotLayout = new LayoutParams(dotSize, dotSize);

        int dotMargin = resources.getDimensionPixelSize(R.dimen.page_dot_margin);
        dotLayout.setMargins(dotMargin, dotMargin, dotMargin, dotMargin);


        if (attrs != null) {
            TypedArray styles = getContext().obtainStyledAttributes(attrs, R.styleable.PageDots, defStyleAttr, 0);

            @DotStyle int dotStyle = styles.getInt(R.styleable.PageDots_dotStyle, STYLE_WHITE);
            setDotStyle(dotStyle);

            styles.recycle();
        }
    }

    //endregion


    //region Internal

    private ImageView createDotView() {
        ImageView dotView = new ImageView(getContext());
        dotView.setScaleType(ImageView.ScaleType.CENTER);
        dotView.setImageDrawable(unselectedDrawable);
        return dotView;
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
            ImageView dotView = (ImageView) getChildAt(i);
            if (i == selection) {
                dotView.setImageDrawable(selectedDrawable);
            } else {
                dotView.setImageDrawable(unselectedDrawable);
            }
        }
    }

    //endregion


    //region Properties

    public void setSelectedDrawable(@NonNull Drawable selectedDrawable) {
        this.selectedDrawable = selectedDrawable;
        syncSelection();
    }

    public void setSelectedResource(@DrawableRes int activeRes) {
        setSelectedDrawable(getResources().getDrawable(activeRes));
    }

    public void setUnselectedDrawable(@NonNull Drawable unselectedDrawable) {
        this.unselectedDrawable = unselectedDrawable;
        syncSelection();
    }

    public void setUnselectedResource(@DrawableRes int inactiveRes) {
        setUnselectedDrawable(getResources().getDrawable(inactiveRes));
    }

    public void setDotStyle(@DotStyle int dotStyle) {
        switch (dotStyle) {
            case STYLE_WHITE: {
                setSelectedResource(R.drawable.page_dot_white_selected);
                setUnselectedResource(R.drawable.page_dot_white_unselected);
                break;
            }

            case STYLE_BLUE: {
                setSelectedResource(R.drawable.page_dot_blue_selected);
                setUnselectedResource(R.drawable.page_dot_blue_unselected);
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

    public void setOnPageChangeListener(@Nullable ViewPager.OnPageChangeListener onPageChangeListener) {
        this.onPageChangeListener = onPageChangeListener;
    }

    public void attach(@NonNull ViewPager viewPager) {
        viewPager.setOnPageChangeListener(this);
        PagerAdapter adapter = viewPager.getAdapter();
        if (adapter != null) {
            setCount(adapter.getCount());
        }
    }

    //endregion


    //region OnPageChangeListener

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if (onPageChangeListener != null) {
            onPageChangeListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
        }
    }

    @Override
    public void onPageSelected(int position) {
        setSelection(position);
        if (onPageChangeListener != null) {
            onPageChangeListener.onPageSelected(position);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (onPageChangeListener != null) {
            onPageChangeListener.onPageScrollStateChanged(state);
        }
    }

    //endregion
}
