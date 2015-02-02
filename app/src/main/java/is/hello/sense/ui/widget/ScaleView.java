package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

import is.hello.sense.R;

public class ScaleView extends FrameLayout {
    //region Constants

    public static final int VERTICAL = 0;
    public static final int HORIZONTAL = 1;

    private static final int EMPHASIS_STEP = 5;
    private static final float TICK_NORMAL_SCALE = 0.5f;
    private static final float TICK_EMPHASIS_SCALE = 0.25f;

    //endregion


    //region Contents

    private final Paint linePaint = new Paint();
    private float diamondHalf;
    private int segmentSize;
    private int scaleInset;

    private FrameLayout tickFillHost;
    private TickView tickView;

    //endregion


    //region Properties

    private int orientation;
    private int minValue = 20;
    private int maxValue = 120;
    private @Nullable OnValueChangedListener onValueChangedListener;

    //endregion


    //region Lifecycle

    public ScaleView(Context context) {
        this(context, null);
    }

    public ScaleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScaleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        int orientation = VERTICAL;
        int initialValue = 0;
        if (attrs != null) {
            TypedArray styles = getContext().obtainStyledAttributes(attrs, R.styleable.ScaleView, defStyleAttr, 0);

            orientation = styles.getInteger(R.styleable.ScaleView_scaleOrientation, VERTICAL);
            this.minValue = styles.getInteger(R.styleable.ScaleView_scaleMinValue, 0);
            this.maxValue = styles.getInteger(R.styleable.ScaleView_scaleMaxValue, 100);
            initialValue = styles.getInteger(R.styleable.ScaleView_scaleValue, 0);

            styles.recycle();
        }

