package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
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

import is.hello.go99.Anime;
import is.hello.sense.R;

public class RotaryPickerView extends RecyclerView implements View.OnClickListener {
    public static final int NUM_VISIBLE_ITEMS = 5;
    public static final @StyleRes int ITEM_TEXT_APPEARANCE = R.style.AppTheme_Text_RotaryPickerItem;
    public static final @StyleRes int ITEM_TEXT_APPEARANCE_FOCUSED = R.style.AppTheme_Text_RotaryPickerItem_Focused;

    //region Internal

    private final ItemAdapter adapter;
    private final LinearLayoutManager layoutManager;

    private final int itemWidth;
    private final int itemHeight;
    private float recyclerMidY = 0;
    private int itemHorizontalPadding;
    private boolean wrapAroundEventsEnabled = true;

    //endregion

    //region Attributes

    private int minValue = 0;
    private int maxValue = 100;
    private int value = 0;
    private boolean wrapsAround = false;
    private int wrapAroundValue;

    private @Nullable Drawable itemBackground;
    private boolean wantsLeadingZeros = true;
    private int itemGravity = Gravity.CENTER;
    private boolean magnifyItemsNearCenter = true;

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

        final Resources resources = getResources();

        this.layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        this.itemWidth = resources.getDimensionPixelSize(R.dimen.view_rotary_picker_width);
        this.itemHeight = resources.getDimensionPixelSize(R.dimen.view_rotary_picker_item_height);
        this.itemHorizontalPadding = resources.getDimensionPixelSize(R.dimen.gap_small);

        this.adapter = new ItemAdapter(resources);

        setHasFixedSize(true);
        setLayoutManager(layoutManager);
        setAdapter(adapter);
        addItemDecoration(new Decoration());
        addOnScrollListener(new ScrollListener());
        setOverScrollMode(OVER_SCROLL_NEVER);

        if (attrs != null) {
            final TypedArray styles = context.obtainStyledAttributes(attrs, R.styleable.RotaryPickerView, defStyle, 0);

            this.itemBackground = styles.getDrawable(R.styleable.RotaryPickerView_senseItemBackground);
            this.wantsLeadingZeros = styles.getBoolean(R.styleable.RotaryPickerView_senseWantsLeadingZeros, true);
            this.itemGravity = styles.getInt(R.styleable.RotaryPickerView_android_gravity, Gravity.CENTER);

            styles.recycle();
        }
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        final SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        setMinValue(savedState.minValue);
        setMaxValue(savedState.maxValue);
        setValue(savedState.value, false);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final SavedState savedState = new SavedState(super.onSaveInstanceState());
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

        this.recyclerMidY = height / 2f;
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

    public void setMagnifyItemsNearCenter(boolean magnifyItemsNearCenter) {
        this.magnifyItemsNearCenter = magnifyItemsNearCenter;
        adapter.notifyDataSetChanged();
    }

    public void setValue(int newValue, boolean animate) {
        final int constrainedValue = constrainValue(newValue);
        final int unfocusedItems = (NUM_VISIBLE_ITEMS / 2);
        final int offset = itemHeight * unfocusedItems;
        final int position = adapter.getItemPosition(newValue);

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
        int newValue = constrainValue(value + 1);
        if (newValue == maxValue && !wrapsAround) {
            return;
        } else {
            newValue = minValue;
        }

        smoothScrollBy(0, itemHeight);
        this.value = newValue;
    }

    public void decrement() {
        int newValue = constrainValue(value - 1);
        if (newValue == minValue && !wrapsAround) {
            return;
        } else {
            newValue = maxValue;
        }

        smoothScrollBy(0, -itemHeight);
        this.value = newValue;
    }

    public int getValue() {
        return value;
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
        final View centerView = findChildViewUnder(0, recyclerMidY);
        return (centerView == itemView);
    }

    @Override
    public void onClick(View itemView) {
        if (isCenterView(itemView)) {
            return;
        }

        final int position = getChildAdapterPosition(itemView);
        final int value = adapter.getItem(position);
        setValue(value, true);
    }

    class ScrollListener extends RecyclerView.OnScrollListener {
        private int previousState = SCROLL_STATE_IDLE;
        private int lastWrapAroundPosition = NO_POSITION;
        private @Nullable View centerView;

        private int getValueForView(@NonNull View view) {
            final int adapterPosition = getChildAdapterPosition(view);
            return adapter.getItem(adapterPosition);
        }

