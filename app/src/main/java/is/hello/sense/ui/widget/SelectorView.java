package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Styles;

public class SelectorView extends LinearLayout implements View.OnClickListener {
    public static final int EMPTY_SELECTION = -1;

    private final List<ToggleButton> buttons = new ArrayList<>();
    private int selectedIndex = EMPTY_SELECTION;

    private @Nullable SelectionAwareDrawable selectionAwareBackground;
    private @Nullable OnSelectionChangedListener onSelectionChangedListener;


    //region Lifecycle

    public SelectorView(@NonNull Context context) {
        this(context, null);
    }

    public SelectorView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SelectorView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    //endregion


    //region Backgrounds

    @Override
    public void setBackground(Drawable background) {
        super.setBackground(background);

        if (background instanceof SelectionAwareDrawable) {
            setSelectionAwareDrawable((SelectionAwareDrawable) background);
        } else {
            this.selectionAwareBackground = null;
        }
    }

    public void setSelectionAwareDrawable(@Nullable SelectionAwareDrawable drawable) {
        this.selectionAwareBackground = drawable;
        synchronize();
    }

    //endregion


    //region Hooks

    @Override
    public void addView(@NonNull View child, int index, ViewGroup.LayoutParams params) {
        if (child instanceof ToggleButton) {
            ToggleButton button = (ToggleButton) child;
            int buttonIndex = buttons.size();
            button.setOnClickListener(this);
            button.setTag(R.id.layout_linear_selector_tag_key_index, buttonIndex);
            if (button.isChecked()) {
                this.selectedIndex = buttonIndex;
            }
            buttons.add(button);
        }

        super.addView(child, index, params);
    }

    @Override
    public void onClick(@NonNull View view) {
        this.selectedIndex = (Integer) view.getTag(R.id.layout_linear_selector_tag_key_index);
        synchronize();
        if (getOnSelectionChangedListener() != null) {
            getOnSelectionChangedListener().onSelectionChanged(selectedIndex);
        }
    }

    public void synchronize() {
        for (ToggleButton button : buttons) {
            int index = (Integer) button.getTag(R.id.layout_linear_selector_tag_key_index);
            boolean isSelected = (index == selectedIndex);
            button.setChecked(isSelected);
        }

        if (selectionAwareBackground != null) {
            selectionAwareBackground.setNumberOfItems(buttons.size());
            selectionAwareBackground.setSelectedIndex(selectedIndex);
        }
    }

    //endregion


    //region Properties

    public int getButtonCount() {
        return buttons.size();
    }

    public @NonNull ToggleButton getButtonAt(int index) {
        return buttons.get(index);
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;
        synchronize();
    }

    public @Nullable OnSelectionChangedListener getOnSelectionChangedListener() {
        return onSelectionChangedListener;
    }

    public void setOnSelectionChangedListener(@Nullable OnSelectionChangedListener onSelectionChangedListener) {
        this.onSelectionChangedListener = onSelectionChangedListener;
    }

    //endregion


    //region Button Tags

    public void setButtonTags(Object... tags) {
        if (tags.length != buttons.size()) {
            throw new IllegalArgumentException("Expected " + buttons.size() + " tags, got " + tags.length);
        }

        for (int i = 0, count = tags.length; i < count; i++) {
            buttons.get(i).setTag(R.id.layout_linear_selector_tag_key_user, tags[i]);
        }
    }

    public Object getButtonTagAt(int index) {
        return buttons.get(index).getTag(R.id.layout_linear_selector_tag_key_user);
    }

    //endregion


    //region Options

    public int addOptionButton(@NonNull String title, @Nullable Object tag) {
        Resources resources = getResources();

        ToggleButton optionButton = new ToggleButton(getContext());
        optionButton.setBackgroundResource(R.drawable.selectable_dark_bounded);
        optionButton.setTextAppearance(getContext(), R.style.AppTheme_Text_Body_Medium);
        optionButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelOffset(R.dimen.text_size_body_mid_sized));
        optionButton.setTextColor(resources.getColorStateList(R.color.text_color_selector_toggle_button));
        optionButton.setTextOn(title);
        optionButton.setTextOff(title);
        optionButton.setText(title);
        optionButton.setGravity(Gravity.CENTER);
        optionButton.setTag(R.id.layout_linear_selector_tag_key_user, tag);
        optionButton.setMinimumHeight(resources.getDimensionPixelSize(R.dimen.button_min_size));

        if (getChildCount() > 0) {
            View divider = Styles.createVerticalDivider(getContext(), ViewGroup.LayoutParams.MATCH_PARENT);
            LayoutParams layoutParams = new LayoutParams(divider.getLayoutParams());
            int margin = resources.getDimensionPixelSize(R.dimen.gap_medium);
            layoutParams.setMargins(0, margin, 0, margin);
            addView(divider, layoutParams);
        }

        int index = buttons.size();
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        addView(optionButton, layoutParams);
        return index;
    }

    public void removeAllButtons() {
        removeViews(0, getChildCount());
        buttons.clear();
        this.selectedIndex = EMPTY_SELECTION;
    }

    //endregion


    public interface OnSelectionChangedListener {
        void onSelectionChanged(int newSelectionIndex);
    }

    public static abstract class SelectionAwareDrawable extends Drawable {
        protected int numberOfItems = 0;
        protected int selectedIndex = EMPTY_SELECTION;

        protected boolean isSelectionValid() {
            return (numberOfItems > 0 && selectedIndex != EMPTY_SELECTION);
        }

        public void setNumberOfItems(int numberOfItems) {
            this.numberOfItems = numberOfItems;
            invalidateSelf();
        }

        public void setSelectedIndex(int selectedIndex) {
            this.selectedIndex = selectedIndex;
            invalidateSelf();
        }
    }
}