        initialize(orientation, initialValue);
    }

    protected void initialize(int orientation, int initialValue) {
        LayoutParams tickLayoutParams;
        if (orientation == VERTICAL) {
            this.tickFillHost = new ScrollView(getContext()) {
                @Override
                protected void onScrollChanged(int l, int t, int oldL, int oldT) {
                    super.onScrollChanged(l, t, oldL, oldT);
                    ScaleView.this.onFillHostScrolled();
                }
            };
            tickFillHost.setVerticalScrollBarEnabled(false);
            tickFillHost.setVerticalFadingEdgeEnabled(true);

            tickLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        } else if (orientation == HORIZONTAL) {
            this.tickFillHost = new HorizontalScrollView(getContext()) {
                @Override
                protected void onScrollChanged(int l, int t, int oldL, int oldT) {
                    super.onScrollChanged(l, t, oldL, oldT);
                    ScaleView.this.onFillHostScrolled();
                }
            };
            tickFillHost.setHorizontalScrollBarEnabled(false);
            tickFillHost.setHorizontalFadingEdgeEnabled(true);
            tickLayoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        } else {
            throw new IllegalArgumentException();
        }
        this.orientation = orientation;

        Resources resources = getResources();

        linePaint.setColor(resources.getColor(R.color.light_accent));
        this.diamondHalf = resources.getDimensionPixelSize(R.dimen.scale_view_diamond) / 2f;
        this.segmentSize = resources.getDimensionPixelSize(R.dimen.scale_view_segment);

        this.tickView = new TickView(getContext());
        tickFillHost.addView(tickView, tickLayoutParams);

        tickFillHost.setFadingEdgeLength(resources.getDimensionPixelSize(R.dimen.shadow_size));
        tickFillHost.setOverScrollMode(ScrollView.OVER_SCROLL_NEVER);
        tickFillHost.setBackgroundColor(Color.WHITE);
        addView(tickFillHost, tickLayoutParams);

        setWillNotDraw(false);
        updateFillArea();
        setValueAsync(initialValue, false);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state != null && state instanceof Bundle) {
            Bundle savedState = (Bundle) state;

            setMinValue(savedState.getInt("minValue"));
            setMaxValue(savedState.getInt("maxValue"));
            setValueAsync(savedState.getInt("value"), true);

            state = savedState.getParcelable("savedState");
        }
        super.onRestoreInstanceState(state);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle savedState = new Bundle();
        savedState.putParcelable("savedState", super.onSaveInstanceState());
        savedState.putInt("minValue", minValue);
        savedState.putInt("maxValue", maxValue);
        savedState.putInt("value", getValue());
        return savedState;
    }

    //endregion


    //region Drawing

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);

        if (orientation == VERTICAL) {
            this.scaleInset = h / 2;
        } else {
            this.scaleInset = w / 2;
        }
        updateFillArea();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);

        int width = canvas.getWidth(),
            midX = width / 2,
            height = canvas.getHeight(),
            midY = height / 2;

        if (orientation == VERTICAL) {
            canvas.drawRect(0f, midY - diamondHalf, width, midY + diamondHalf, linePaint);
        } else {
            canvas.drawRect(midX - diamondHalf, 0f, midX + diamondHalf, height, linePaint);
        }
    }

    //endregion


    //region Values

    protected void updateFillArea() {
        int area = (scaleInset * 2) + (maxValue - minValue) * segmentSize;
        if (orientation == VERTICAL) {
            tickView.setMinimumHeight(area);
        } else {
            tickView.setMinimumWidth(area);
        }
        invalidate();
    }

    protected int normalizeValue(int value) {
        if (value < minValue) {
            return minValue;
        } else if (value > maxValue) {
            return maxValue;
        } else {
            return value;
        }
    }

    protected int getScaleHeight() {
        return (tickView.getMeasuredHeight() - (scaleInset * 2));
    }

    protected int calculateOffset(int value) {
        int rawOffset = (segmentSize * (normalizeValue(value) - minValue));
        if (orientation == VERTICAL) {
            return (getScaleHeight() - rawOffset);
        } else {
            return rawOffset;
        }
    }

    public void setMinValue(int minValue) {
        if (minValue == maxValue) {
            throw new IllegalArgumentException("minValue cannot equal maxValue");
        }

        this.minValue = minValue;
        updateFillArea();
    }

    public void setMaxValue(int maxValue) {
        if (minValue == maxValue) {
            throw new IllegalArgumentException("minValue cannot equal maxValue");
        }

        this.maxValue = maxValue;
        updateFillArea();
    }

    public void setValueAsync(int value, boolean notifyListener) {
        updateFillArea();

        post(() -> {
            int offset = calculateOffset(value);
            if (orientation == VERTICAL) {
                tickFillHost.scrollTo(0, offset);
            } else {
                tickFillHost.scrollTo(offset, 0);
            }
            if (notifyListener) {
                onFillHostScrolled();
            }
        });
    }

    public int getValue() {
        int offset = (orientation == VERTICAL) ? (getScaleHeight() - tickFillHost.getScrollY()) : tickFillHost.getScrollX();
        return minValue + (offset / segmentSize);
    }

    public void setOnValueChangedListener(@Nullable OnValueChangedListener onValueChangedListener) {
        this.onValueChangedListener = onValueChangedListener;
    }

    protected void onFillHostScrolled() {
        if (onValueChangedListener != null) {
            onValueChangedListener.onValueChanged(getValue());
        }
    }

    //endregion


    private class TickView extends View {
        private final Paint linePaint = new Paint();
        private final float lineHeightHalf;
        private final int normalColor;
        private final int emphasizedColor;

        private TickView(@NonNull Context context) {
            super(context);

            Resources resources = context.getResources();
            this.lineHeightHalf = resources.getDimensionPixelSize(R.dimen.scale_view_tick) / 2f;
            this.normalColor = resources.getColor(R.color.view_scale_tick_normal);
            this.emphasizedColor = resources.getColor(R.color.view_scale_tick_emphasized);

            setDrawingCacheQuality(DRAWING_CACHE_QUALITY_LOW);
        }

        protected float getScaleForTick(int tick) {
            if ((tick % EMPHASIS_STEP) == 0) {
                return TICK_EMPHASIS_SCALE;
            } else {
                return TICK_NORMAL_SCALE;
            }
        }

        protected int getColorForTick(int tick) {
            if ((tick % EMPHASIS_STEP) == 0) {
                return emphasizedColor;
            } else {
                return normalColor;
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int width = canvas.getWidth(),
                height = canvas.getHeight();

            if (orientation == VERTICAL) {
                int ticks = (height - scaleInset * 2) / segmentSize;
                for (int tick = 0; tick < ticks; tick++) {
                    float tickScale = getScaleForTick(tick);
                    float tickStart = width * tickScale;
                    float tickY = scaleInset + (tick * segmentSize);
                    linePaint.setColor(getColorForTick(tick));
                    canvas.drawRect(tickStart, tickY - lineHeightHalf, width, tickY + lineHeightHalf, linePaint);
                }
            } else {
                int ticks = (width - scaleInset * 2) / segmentSize;
                for (int tick = 0; tick < ticks; tick++) {
                    float tickScale = getScaleForTick(tick);
                    float tickStart = height * tickScale;
                    float tickX = scaleInset + (tick * segmentSize);
                    linePaint.setColor(getColorForTick(tick));
                    canvas.drawRect(tickX - lineHeightHalf, tickStart, tickX + lineHeightHalf, height, linePaint);
                }
            }
        }

        @Override
        public boolean isOpaque() {
            return true;
        }
    }


    public interface OnValueChangedListener {
        void onValueChanged(int newValue);
    }
}
