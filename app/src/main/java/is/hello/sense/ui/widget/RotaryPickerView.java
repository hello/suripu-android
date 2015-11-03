package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import is.hello.go99.Anime;
import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.ui.widget.util.Views;

public class RotaryPickerView extends RecyclerView implements View.OnClickListener {
    public static final int DEFAULT_UNFOCUSED_ITEM_COUNT = 2;
    public static final @StyleRes int ITEM_TEXT_APPEARANCE = R.style.AppTheme_Text_RotaryPickerItem;
    public static final @StyleRes int ITEM_TEXT_APPEARANCE_FOCUSED = R.style.AppTheme_Text_RotaryPickerItem_Focused;

    //region Internal

    private final ItemAdapter adapter;
    private final LinearLayoutManager layoutManager;
    private final int glyphWidth, glyphHeight;

    private int itemHorizontalPadding;
    private int itemVerticalPadding;
    private int itemWidth;
    private int itemHeight;

    private int longestValueStringLength = 3;
    private float recyclerMidY = 0;

    private int lastWrapAroundPosition = NO_POSITION;
    private boolean wrapAroundEventsEnabled = true;

    //endregion

    //region Attributes

    private int unfocusedItemCount = DEFAULT_UNFOCUSED_ITEM_COUNT;
    private int visibleItemCount;
    private int rollForwardPosition, rollBackwardPosition;
    private int minValue = 0;
    private int maxValue = 100;
    private int value = 0;
    private boolean wrapsAround = false;

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
        this.itemVerticalPadding = resources.getDimensionPixelSize(R.dimen.gap_medium);
        this.itemHorizontalPadding = resources.getDimensionPixelSize(R.dimen.gap_medium);

        final TextPaint measuringPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        Drawing.updateTextPaintFromStyle(measuringPaint, context, ITEM_TEXT_APPEARANCE_FOCUSED);
        this.glyphWidth = Drawing.getMaximumGlyphWidth(measuringPaint);
        this.glyphHeight = Drawing.getEstimatedTextHeight(measuringPaint);

        this.adapter = new ItemAdapter(resources);

        setHasFixedSize(true);
        setLayoutManager(layoutManager);
        setAdapter(adapter);
        addItemDecoration(new Decoration());
        addOnScrollListener(new ScrollListener());
        setOverScrollMode(OVER_SCROLL_NEVER);

        if (attrs != null) {
            final TypedArray styles = context.obtainStyledAttributes(attrs,
                                                                     R.styleable.RotaryPickerView,
                                                                     defStyle, 0);

            this.itemBackground =
                    styles.getDrawable(R.styleable.RotaryPickerView_senseItemBackground);
            this.wantsLeadingZeros =
                    styles.getBoolean(R.styleable.RotaryPickerView_senseWantsLeadingZeros, true);
            this.itemGravity =
                    styles.getInt(R.styleable.RotaryPickerView_android_gravity, Gravity.CENTER);
            this.unfocusedItemCount =
                    styles.getInt(R.styleable.RotaryPickerView_unfocusedItemCount, DEFAULT_UNFOCUSED_ITEM_COUNT);

            styles.recycle();
        }

        calculateVisibleItemCount();
        calculateRollOverPoints();
        calculateMaximumGlyphCount();
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
    protected int getSuggestedMinimumWidth() {
        return itemWidth;
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        return (itemHeight * visibleItemCount);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        this.itemWidth = (longestValueStringLength * glyphWidth) + (itemHorizontalPadding * 2);
        this.itemHeight = glyphHeight + (itemVerticalPadding * 2);

        final int widthMode = MeasureSpec.getMode(widthSpec);
        final int width = MeasureSpec.getSize(widthSpec);
        final int measuredWidth;
        switch (widthMode) {
            case MeasureSpec.EXACTLY: {
                measuredWidth = width;
                break;
            }
            case MeasureSpec.AT_MOST: {
                measuredWidth = Math.min(width, itemWidth);
                break;
            }
            default: {
                measuredWidth = getSuggestedMinimumWidth();
                break;
            }
        }

        final int heightMode = MeasureSpec.getMode(heightSpec);
        final int height = MeasureSpec.getSize(heightSpec);
        final int measuredHeight;
        switch (heightMode) {
            case MeasureSpec.EXACTLY: {
                measuredHeight = height;
                break;
            }
            case MeasureSpec.AT_MOST: {
                measuredHeight = Math.min(height, itemHeight * visibleItemCount);
                break;
            }
            default: {
                measuredHeight = getSuggestedMinimumHeight();
                break;
            }
        }

        this.recyclerMidY = measuredHeight / 2f;
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
        return super.fling(velocityX / 3, velocityY / 3);
    }

    //endregion


    //region Attributes

