package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import is.hello.sense.R;

public class ScaleView extends FrameLayout {
    //region Constants

    public static final int VERTICAL = 0;
    public static final int HORIZONTAL = 1;

    private static final int EMPHASIS_STEP = 5;
    private static final float TICK_NORMAL_SCALE = 0.5f;
    private static final float TICK_EMPHASIS_SCALE = 0.25f;

    private static final float ANIMATION_MS_PER_PX = 100f;

    //endregion


    private final Paint linePaint = new Paint();
    private final float diamondHalf;
    private final int segmentSize;
    private int scaleInset;

    private final RecyclerView recyclerView;
    private final LinearLayoutManager layoutManager;
    private final TickAdapter adapter;

    private boolean animating = false;


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

        this.orientation = VERTICAL;
        int initialValue = 0;
        if (attrs != null) {
            initialValue = takeStateFromAttributes(attrs, defStyleAttr);
        }

        this.recyclerView = new RecyclerView(getContext());
        recyclerView.setHasFixedSize(true);
        recyclerView.setOverScrollMode(OVER_SCROLL_NEVER);
        recyclerView.setHorizontalScrollBarEnabled(false);
        recyclerView.setVerticalScrollBarEnabled(false);
        recyclerView.setClipToPadding(false);
        if (orientation == VERTICAL) {
            this.layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, true);
        } else if (orientation == HORIZONTAL) {
            this.layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        } else {
            throw new IllegalStateException();
        }
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                notifyValueChangedListener();
            }
        });

        this.adapter = new TickAdapter();
        recyclerView.setAdapter(adapter);
        addView(recyclerView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        setValue(initialValue, false);
        syncAdapter();


        Resources resources = getResources();
        linePaint.setColor(resources.getColor(R.color.light_accent));
        this.diamondHalf = resources.getDimensionPixelSize(R.dimen.scale_view_diamond) / 2f;
        this.segmentSize = resources.getDimensionPixelSize(R.dimen.scale_view_segment);

        setWillNotDraw(false);
    }

    private int takeStateFromAttributes(@NonNull AttributeSet attrs, int defStyleAttr) {
        TypedArray styles = getContext().obtainStyledAttributes(attrs, R.styleable.ScaleView, defStyleAttr, 0);

        this.orientation = styles.getInteger(R.styleable.ScaleView_scaleOrientation, VERTICAL);
        this.minValue = styles.getInteger(R.styleable.ScaleView_scaleMinValue, 0);
        this.maxValue = styles.getInteger(R.styleable.ScaleView_scaleMaxValue, 100);
        int initialValue = styles.getInteger(R.styleable.ScaleView_scaleValue, 0);

        styles.recycle();

        return initialValue;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state != null && state instanceof Bundle) {
            Bundle savedState = (Bundle) state;

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

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);

        if (orientation == VERTICAL) {
            this.scaleInset = h / 2;
            recyclerView.setPadding(0, scaleInset, 0, scaleInset);
        } else {
            this.scaleInset = w / 2;
            recyclerView.setPadding(scaleInset, 0, scaleInset, 0);
        }
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

    public void onDestroyView() {
        if(onValueChangedListener != null){
            onValueChangedListener = null;
        }
    }

    //endregion


    //region Internal

    protected void syncAdapter() {
        adapter.setItemCount(maxValue - minValue);
    }

    protected void notifyValueChangedListener() {
        if (onValueChangedListener != null) {
            onValueChangedListener.onValueChanged(getValue());
        }
    }

    protected View findCenterTick() {
        if (orientation == VERTICAL) {
            return recyclerView.findChildViewUnder(0, scaleInset);
        } else {
            return recyclerView.findChildViewUnder(scaleInset, 0);
        }
    }

    //endregion


    //region Properties

    public void setMinValue(int minValue) {
        this.minValue = minValue;
        syncAdapter();
    }

    public int getMinValue() {
        return minValue;
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
        syncAdapter();
    }

    public void setValue(int newValue, boolean notifyListener) {
        final int newPosition = (newValue - minValue - 1);
        recyclerView.scrollToPosition(newPosition);
        if (notifyListener) {
            recyclerView.post(this::notifyValueChangedListener);
        }
    }

    public void animateToValue(int newValue) {
        final LinearSmoothScroller smoothScroller = new LinearSmoothScroller(getContext()) {
            final float timePerPx = ANIMATION_MS_PER_PX / getResources().getDisplayMetrics().densityDpi;

            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                return layoutManager.computeScrollVectorForPosition(targetPosition);
            }

            @Override
            protected int calculateTimeForScrolling(int dx) {
                return (int) Math.ceil(Math.abs(dx) * timePerPx);
            }

            @Override
            protected void onStart() {
                super.onStart();

                ScaleView.this.animating = true;
            }

            @Override
            protected void onStop() {
                super.onStop();

                ScaleView.this.animating = false;
            }
        };
        final int newPosition = (newValue - minValue - 1);
        if (newPosition >= 0) {
            smoothScroller.setTargetPosition(newPosition);
            layoutManager.startSmoothScroll(smoothScroller);
        }
    }

    public int getValue() {
        View tick = findCenterTick();
        if (tick == null) {
            return minValue;
        } else {
            return minValue + recyclerView.getChildAdapterPosition(tick);
        }
    }

    public boolean isAnimating() {
        return animating;
    }

    public void setOnValueChangedListener(@Nullable OnValueChangedListener onValueChangedListener) {
        this.onValueChangedListener = onValueChangedListener;
    }

    //endregion


    private class TickAdapter extends RecyclerView.Adapter<TickAdapter.TickViewHolder> {
        private int itemCount = 0;

        public void setItemCount(int itemCount) {
            this.itemCount = itemCount;
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return itemCount;
        }

        @Override
        public TickAdapter.TickViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new TickViewHolder(new Tick(getContext()));
        }

        @Override
        public void onBindViewHolder(TickAdapter.TickViewHolder holder, int position) {
            holder.tick.setEmphasized((position % EMPHASIS_STEP) == 0);
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

        private Tick(@NonNull Context context) {
            super(context);

            Resources resources = context.getResources();
            this.lineSize = resources.getDimensionPixelSize(R.dimen.scale_view_tick);
            this.normalColor = resources.getColor(R.color.view_scale_tick_normal);
            this.emphasizedColor = resources.getColor(R.color.view_scale_tick_emphasized);
        }

        void setEmphasized(boolean emphasized) {
            if (emphasized) {
                linePaint.setColor(emphasizedColor);
                this.extentScale = TICK_EMPHASIS_SCALE;
            } else {
                linePaint.setColor(normalColor);
                this.extentScale = TICK_NORMAL_SCALE;
            }
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float maxX = canvas.getWidth(),
                  maxY = canvas.getHeight();

            if (orientation == VERTICAL) {
                float minX = maxX * extentScale;
                canvas.drawRect(minX, 0, maxX, lineSize, linePaint);
            } else {
                float minY = maxY * extentScale;
                canvas.drawRect(0, minY, lineSize, maxY, linePaint);
            }
        }
    }

    public interface OnValueChangedListener {
        void onValueChanged(int newValue);
    }
}
