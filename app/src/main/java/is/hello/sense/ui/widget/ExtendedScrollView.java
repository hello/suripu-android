package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import is.hello.sense.R;

public class ExtendedScrollView extends ScrollView {
    public static final int FADING_EDGE_STYLE_NONE = 0;
    public static final int FADING_EDGE_STYLE_ROUNDED = 1;
    public static final int FADING_EDGE_STYLE_STRAIGHT = 2;

    @IntDef({FADING_EDGE_STYLE_NONE,
            FADING_EDGE_STYLE_ROUNDED,
            FADING_EDGE_STYLE_STRAIGHT})
    @Target({ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
    @Retention(RetentionPolicy.SOURCE)
    @interface FadingEdgeStyle {}

    private @Nullable Drawable topEdge;
    private @Nullable Drawable bottomEdge;

    private boolean scrollingEnabled = true;
    private @Nullable OnScrollListener onScrollListener;
    private boolean dispatchedInitialScrollEvent = false;


    //region Lifecycle

    public ExtendedScrollView(@NonNull Context context) {
        this(context, null);
    }

    public ExtendedScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExtendedScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (attrs != null) {
            final TypedArray styles = context.obtainStyledAttributes(attrs,
                                                                     R.styleable.ExtendedScrollView,
                                                                     defStyleAttr, 0);

            final @FadingEdgeStyle int fadingEdgeStyle =
                    styles.getInt(R.styleable.ExtendedScrollView_senseFadingEdgeStyle,
                                  FADING_EDGE_STYLE_NONE);
            setFadingEdgeStyle(fadingEdgeStyle);

            final boolean scrollingEnabled =
                    styles.getBoolean(R.styleable.ExtendedScrollView_senseScrollingEnabled, true);
            setScrollingEnabled(scrollingEnabled);

            styles.recycle();
        }
    }

    //endregion


    //region Drawing

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        final int contentHeight = getContentHeight() + getPaddingTop() + getPaddingBottom();
        final int containerHeight = getMeasuredHeight();
        if (contentHeight > containerHeight) {
            final int scrollY = getScrollY();

            final int top = getPaddingTop() + scrollY;
            final int bottom = canvas.getHeight() - getPaddingBottom() + scrollY;
            final int left = getPaddingLeft();
            final int right = canvas.getWidth() - getPaddingRight();

            if (topEdge != null && scrollY > 0) {
                topEdge.setBounds(left, top, right, top + topEdge.getIntrinsicHeight());
                topEdge.draw(canvas);
            }

            if (bottomEdge != null && scrollY < (contentHeight - containerHeight)) {
                bottomEdge.setBounds(left, bottom - bottomEdge.getIntrinsicHeight(),
                                     right, bottom);
                bottomEdge.draw(canvas);
            }
        }
    }

    //endregion


    //region Attributes

    public void setScrollingEnabled(boolean scrollingEnabled) {
        this.scrollingEnabled = scrollingEnabled;
    }

    public void setOnScrollListener(@Nullable OnScrollListener onScrollListener) {
        this.onScrollListener = onScrollListener;

        if (onScrollListener != null && ViewCompat.isLaidOut(this)) {
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();
            onScrollListener.onScrollChanged(scrollX, scrollY,
                                             scrollX, scrollY);

            this.dispatchedInitialScrollEvent = true;
        } else {
            this.dispatchedInitialScrollEvent = false;
        }
    }

    public void setFadingEdgeStyle(@FadingEdgeStyle int style) {
        final Resources resources = getResources();
        switch (style) {
            case FADING_EDGE_STYLE_NONE:
                this.topEdge = null;
                this.bottomEdge = null;
                break;
            case FADING_EDGE_STYLE_ROUNDED:
                this.topEdge = ResourcesCompat.getDrawable(resources, R.drawable.rounded_shadow_top_down, null);
                this.bottomEdge = ResourcesCompat.getDrawable(resources, R.drawable.rounded_shadow_bottom_up, null);
                break;
            case FADING_EDGE_STYLE_STRAIGHT:
                this.topEdge = ResourcesCompat.getDrawable(resources, R.drawable.shadow_top_down, null);
                this.bottomEdge = ResourcesCompat.getDrawable(resources, R.drawable.shadow_bottom_up, null);
                break;
        }

        setWillNotDraw(style == FADING_EDGE_STYLE_NONE);
        setClipToPadding(style == FADING_EDGE_STYLE_NONE);
        invalidate();
    }

    public View getContentView() {
        return getChildAt(0);
    }

    public int getContentHeight() {
        final View contentView = getContentView();
        if (contentView != null) {
            return contentView.getMeasuredHeight();
        } else {
            return 0;
        }
    }

    //endregion


    //region Events

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (!dispatchedInitialScrollEvent) {
            if (onScrollListener != null) {
                final int scrollX = getScrollX();
                final int scrollY = getScrollY();
                onScrollListener.onScrollChanged(scrollX, scrollY,
                                                 scrollX, scrollY);
            }
            
            this.dispatchedInitialScrollEvent = true;
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        return scrollingEnabled && super.onTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull MotionEvent ev) {
        return scrollingEnabled && super.onInterceptTouchEvent(ev);
    }

    @Override
    protected void onScrollChanged(int scrollX, int scrollY,
                                   int oldScrollX, int oldScrollY) {
        super.onScrollChanged(scrollX, scrollY, oldScrollX, oldScrollY);

        if (onScrollListener != null) {
            onScrollListener.onScrollChanged(scrollX, scrollY, oldScrollX, oldScrollY);
        }
    }

    //endregion


    public interface OnScrollListener {
        void onScrollChanged(int scrollX, int scrollY,
                             int oldScrollX, int oldScrollY);
    }
}