    protected void calculateMaximumGlyphCount() {
        if (valueStrings != null) {
            int longestLength = 0;
            for (final String valueString : valueStrings) {
                final int length = valueString.length();
                if (length > longestLength) {
                    longestLength = length;
                }
            }
            this.longestValueStringLength = longestLength;
        } else {
            this.longestValueStringLength = Integer.toString(maxValue, 10).length();
        }
    }

    protected void calculateRollOverPoints() {
        this.rollBackwardPosition = 0;
        this.rollForwardPosition = adapter.getBoundedItemCount() - 1;
    }

    protected void calculateVisibleItemCount() {
        this.visibleItemCount = (unfocusedItemCount * 2) + 1;
    }

    protected int constrainValue(int value) {
        if (value < minValue) {
            return minValue;
        } else if (value > maxValue) {
            return maxValue;
        } else {
            return value;
        }
    }

    public void setUnfocusedItemCount(int unfocusedItemCount) {
        this.unfocusedItemCount = unfocusedItemCount;
        calculateVisibleItemCount();

        requestLayout();
        invalidate();
    }

    public void setRolloverPositions(int backward, int forward) {
        this.rollBackwardPosition = backward;
        this.rollForwardPosition = forward;
    }

    public void setMinValue(int minValue) {
        if (minValue >= maxValue) {
            throw new IllegalArgumentException("minValue >= maxValue");
        }

        this.minValue = minValue;
        calculateRollOverPoints();
        adapter.notifyDataSetChanged();
    }

    public void setMaxValue(int maxValue) {
        if (maxValue < minValue) {
            throw new IllegalArgumentException("maxValue < minValue");
        }

        this.maxValue = maxValue;
        calculateMaximumGlyphCount();
        calculateRollOverPoints();
        adapter.notifyDataSetChanged();
    }

    public void setMagnifyItemsNearCenter(boolean magnifyItemsNearCenter) {
        this.magnifyItemsNearCenter = magnifyItemsNearCenter;
        adapter.notifyDataSetChanged();
    }

    private void scrollToValue(int oldValue, int newValue, boolean animate) {
        final int distanceToFocusedItem = itemHeight * unfocusedItemCount;
        final int position = adapter.getItemPosition(newValue);

        this.wrapAroundEventsEnabled = false;
        if (animate) {
            if (newValue > oldValue) {
                smoothScrollToPosition(position + unfocusedItemCount);
            } else {
                smoothScrollToPosition(Math.max(0, position - unfocusedItemCount));
            }
        } else {
            layoutManager.scrollToPositionWithOffset(position, distanceToFocusedItem);
            post(() -> {
                // #scrollToPositionWithOffset will fire one #onScrolled event
                // on the next run-loop cycle when the recycler view re-lays out.
                // This Runnable will execute after that, and everything will work
                // more or less as intended.
                this.wrapAroundEventsEnabled = true;
            });
        }
    }

    public void setValue(int newValue, boolean animate) {
        final int oldValue = this.value;
        final int constrainedValue = constrainValue(newValue);
        this.value = constrainedValue;

        if (!ViewCompat.isLaidOut(this)) {
            Views.runWhenLaidOut(this, () -> {
                if (itemHeight == 0) {
                    throw new IllegalStateException("itemHeight == 0 after layout");
                }

                scrollToValue(oldValue, constrainedValue, animate);
            });
        } else {
            scrollToValue(oldValue, constrainedValue, animate);
        }
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

        if (wrapAroundEventsEnabled && onSelectionListener != null
                && wrapsAround && newValue == rollForwardPosition) {
            onSelectionListener.onSelectionRolledOver(this, RolloverDirection.FORWARD);

            final View centerView = findChildViewUnder(0, recyclerMidY);
            this.lastWrapAroundPosition = getChildAdapterPosition(centerView);
        }
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
        if (wrapAroundEventsEnabled && onSelectionListener != null
                && wrapsAround && newValue == rollBackwardPosition) {
            onSelectionListener.onSelectionRolledOver(this, RolloverDirection.FORWARD);

            final View centerView = findChildViewUnder(0, recyclerMidY);
            this.lastWrapAroundPosition = getChildAdapterPosition(centerView);
        }
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
        calculateMaximumGlyphCount();
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

                if (dy > 0f) {
                    final int position = adapterPosition % adapter.getBoundedItemCount();
                    if (position == rollForwardPosition) {
                        onSelectionListener.onSelectionRolledOver(RotaryPickerView.this,
                                                                  RolloverDirection.FORWARD);
                        RotaryPickerView.this.lastWrapAroundPosition = adapterPosition;
                        return;
                    }
                } else {
                    final int position = adapterPosition % adapter.getBoundedItemCount();
                    if (position == rollBackwardPosition) {
                        onSelectionListener.onSelectionRolledOver(RotaryPickerView.this,
                                                                  RolloverDirection.BACKWARD);
                        RotaryPickerView.this.lastWrapAroundPosition = adapterPosition;
                        return;
                    }
                }

                RotaryPickerView.this.lastWrapAroundPosition = NO_POSITION;
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
