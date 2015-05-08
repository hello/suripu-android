package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import is.hello.sense.R;

public class RotaryPickerView extends RecyclerView implements View.OnClickListener {
    public static final int NUM_VISIBLE_ITEMS = 5;
    public static final @StyleRes int DEFAULT_ITEM_TEXT_APPEARANCE = R.style.AppTheme_Text_RotaryPickerItem;

    //region Internal

    private final ItemAdapter adapter = new ItemAdapter();
    private final LinearLayoutManager layoutManager;

    private final int itemWidth;
    private final int itemHeight;

    //endregion

    //region Attributes

    private int minValue = 0;
    private int maxValue = 100;
    private int value = 0;
    private boolean wrapsAround = false;

    private @StyleRes int itemTextAppearance = DEFAULT_ITEM_TEXT_APPEARANCE;
    private @Nullable Drawable itemBackground;

    private @Nullable String[] valueStrings;
    private @Nullable OnSelectionListener onSelectionListener;

    //endregion


    //region Lifecycle

    public RotaryPickerView(@NonNull Context context) {
        this(context, null);
    }

    public RotaryPickerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RotaryPickerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        this.layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        this.itemWidth = getResources().getDimensionPixelSize(R.dimen.view_rotary_picker_width);
        this.itemHeight = getResources().getDimensionPixelSize(R.dimen.view_rotary_picker_height);

        setHasFixedSize(true);
        setLayoutManager(layoutManager);
        setAdapter(adapter);
        addItemDecoration(new Decoration());
        setOnScrollListener(new ScrollListener());

        setBackgroundColor(Color.WHITE);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setVerticalFadingEdgeEnabled(true);
        setFadingEdgeLength(itemHeight * 2);

