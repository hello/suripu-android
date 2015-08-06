package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
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
    private int itemHorizontalPadding;
    private boolean wrapAroundEventsEnabled = true;

    //endregion

    //region Attributes

    private int minValue = 0;
    private int maxValue = 100;
    private int value = 0;
    private boolean wrapsAround = false;
    private int wrapAroundValue;

    private @StyleRes int itemTextAppearance = DEFAULT_ITEM_TEXT_APPEARANCE;
    private @Nullable Drawable itemBackground;
    private boolean wantsLeadingZeros = true;
    private int itemGravity = Gravity.CENTER;

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

        Resources resources = getResources();

        this.layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        this.itemWidth = resources.getDimensionPixelSize(R.dimen.view_rotary_picker_width);
        this.itemHeight = resources.getDimensionPixelSize(R.dimen.view_rotary_picker_height);
        this.itemHorizontalPadding = resources.getDimensionPixelSize(R.dimen.gap_small);

        setHasFixedSize(true);
        setLayoutManager(layoutManager);
        setAdapter(adapter);
        addItemDecoration(new Decoration());
        addOnScrollListener(new ScrollListener());

        setBackgroundColor(Color.WHITE);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setVerticalFadingEdgeEnabled(true);
        setFadingEdgeLength(itemHeight * 2);

        if (attrs != null) {
            TypedArray styles = context.obtainStyledAttributes(attrs, R.styleable.RotaryPickerView, defStyle, 0);

            this.itemTextAppearance = styles.getResourceId(R.styleable.RotaryPickerView_senseTextAppearance, DEFAULT_ITEM_TEXT_APPEARANCE);
            this.itemBackground = styles.getDrawable(R.styleable.RotaryPickerView_senseItemBackground);
            this.wantsLeadingZeros = styles.getBoolean(R.styleable.RotaryPickerView_senseWantsLeadingZeros, true);
            this.itemGravity = styles.getInt(R.styleable.RotaryPickerView_android_gravity, Gravity.CENTER);

            styles.recycle();
        }
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        setMinValue(savedState.minValue);
        setMaxValue(savedState.maxValue);
        setValue(savedState.value, false);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.minValue = minValue;
        savedState.maxValue = maxValue;
        savedState.value = value;
        return savedState;
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
        setWrapAroundValue(maxValue - minValue);
        adapter.notifyDataSetChanged();
    }

    public void setMaxValue(int maxValue) {
        if (maxValue < minValue) {
            throw new IllegalArgumentException("maxValue < minValue");
        }

        this.maxValue = maxValue;
        setWrapAroundValue(maxValue - minValue);
        adapter.notifyDataSetChanged();
    }

    public void setWrapAroundValue(int wrapAroundValue) {
        this.wrapAroundValue = wrapAroundValue;
    }

    public void setValue(int newValue, boolean animate) {
        int constrainedValue = constrainValue(newValue);
        int unfocusedItems = (NUM_VISIBLE_ITEMS / 2);
        int offset = itemHeight * unfocusedItems;
        int position = adapter.getItemPosition(newValue);

        this.wrapAroundEventsEnabled = false;
        if (animate) {
            if (constrainedValue > this.value) {
                smoothScrollToPosition(position + unfocusedItems);
            } else {
                smoothScrollToPosition(Math.max(0, position - unfocusedItems));
            }
        } else {
            layoutManager.scrollToPositionWithOffset(position, offset);
            post(() -> {
                // #scrollToPositionWithOffset will fire one #onScrolled event
                // on the next run-loop cycle when the recycler view re-lays out.
                // This Runnable will execute after that, and everything will work
                // more or less as intended.
                this.wrapAroundEventsEnabled = true;
            });
        }

        this.value = constrainedValue;
    }

    public void increment() {
        int newValue = value + 1;
        if (newValue == maxValue) {
            newValue = minValue;
        }
        setValue(newValue, true);
    }

    public void decrement() {
        int newValue = value - 1;
        if (newValue == minValue) {
            newValue = maxValue;
        }
        setValue(newValue, true);
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

    public void setItemHorizontalPadding(int itemHorizontalPadding) {
        this.itemHorizontalPadding = itemHorizontalPadding;
        adapter.notifyDataSetChanged();
    }

    public void setItemGravity(int itemGravity) {
        this.itemGravity = itemGravity;
        adapter.notifyDataSetChanged();
    }

    public void setWantsLeadingZeros(boolean wantsLeadingZeros) {
        this.wantsLeadingZeros = wantsLeadingZeros;
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

    private boolean isCenterView(View itemView) {
        int containerMidY = (getMeasuredHeight() / 2);
        View centerView = findChildViewUnder(0, containerMidY);
        return (centerView == itemView);
    }

    @Override
    public void onClick(View itemView) {
        if (isCenterView(itemView)) {
            return;
        }

        int position = getChildAdapterPosition(itemView);
        int value = adapter.getItem(position);
        setValue(value, true);
    }

    class ScrollListener extends RecyclerView.OnScrollListener {
        private int previousState = SCROLL_STATE_IDLE;
        private int lastWrapAroundPosition = NO_POSITION;

        private View findCenterView() {
            int containerMidY = (getMeasuredHeight() / 2);
            return findChildViewUnder(0, containerMidY);
        }

        private int getValueForView(@NonNull View view) {
            int adapterPosition = getChildAdapterPosition(view);
            return adapter.getItem(adapterPosition);
        }

        private void updateValueFromView(@NonNull View view) {
            int newValue = getValueForView(view);
            RotaryPickerView.this.value = newValue;
            if (onSelectionListener != null) {
                onSelectionListener.onSelectionChanged(newValue);
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (wrapAroundEventsEnabled && wrapsAround && onSelectionListener != null) {
                View centerView = findCenterView();
                int adapterPosition = getChildAdapterPosition(centerView);
                if (adapterPosition == lastWrapAroundPosition) {
                    return;
                }

                int position = adapterPosition % adapter.getBoundedItemCount();
                if (position == wrapAroundValue) {
                    RolloverDirection direction = dy > 0f
                            ? RolloverDirection.FORWARD
                            : RolloverDirection.BACKWARD;
                    onSelectionListener.onSelectionRolledOver(RotaryPickerView.this, direction);
                    this.lastWrapAroundPosition = adapterPosition;
                } else {
                    this.lastWrapAroundPosition = NO_POSITION;
                }
            }
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (previousState == SCROLL_STATE_IDLE && newState == SCROLL_STATE_DRAGGING) {
                if (onSelectionListener != null) {
                    onSelectionListener.onSelectionWillChange();
                }
            } else if (previousState != SCROLL_STATE_IDLE && newState == SCROLL_STATE_IDLE) {
                View centerView = findCenterView();
                int containerMidY = getMeasuredHeight() / 2;
                int centerViewMidY = (centerView.getTop() + centerView.getBottom()) / 2;
                int distanceToNotch = centerViewMidY - containerMidY;
                if (distanceToNotch == 0) {
                    RotaryPickerView.this.wrapAroundEventsEnabled = true;
                    updateValueFromView(centerView);
                } else {
                    smoothScrollBy(0, distanceToNotch);
                }
            } else if (previousState == SCROLL_STATE_IDLE && newState == SCROLL_STATE_IDLE) {
                View centerView = findCenterView();
                updateValueFromView(centerView);
                RotaryPickerView.this.wrapAroundEventsEnabled = true;
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
                int centerPosition = getItemCount() / 2;
                int centerStartPosition = centerPosition - (centerPosition % getBoundedItemCount());
                position += centerStartPosition;
            }
            return position;
        }

        public String getItemString(int position) {
            if (valueStrings != null) {
                return valueStrings[position];
            } else if (wantsLeadingZeros) {
                return String.format("%02d", getItem(position));
            } else {
                return Integer.toString(getItem(position));
            }
        }

        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = getContext();
            TextView itemView = new TextView(context);
            itemView.setTextAppearance(context, itemTextAppearance);
            itemView.setBackground(itemBackground);
            itemView.setGravity(itemGravity);
            itemView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, itemHeight));
            itemView.setPadding(itemHorizontalPadding, 0, itemHorizontalPadding, 0);
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


    //region Saving

    public static class SavedState extends BaseSavedState {
        private int minValue, maxValue, value;

        public SavedState(Parcel in) {
            super(in);

            this.minValue = in.readInt();
            this.maxValue = in.readInt();
            this.value = in.readInt();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            super.writeToParcel(out, flags);

            out.writeInt(minValue);
            out.writeInt(maxValue);
            out.writeInt(value);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new SavedState.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    //endregion


    //region Interfaces

    public interface OnSelectionListener {
        void onSelectionRolledOver(@NonNull RotaryPickerView picker, @NonNull RolloverDirection direction);
        void onSelectionWillChange();
        void onSelectionChanged(int newValue);
    }

    public enum RolloverDirection {
        FORWARD,
        BACKWARD,
    }

    //endregion
}
