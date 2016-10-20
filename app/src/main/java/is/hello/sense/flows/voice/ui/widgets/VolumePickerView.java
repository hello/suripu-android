package is.hello.sense.flows.voice.ui.widgets;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import is.hello.sense.R;

public class VolumePickerView extends LinearLayout {
    //region Constants

    public static final int VERTICAL = 0;
    public static final int HORIZONTAL = 1;

    private static final int DEFAULT_MIN = 1;
    private static final int DEFAULT_MAX = 11;
    private static final int DEFAULT_INITIAL = 8;

    //endregion

    private final int segmentSize;

    private final TickAdapter adapter;

    private boolean animating = false;


    //region Properties

    private int orientation;
    private int minValue = DEFAULT_MIN;
    private int maxValue = DEFAULT_MAX;
    private int selectedValue = DEFAULT_INITIAL;
    private @Nullable OnValueChangedListener onValueChangedListener;

    //endregion


    //region Lifecycle

    public VolumePickerView(Context context) {
        this(context, null);
    }

    public VolumePickerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VolumePickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        int initialValue = 0;
        this.segmentSize = context.getResources().getDimensionPixelSize(R.dimen.volume_picker_min_size);
        if (attrs != null) {
            initialValue = takeStateFromAttributes(attrs, defStyleAttr);
        }

        setOrientation(orientation);

        this.adapter = new TickAdapter();

        setValue(initialValue, false);
        addTicks();

    }

    private int takeStateFromAttributes(@NonNull final AttributeSet attrs, final int defStyleAttr) {
        final TypedArray styles = getContext().obtainStyledAttributes(attrs, R.styleable.ScaleView, defStyleAttr, 0);

        this.orientation = styles.getInteger(R.styleable.ScaleView_scaleOrientation, HORIZONTAL);
        this.minValue = styles.getInteger(R.styleable.ScaleView_scaleMinValue, DEFAULT_MIN);
        this.maxValue = styles.getInteger(R.styleable.ScaleView_scaleMaxValue, DEFAULT_MAX);
        final int initialValue = styles.getInteger(R.styleable.ScaleView_scaleValue, DEFAULT_INITIAL);

        styles.recycle();

        return initialValue;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            final Bundle savedState = (Bundle) state;

            setMinValue(savedState.getInt("minValue"));
            setMaxValue(savedState.getInt("maxValue"));
            setValue(savedState.getInt("value"), true);

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

    public void onDestroyView() {
        if(onValueChangedListener != null){
            onValueChangedListener = null;
        }
    }

    //endregion


    //region Internal

    protected void addTicks() {
        final int itemCount = maxValue - minValue;

            for (int i = 0; i < itemCount; i++) {
                final int position = i;
                post( () -> {
                    final float scale = 0.6f * (((float) itemCount - position) / itemCount);
                    final Tick tick = new Tick(getContext(), scale);
                    tick.setMinimumHeight(segmentSize);
                    addView(tick,
                        new LinearLayout.LayoutParams(getMeasuredWidth() / itemCount,
                                                      getMeasuredHeight()));
                });
            }

    }

    protected void notifyValueChangedListener() {
        if (onValueChangedListener != null) {
            onValueChangedListener.onValueChanged(getValue());
        }
    }

    //endregion


    //region Properties

    public void setMinValue(final int minValue) {
        this.minValue = minValue;
        addTicks();
    }

    public int getMinValue() {
        return minValue;
    }

    public void setMaxValue(final int maxValue) {
        this.maxValue = maxValue;
        addTicks();
    }

    public void setValue(final int newValue, final boolean notifyListener) {
        final int newPosition = (newValue - minValue - 1);
        if (notifyListener) {
            post(this::notifyValueChangedListener);
        }
    }

    public int getValue() {
        return selectedValue;
    }

    public boolean isAnimating() {
        return animating;
    }

    public void setOnValueChangedListener(@Nullable final OnValueChangedListener onValueChangedListener) {
        this.onValueChangedListener = onValueChangedListener;
    }

    //endregion


    private class TickAdapter extends RecyclerView.Adapter<TickAdapter.TickViewHolder> {
        private int itemCount = 0;

        void setItemCount(final int itemCount) {
            this.itemCount = itemCount;
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return itemCount;
        }

        @Override
        public TickAdapter.TickViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
            final Tick tick = new Tick(getContext(), 1f);
            tick.setLayoutParams(new RecyclerView.LayoutParams(parent.getMeasuredWidth() / getItemCount(),
                                                               parent.getMeasuredHeight()));
            final TickViewHolder viewHolder = new TickViewHolder(tick);
            viewHolder.itemView.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(MotionEvent.ACTION_DOWN == event.getAction()){
                        viewHolder.tick.setEmphasized(true);
                        selectedValue = viewHolder.getAdapterPosition();
                    }
                    return false;
                }
            });

            return viewHolder;
        }

        @Override
        public void onBindViewHolder(final TickAdapter.TickViewHolder holder, final int position) {
            final int itemCount = getItemCount();
            final float scale = 0.4f + 0.6f * (((float) itemCount - position) / itemCount);
            holder.tick.setExtentScale(scale);
            if (orientation == VERTICAL) {
                holder.tick.setMinimumHeight(segmentSize);
            } else {
                holder.tick.setMinimumWidth(segmentSize);
            }
        }

        class TickViewHolder extends RecyclerView.ViewHolder {
            final Tick tick;

            TickViewHolder(@NonNull Tick itemView) {
                super(itemView);

                this.tick = itemView;
            }
        }
    }

    private class Tick extends View {
        private final Paint linePaint = new Paint();
        private final int lineSize;
        private final int normalColor;
        private final int emphasizedColor;

        private float extentScale;

        private Tick(@NonNull final Context context, final float scale) {
            this(context,
                 R.dimen.scale_view_tick,
                 R.color.gray3,
                 R.color.blue6,
                 scale);
        }

        private Tick(@NonNull final Context context,
                     @DimenRes final int lineSizeRes,
                     @ColorRes final int normalColorRes,
                     @ColorRes final int emphasizedColorRes,
                     final float scale){
            super(context);

            final Resources resources = context.getResources();
            this.lineSize = resources.getDimensionPixelSize(lineSizeRes);
            this.normalColor = ContextCompat.getColor(context, normalColorRes);
            this.emphasizedColor = ContextCompat.getColor(context, emphasizedColorRes);
            this.extentScale = scale;
        }

        public void setExtentScale(final float extentScale){
            this.extentScale = extentScale;
            invalidate();
        }

        protected void setEmphasized(final boolean emphasized) {
            if (emphasized) {
                linePaint.setColor(emphasizedColor);
            } else {
                linePaint.setColor(normalColor);
            }
            invalidate();
        }

        @Override
        protected void onDraw(final Canvas canvas) {
            final float maxX = canvas.getWidth(),
                  maxY = canvas.getHeight();

            if (orientation == VERTICAL) {
                final float minX = maxX * extentScale;
                canvas.drawRect(minX, 0, maxX, lineSize, linePaint);
            } else {
                final float minY = maxY * extentScale;
                canvas.drawRect(0, minY, lineSize, maxY, linePaint);
            }
        }
    }

    public interface OnValueChangedListener {
        void onValueChanged(int newValue);
    }
}
