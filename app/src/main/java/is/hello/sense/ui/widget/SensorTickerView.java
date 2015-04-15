package is.hello.sense.ui.widget;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.text.TextPaint;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.api.model.Condition;

public class SensorTickerView extends RecyclerView {
    public static final int RAMP_UP_DURATION_MS = 550;
    public static final int RAMP_DOWN_DURATION_MS = 300;
    public static final int ANIMATION_DURATION_MS = (RAMP_UP_DURATION_MS + RAMP_DOWN_DURATION_MS);

    private static final String DEFAULT_DIGIT = "0";

    private final NumberAdapter adapter;
    private final LinearLayoutManager layoutManager;
    private final int contentHeight;

    private final ArgbEvaluator colorEvaluator = new ArgbEvaluator();
    private final int startColor;
    private int endColor;

    private boolean animating = false;

    //region Lifecycle

    public SensorTickerView(@NonNull Context context) {
        this(context, null);
    }

    public SensorTickerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SensorTickerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setHasFixedSize(true);
        setOverScrollMode(OVER_SCROLL_NEVER);

        this.adapter = new NumberAdapter(context);
        setAdapter(adapter);

        this.layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true);
        setLayoutManager(layoutManager);

        setOnScrollListener(new ScrollListener());

        TextPaint paint = new TextPaint();
        TextAppearanceSpan textAppearance = new TextAppearanceSpan(context, R.style.AppTheme_Text_BigScore);
        textAppearance.updateMeasureState(paint);
        Paint.FontMetricsInt fontMetrics = paint.getFontMetricsInt();
        this.contentHeight = Math.abs(fontMetrics.descent - fontMetrics.ascent);

        this.startColor = getResources().getColor(Condition.ALERT.colorRes);
        this.endColor = startColor;
    }

    @Override
    protected void onMeasure(final int widthSpec, final int heightSpec) {
        final int widthMode = MeasureSpec.getMode(widthSpec);
        final int suggestedWidth = MeasureSpec.getSize(widthSpec);
        int measuredWidth;
        switch (widthMode) {
            case MeasureSpec.EXACTLY: {
                measuredWidth = suggestedWidth;
                break;
            }
            case MeasureSpec.AT_MOST: {
                measuredWidth = Math.min(getSuggestedMinimumWidth(), suggestedWidth);
                break;
            }
            default:
            case MeasureSpec.UNSPECIFIED: {
                measuredWidth = getSuggestedMinimumWidth();
                break;
            }
        }

        final int heightMode = MeasureSpec.getMode(heightSpec);
        final int suggestedHeight = MeasureSpec.getSize(heightSpec);
        int measuredHeight;
        switch (heightMode) {
            case MeasureSpec.EXACTLY: {
                measuredHeight = suggestedHeight;
                break;
            }
            case MeasureSpec.AT_MOST: {
                measuredHeight = Math.min(contentHeight, suggestedHeight);
                break;
            }
            default:
            case MeasureSpec.UNSPECIFIED: {
                measuredHeight = getSuggestedMinimumHeight();
                break;
            }
        }

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    //endregion


    //region Values

    public void setValue(int value, int endColor) {
        this.endColor = endColor;
        adapter.setCount(value);
        scrollToPosition(value);
    }

    public void animateToValue(int value, int endColor, @NonNull Runnable onCompletion) {
        this.endColor = endColor;
        adapter.setCount(value);
        scrollToPosition(0);

        LinearSmoothScroller smoothScroller = new LinearSmoothScroller(getContext()) {
            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                return layoutManager.computeScrollVectorForPosition(targetPosition);
            }

            @Override
            protected int calculateTimeForScrolling(int dx) {
                return RAMP_UP_DURATION_MS;
            }

            @Override
            protected int calculateTimeForDeceleration(int dx) {
                return RAMP_DOWN_DURATION_MS;
            }

            @Override
            protected int getVerticalSnapPreference() {
                return SNAP_TO_END;
            }

            @Override
            protected void onStart() {
                super.onStart();

                SensorTickerView.this.animating = true;
            }

            @Override
            protected void onStop() {
                super.onStop();

                SensorTickerView.this.animating = false;
                onCompletion.run();
            }
        };
        smoothScroller.setTargetPosition(value);
        layoutManager.startSmoothScroll(smoothScroller);
    }

    public boolean isAnimating() {
        return animating;
    }

    public void stopAnimating() {
        stopScroll();
    }

    //endregion


    //region Events

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        return false;
    }

    //endregion


    class NumberAdapter extends Adapter<NumberAdapter.ViewHolder> {
        private final ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        private final Context context;
        private int count = 0;

        NumberAdapter(@NonNull Context context) {
            this.context = context;
        }

        void setCount(int count) {
            this.count = count;
            notifyDataSetChanged();
        }

        int getColorForPosition(int position) {
            float fraction = (position / (float) count);
            return (int) colorEvaluator.evaluate(fraction, startColor, endColor);
        }

        @Override
        public int getItemCount() {
            return count;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            NumberView numberView = new NumberView(context);
            numberView.setLayoutParams(layoutParams);
            return new ViewHolder(numberView);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.display(position);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final NumberView numberView;

            ViewHolder(@NonNull NumberView itemView) {
                super(itemView);

                this.numberView = itemView;
            }

            void display(int position) {
                numberView.setValue(position);
                numberView.setColor(getColorForPosition(position));
            }
        }
    }

    static class NumberView extends LinearLayout {
        private static final int NUMBER_DIGITS = 3;

        private final TextView[] textViews = new TextView[NUMBER_DIGITS];

        NumberView(@NonNull Context context) {
            this(context, null);
        }

        NumberView(@NonNull Context context, @Nullable AttributeSet attrs) {
            this(context, attrs, 0);
        }

        NumberView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);

            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER);
            setClipChildren(false);

            LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            for (int i = 0, length = textViews.length; i < length; i++) {
                TextView textView = new TextView(context, attrs, defStyleAttr);
                textView.setTextAppearance(context, R.style.AppTheme_Text_BigScore);
                textView.setText(DEFAULT_DIGIT);
                addView(textView, layoutParams);

                this.textViews[i] = textView;
            }
        }

        void setColor(int color) {
            for (TextView textView : textViews) {
                textView.setTextColor(color);
            }
        }

        void setValue(int value) {
            if (value > 999) {
                throw new IllegalArgumentException("Colors larger than 999 not supported");
            }

            String asString = Integer.toString(value);
            int start = textViews.length - asString.length();
            for (int i = 0, length = textViews.length; i < length; i++) {
                TextView textView = textViews[i];
                if (i < start) {
                    textView.setVisibility(INVISIBLE);
                } else {
                    int offset = i - start;
                    textView.setText(asString.substring(offset, offset + 1));
                    textView.setVisibility(VISIBLE);
                }
            }
        }

        void setOffset(float offset) {
            setAlpha(1f - Math.abs(offset));
            for (int i = 0, length = textViews.length; i < length; i++) {
                TextView textView = textViews[i];
                float fraction = (0.1f * i) * Math.abs(offset);
                textView.setTranslationY(textView.getMeasuredHeight() * fraction);
            }
        }
    }

    class ScrollListener extends OnScrollListener {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            int containerHeight = recyclerView.getMeasuredHeight();
            float containerCenterY = containerHeight / 2f;
            for (int i = 0, count = recyclerView.getChildCount(); i < count; i++) {
                NumberView child = (NumberView) recyclerView.getChildAt(i);
                float childCenterY = (child.getTop() + child.getBottom()) / 2f;
                float distance = containerCenterY - childCenterY;
                float offset = distance / containerCenterY;
                child.setOffset(offset);
            }
        }
    }
}
