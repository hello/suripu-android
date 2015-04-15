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
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.api.model.Condition;

public class SensorTickerView extends RecyclerView {
    private static final float ANIMATION_MS_PER_PX = 100f;

    private final NumberAdapter adapter;
    private final LinearLayoutManager layoutManager;

    private final ArgbEvaluator colorEvaluator = new ArgbEvaluator();
    private final int startColor;
    private int endColor;

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

        this.adapter = new NumberAdapter(context);
        setAdapter(adapter);

        this.layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        setLayoutManager(layoutManager);

        TextPaint paint = new TextPaint();
        TextAppearanceSpan textAppearance = new TextAppearanceSpan(context, R.style.AppTheme_Text_BigScore);
        textAppearance.updateMeasureState(paint);
        Paint.FontMetricsInt fontMetrics = paint.getFontMetricsInt();
        setMinimumHeight(fontMetrics.top + fontMetrics.descent);

        this.startColor = getResources().getColor(Condition.ALERT.colorRes);
        this.endColor = startColor;
    }

    //endregion


    public void animateToValue(int value, int endColor) {
        this.endColor = endColor;
        adapter.setCount(value);
        scrollToPosition(0);

        LinearSmoothScroller smoothScroller = new LinearSmoothScroller(getContext()) {
            final float timePerPx = ANIMATION_MS_PER_PX / getResources().getDisplayMetrics().densityDpi;

            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                return layoutManager.computeScrollVectorForPosition(targetPosition);
            }

            @Override
            protected int calculateTimeForScrolling(int dx) {
                return (int) Math.ceil(Math.abs(dx) * timePerPx);
            }
        };
        smoothScroller.setTargetPosition(value);
        layoutManager.startSmoothScroll(smoothScroller);
    }


    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        return false;
    }

    class NumberAdapter extends Adapter<NumberAdapter.ViewHolder> {
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
            TextView text = new TextView(context);
            text.setTextAppearance(context, R.style.AppTheme_Text_BigScore);
            text.setGravity(Gravity.CENTER);
            return new ViewHolder(text);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.display(position);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView text;

            ViewHolder(@NonNull TextView itemText) {
                super(itemText);

                this.text = itemText;
            }

            void display(int position) {
                text.setText(Integer.toString(position));
                text.setTextColor(getColorForPosition(position));
            }
        }
    }
}