        if (attrs != null) {
            TypedArray styles = context.obtainStyledAttributes(attrs, R.styleable.RotaryPickerView, defStyle, 0);

            this.itemTextAppearance = styles.getResourceId(R.styleable.RotaryPickerView_senseTextAppearance, DEFAULT_ITEM_TEXT_APPEARANCE);
            this.itemBackground = styles.getDrawable(R.styleable.RotaryPickerView_senseItemBackground);

            styles.recycle();
        }
    }

    //endregion


    //region Measurement

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        final int widthMode = MeasureSpec.getMode(widthSpec);
        final int heightMode = MeasureSpec.getMode(heightSpec);
        final int widthSize = MeasureSpec.getSize(widthSpec);
        final int heightSize = MeasureSpec.getSize(heightSpec);

        int width;
        switch (widthMode) {
            case MeasureSpec.EXACTLY: {
                width = widthSize;
                break;
            }
            case MeasureSpec.AT_MOST: {
                width = Math.min(widthSize, itemWidth);
                break;
            }

            case MeasureSpec.UNSPECIFIED:
            default: {
                width = getSuggestedMinimumWidth();
                break;
            }
        }

        int height;
        switch (heightMode) {
            case MeasureSpec.EXACTLY: {
                height = heightSize;
                break;
            }

            case MeasureSpec.AT_MOST: {
                height = Math.min(heightSize, itemHeight * NUM_VISIBLE_ITEMS);
                break;
            }

            case MeasureSpec.UNSPECIFIED:
            default: {
                height = getSuggestedMinimumHeight();
                break;
            }
        }

        setMeasuredDimension(width, height);
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
        return super.fling(velocityX / 4, velocityY / 4);
    }

    //endregion


    //region Attributes

    protected int constrainValue(int value) {
        if (value < minValue) {
            return minValue;
        } else if (value > maxValue) {
            return maxValue;
        } else {
            return value;
        }
    }

    public void setMinValue(int minValue) {
        if (minValue >= maxValue) {
            throw new IllegalArgumentException("minValue >= maxValue");
        }

        this.minValue = minValue;
        adapter.notifyDataSetChanged();
    }

    public void setMaxValue(int maxValue) {
        if (maxValue < minValue) {
            throw new IllegalArgumentException("maxValue < minValue");
        }

        this.maxValue = maxValue;
        adapter.notifyDataSetChanged();
    }

    public void setValue(int newValue, boolean animate) {
        int constrainedValue = constrainValue(newValue);
        if (constrainedValue == this.value) {
            return;
        }

        int unfocusedItems = (NUM_VISIBLE_ITEMS / 2);
        int offset = itemHeight * unfocusedItems;
        int position = adapter.getItemPosition(newValue);
        if (animate) {
            if (constrainedValue > this.value) {
                smoothScrollToPosition(position + unfocusedItems);
            } else {
                smoothScrollToPosition(position - unfocusedItems);
            }
        } else {
            layoutManager.scrollToPositionWithOffset(position, offset);
        }

        this.value = constrainedValue;
    }

    public int getValue() {
        return value;
    }

    public void setItemTextAppearance(@StyleRes int itemTextAppearance) {
        this.itemTextAppearance = itemTextAppearance;
        adapter.notifyDataSetChanged();
    }

    public void setItemBackground(@Nullable Drawable itemBackground) {
        this.itemBackground = itemBackground;
        adapter.notifyDataSetChanged();
    }

    public void setWrapsAround(boolean wrapsAround) {
        this.wrapsAround = wrapsAround;
        adapter.notifyDataSetChanged();

        if (wrapsAround) {
            setValue(adapter.getItemCount() / 2, false);
        }
    }

    public void setValueStrings(@Nullable String[] valueStrings) {
        this.valueStrings = valueStrings;
        adapter.notifyDataSetChanged();
    }

    public void setOnSelectionListener(@Nullable OnSelectionListener onSelectionListener) {
        this.onSelectionListener = onSelectionListener;
    }

    //endregion


    //region Data


    @Override
    public void onClick(View itemView) {
        int position = getChildAdapterPosition(itemView);
        int value = adapter.getItem(position);
        setValue(value, true);

        if (onSelectionListener != null) {
            onSelectionListener.onSelectionChanged(this, value);
        }
    }

    class ScrollListener extends RecyclerView.OnScrollListener {
        private int previousState = RecyclerView.SCROLL_STATE_IDLE;

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (previousState == SCROLL_STATE_IDLE && newState == SCROLL_STATE_DRAGGING) {
                if (onSelectionListener != null) {
                    onSelectionListener.onSelectionWillChange(RotaryPickerView.this);
                }
            } else if (previousState != SCROLL_STATE_IDLE && newState == SCROLL_STATE_IDLE) {
                int containerMidY = recyclerView.getMeasuredHeight() / 2;
                View centerView = recyclerView.findChildViewUnder(0, containerMidY);
                int centerViewMidY = (centerView.getTop() + centerView.getBottom()) / 2;
                int distanceToNotch = centerViewMidY - containerMidY;
                if (distanceToNotch == 0) {
                    int adapterPosition = recyclerView.getChildAdapterPosition(centerView);
                    int newValue = adapter.getItem(adapterPosition);
                    RotaryPickerView.this.value = newValue;
                    if (onSelectionListener != null) {
                        onSelectionListener.onSelectionChanged(RotaryPickerView.this, newValue);
                    }
                } else {
                    recyclerView.smoothScrollBy(0, distanceToNotch);
                }
            }

            this.previousState = newState;
        }
    }

    class Decoration extends RecyclerView.ItemDecoration {
        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, State state) {
            int position = parent.getChildAdapterPosition(view);
            if (position == 0) {
                outRect.top += (parent.getMeasuredHeight() / 2) - (itemHeight / 2);
            }
            if (position == adapter.getItemCount() - 1) {
                outRect.bottom += (parent.getMeasuredHeight() / 2) - (itemHeight / 2);
            }
        }
    }

    class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {
        public int getBoundedItemCount() {
            return (maxValue - minValue) + 1;
        }

        @Override
        public int getItemCount() {
            if (wrapsAround) {
                return Integer.MAX_VALUE;
            } else {
                return getBoundedItemCount();
            }
        }

        public int getItem(int position) {
            return minValue + (position % getBoundedItemCount());
        }

        public int getItemPosition(int value) {
            int position = value - minValue;
            if (wrapsAround) {
                int count = getItemCount() / 2;
                int centerStartPosition = count - (count % getBoundedItemCount());
                position += centerStartPosition;
            }
            return position;
        }

        public String getItemString(int position) {
            if (valueStrings != null) {
                return valueStrings[position];
            } else {
                return String.format("%02d", getItem(position));
            }
        }

        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = getContext();
            TextView itemView = new TextView(context);
            itemView.setTextAppearance(context, itemTextAppearance);
            itemView.setBackground(itemBackground);
            itemView.setGravity(Gravity.CENTER);
            itemView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, itemHeight));
            itemView.setOnClickListener(RotaryPickerView.this);
            return new ItemViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(ItemViewHolder holder, int position) {
            holder.bind(getItemString(position));
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {
            final TextView text;

            ItemViewHolder(@NonNull TextView itemView) {
                super(itemView);

                this.text = itemView;
            }

            void bind(@NonNull String value) {
                text.setText(value);
            }
        }
    }

    //endregion


    //region Interfaces

    public interface OnSelectionListener {
        void onSelectionWillChange(@NonNull RotaryPickerView pickerView);
        void onSelectionChanged(@NonNull RotaryPickerView pickerView, int newValue);
    }

    //endregion
}