        private void updateValueFromView(@NonNull View view) {
            final int newValue = getValueForView(view);
            RotaryPickerView.this.value = newValue;
            if (onSelectionListener != null) {
                onSelectionListener.onSelectionChanged(newValue);
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            for (int i = recyclerView.getChildCount() - 1; i >= 0; i--) {
                final View view = recyclerView.getChildAt(i);
                final float top = view.getTop() + view.getTranslationY();
                final float bottom = view.getBottom() + view.getTranslationY();
                if (recyclerMidY >= top && recyclerMidY <= bottom) {
                    centerView = view;
                }

                final RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(view);
                final ItemAdapter.ItemViewHolder eventViewHolder = (ItemAdapter.ItemViewHolder) viewHolder;
                final float childCenter = (top + bottom) / 2f;
                final float centerDistance = Math.abs((childCenter - recyclerMidY) / recyclerMidY);
                eventViewHolder.setDistanceToCenter(centerDistance);
            }

            if (wrapAroundEventsEnabled && wrapsAround && onSelectionListener != null) {
                final int adapterPosition = getChildAdapterPosition(centerView);
                if (adapterPosition == lastWrapAroundPosition) {
                    return;
                }

                final int position = adapterPosition % adapter.getBoundedItemCount();
                if (position == wrapAroundValue) {
                    final RolloverDirection direction = dy > 0f
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
            } else if (centerView != null) {
                if (previousState != SCROLL_STATE_IDLE && newState == SCROLL_STATE_IDLE) {
                    final int centerViewMidY = (centerView.getTop() + centerView.getBottom()) / 2;
                    final int distanceToNotch = Math.round(centerViewMidY - recyclerMidY);
                    if (distanceToNotch == 0) {
                        RotaryPickerView.this.wrapAroundEventsEnabled = true;
                        updateValueFromView(centerView);
                    } else {
                        smoothScrollBy(0, distanceToNotch);
                    }
                } else if (previousState == SCROLL_STATE_IDLE && newState == SCROLL_STATE_IDLE) {
                    updateValueFromView(centerView);
                    RotaryPickerView.this.wrapAroundEventsEnabled = true;
                }
            }

            this.previousState = newState;
        }
    }

    class Decoration extends RecyclerView.ItemDecoration {
        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, State state) {
            final int position = parent.getChildAdapterPosition(view);
            if (position == 0) {
                outRect.top += recyclerMidY - (itemHeight / 2);
            }
            if (position == adapter.getItemCount() - 1) {
                outRect.bottom += recyclerMidY - (itemHeight / 2);
            }
        }
    }

    class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {
        final float UNFOCUSED_SCALE = 0.625f, FOCUSED_SCALE = 1f;
        final @ColorInt int focusedColor, unfocusedColor, almostGoneColor;

        ItemAdapter(@NonNull Resources resources) {
            this.focusedColor = resources.getColor(R.color.text_rotary_picker_focused);
            this.unfocusedColor = resources.getColor(R.color.text_rotary_picker_unfocused);
            this.almostGoneColor = resources.getColor(R.color.text_rotary_picker_almost_gone);
        }

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
                final int centerPosition = getItemCount() / 2;
                final int centerStartPosition = centerPosition - (centerPosition % getBoundedItemCount());
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
            final Context context = getContext();
            final TextView itemView = new TextView(context);
            itemView.setTextAppearance(context, ITEM_TEXT_APPEARANCE);
            itemView.setBackground(itemBackground);
            itemView.setGravity(itemGravity);
            itemView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, itemHeight));
            itemView.setPadding(itemHorizontalPadding, 0, itemHorizontalPadding, 0);
            itemView.setIncludeFontPadding(false);
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

            void setDistanceToCenter(float distanceToCenter) {
                final int color;
                final float scale;
                if (distanceToCenter <= 0.5f) {
                    final float fraction = distanceToCenter * 2.0f;
                    color = Anime.interpolateColors(fraction, focusedColor, unfocusedColor);
                    if (magnifyItemsNearCenter) {
                        scale = Anime.interpolateFloats(fraction, FOCUSED_SCALE, UNFOCUSED_SCALE);
                    } else {
                        scale = UNFOCUSED_SCALE;
                    }
                } else {
                    final float fraction = (distanceToCenter - 0.5f) / 0.5f;
                    color = Anime.interpolateColors(fraction, unfocusedColor, almostGoneColor);
                    scale = UNFOCUSED_SCALE;
                }
                text.setTextColor(color);

                // Actually changing the text size has horrible performance
                // on some devices, so we do a cheap layer scale instead.
                text.setScaleX(scale);
                text.setScaleY(scale);
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
