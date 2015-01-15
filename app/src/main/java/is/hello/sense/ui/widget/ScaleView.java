package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

import is.hello.sense.R;

public class ScaleView extends ScrollView {
    private static final int EMPHASIS_MULTIPLE = 5;
    private static final float TICK_NORMAL_SCALE = 0.5f;
    private static final float TICK_EMPHASIS_SCALE = 0.25f;

    private final Paint linePaint = new Paint();
    private int lineHeight;
    private int segmentHeight;
    private int insetHeight;

    private FillView fillView;

    private int minValue = 20;
    private int maxValue = 120;
    private @Nullable OnValueChangedListener onValueChangedListener;

    public ScaleView(Context context) {
        super(context);
        initialize();
    }

    public ScaleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public ScaleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    protected void initialize() {
        Resources resources = getResources();

        linePaint.setColor(resources.getColor(R.color.light_accent));
        this.lineHeight = resources.getDimensionPixelSize(R.dimen.scale_view_tick);
        this.segmentHeight = resources.getDimensionPixelSize(R.dimen.scale_view_segment);

        this.fillView = new FillView(getContext());
        addView(fillView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        setOverScrollMode(ScrollView.OVER_SCROLL_NEVER);
        setVerticalScrollBarEnabled(false);
        setWillNotDraw(false);
        synchronize();
    }


    //region Drawing

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);

        this.insetHeight = h / 2;
        synchronize();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);

        int width = canvas.getWidth(),
            height = canvas.getHeight();
        canvas.drawRect(0f, getScrollY() + height / 2f - lineHeight / 2f, width, getScrollY() + height / 2f + lineHeight / 2f, linePaint);
    }

    //endregion


    //region Properties

    protected void synchronize() {
        fillView.setMinimumHeight((insetHeight * 2) + (maxValue - minValue) * segmentHeight);
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
        int offsetY = insetHeight + (segmentHeight * (value - minValue));
        scrollTo(0, offsetY);
        synchronize();
    }

    public int getValue() {
        return minValue + (getScrollY() / segmentHeight);
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

            int ticks = (height - insetHeight * 2) / segmentHeight;
            float top = insetHeight;
            for (int i = 0; i < ticks; i++) {
                float tickScale = ((i % EMPHASIS_MULTIPLE) == 0) ? TICK_EMPHASIS_SCALE : TICK_NORMAL_SCALE;
                float tickStart = width * tickScale;
                canvas.drawRect(tickStart, top - lineHeight / 2f, width, top + lineHeight / 2f, linePaint);
                top += segmentHeight;
            }
        }
    }


    public interface OnValueChangedListener {
        void onValueChanged();
    }
}
