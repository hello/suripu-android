package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

import is.hello.sense.R;

public class ScaleView extends FrameLayout {
    public static final int VERTICAL = 0;
    public static final int HORIZONTAL = 1;

    private static final int EMPHASIS_MULTIPLE = 5;
    private static final float TICK_NORMAL_SCALE = 0.5f;
    private static final float TICK_EMPHASIS_SCALE = 0.25f;

    private final Paint linePaint = new Paint();
    private int lineHeight;
    private int segmentSize;
    private int scaleInset;

    private FrameLayout scrollView;
    private FillView fillView;

    private int orientation;
    private int minValue = 20;
    private int maxValue = 120;
    private @Nullable OnValueChangedListener onValueChangedListener;

    public ScaleView(Context context) {
        super(context);
        initialize(null, 0);
    }

    public ScaleView(Context context, int orientation) {
        super(context);
        initialize(orientation);
    }

    public ScaleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(attrs, 0);
    }

    public ScaleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(attrs, defStyleAttr);
    }

    protected void initialize(@Nullable AttributeSet attrs, int defStyleAttr) {
        int orientation = VERTICAL;
        if (attrs != null) {
            TypedArray styles = getContext().obtainStyledAttributes(attrs, R.styleable.ScaleView, defStyleAttr, 0);
            orientation = styles.getInteger(R.styleable.ScaleView_scaleOrientation, VERTICAL);
            styles.recycle();
        }

        initialize(orientation);
    }

    protected void initialize(int orientation) {
        this.orientation = orientation;
        switch (orientation) {
            case VERTICAL: {
                this.scrollView = new ScrollView(getContext()) {
                    @Override
                    protected void onScrollChanged(int l, int t, int oldL, int oldT) {
                        super.onScrollChanged(l, t, oldL, oldT);
                        ScaleView.this.onScrollChanged(l, t, oldL, oldT);
                    }
                };
                break;
            }

            case HORIZONTAL: {
                this.scrollView = new HorizontalScrollView(getContext()) {
                    @Override
                    protected void onScrollChanged(int l, int t, int oldL, int oldT) {
                        super.onScrollChanged(l, t, oldL, oldT);
                        ScaleView.this.onScrollChanged(l, t, oldL, oldT);
                    }
                };
                break;
            }

            default: {
                throw new IllegalArgumentException();
            }
        }


        Resources resources = getResources();

        linePaint.setColor(resources.getColor(R.color.light_accent));
        this.lineHeight = resources.getDimensionPixelSize(R.dimen.scale_view_tick);
        this.segmentSize = resources.getDimensionPixelSize(R.dimen.scale_view_segment);

        this.fillView = new FillView(getContext());
        scrollView.addView(fillView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        scrollView.setOverScrollMode(ScrollView.OVER_SCROLL_NEVER);
        scrollView.setVerticalScrollBarEnabled(false);
        addView(scrollView);

        setWillNotDraw(false);
        synchronize();
    }


    //region Drawing

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);

        if (orientation == VERTICAL) {
            this.scaleInset = h / 2;
        } else {
            this.scaleInset = w / 2;
        }
        synchronize();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);

        int width = canvas.getWidth(),
            height = canvas.getHeight();
        if (orientation == VERTICAL) {
            canvas.drawRect(
                    0f,
                    height / 2f - lineHeight / 2f,
                    width,
                    height / 2f + lineHeight / 2f,
                    linePaint
            );
        } else {
            canvas.drawRect(
                    width / 2f - lineHeight / 2f,
                    0,
                    width / 2f + lineHeight / 2f,
                    height,
                    linePaint
            );
        }
    }

    //endregion


    //region Properties

    protected void synchronize() {
        int area = (scaleInset * 2) + (maxValue - minValue) * segmentSize;
        if (orientation == VERTICAL) {
            fillView.setMinimumHeight(area);
        } else {
            fillView.setMinimumWidth(area);
        }
    }

    public void setMinValue(int minValue) {
        this.minValue = minValue;
        synchronize();
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
        synchronize();
    }

    public void setValue(int value) {
        int offset = scaleInset + (segmentSize * (value - minValue));
        if (orientation == VERTICAL) {
            scrollView.scrollTo(0, offset);
        } else {
            scrollView.scrollTo(offset, 0);
        }
        synchronize();
    }

    public int getValue() {
        int offset = (orientation == VERTICAL) ? scrollView.getScrollY() : scrollView.getScrollX();
        return minValue + (offset / segmentSize);
    }

    public void setOnValueChangedListener(@Nullable OnValueChangedListener onValueChangedListener) {
        this.onValueChangedListener = onValueChangedListener;
    }

    //endregion


    @Override
    protected void onScrollChanged(int l, int t, int oldL, int oldT) {
        super.onScrollChanged(l, t, oldL, oldT);

        if (onValueChangedListener != null) {
            onValueChangedListener.onValueChanged();
        }
    }

    class FillView extends View {
        private final Paint linePaint = new Paint();

        FillView(@NonNull Context context) {
            super(context);
            linePaint.setColor(getResources().getColor(R.color.border));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int width = canvas.getWidth(),
                height = canvas.getHeight();

            if (orientation == VERTICAL) {
                int ticks = (height - scaleInset * 2) / segmentSize;
                float offset = scaleInset;
                for (int i = 0; i < ticks; i++) {
                    float tickScale = ((i % EMPHASIS_MULTIPLE) == 0) ? TICK_EMPHASIS_SCALE : TICK_NORMAL_SCALE;
                    float tickStart = width * tickScale;
                    canvas.drawRect(
                            tickStart,
                            offset - lineHeight / 2f,
                            width,
                            offset + lineHeight / 2f,
                            linePaint
                    );
                    offset += segmentSize;
                }
            } else {
                int ticks = (width - scaleInset * 2) / segmentSize;
                float offset = scaleInset;
                for (int i = 0; i < ticks; i++) {
                    float tickScale = ((i % EMPHASIS_MULTIPLE) == 0) ? TICK_EMPHASIS_SCALE : TICK_NORMAL_SCALE;
                    float tickStart = height * tickScale;
                    canvas.drawRect(
                            offset - lineHeight / 2f,
                            tickStart,
                            offset + lineHeight / 2f,
                            height,
                            linePaint
                    );
                    offset += segmentSize;
                }
            }
        }
    }


    public interface OnValueChangedListener {
        void onValueChanged();
    }
}
