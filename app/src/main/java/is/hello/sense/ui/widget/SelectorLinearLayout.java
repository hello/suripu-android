package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Styles;

public class SelectorLinearLayout extends LinearLayout implements View.OnClickListener {
    public static final int EMPTY_SELECTION = -1;

    private final List<ToggleButton> toggleButtons = new ArrayList<>();
    private int selectedIndex = EMPTY_SELECTION;
    private OnSelectionChangedListener onSelectionChangedListener;
    private ButtonStyler buttonStyler;

    @SuppressWarnings("UnusedDeclaration")
    public SelectorLinearLayout(Context context) {
        super(context);
    }

    @SuppressWarnings("UnusedDeclaration")
    public SelectorLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressWarnings("UnusedDeclaration")
    public SelectorLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void addView(@NonNull View child, int index, ViewGroup.LayoutParams params) {
        if (child instanceof ToggleButton) {
            ToggleButton button = (ToggleButton) child;
            int buttonIndex = toggleButtons.size();
            button.setOnClickListener(this);
            button.setTag(R.id.layout_linear_selector_tag_key_index, buttonIndex);
            if (button.isChecked())
                this.selectedIndex = buttonIndex;
            toggleButtons.add(button);
        }

        super.addView(child, index, params);
    }

    @Override
    public void onClick(@NonNull View view) {
        this.selectedIndex = (Integer) view.getTag(R.id.layout_linear_selector_tag_key_index);
        synchronizeButtonStates();
        if (getOnSelectionChangedListener() != null) {
            getOnSelectionChangedListener().onSelectionChanged(selectedIndex);
        }
    }

    private void synchronizeButtonStates() {
        for (ToggleButton button : toggleButtons) {
            int index = (Integer) button.getTag(R.id.layout_linear_selector_tag_key_index);
            boolean isSelected = (index == selectedIndex);
            if (buttonStyler != null) {
                buttonStyler.styleButton(button, isSelected);
            }
            button.setChecked(isSelected);
        }
    }


    public @NonNull List<ToggleButton> getToggleButtons() {
        return toggleButtons;
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;
        synchronizeButtonStates();
    }

    public OnSelectionChangedListener getOnSelectionChangedListener() {
        return onSelectionChangedListener;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener onSelectionChangedListener) {
        this.onSelectionChangedListener = onSelectionChangedListener;
    }

    public void setButtonTags(Object... tags) {
        if (tags.length != toggleButtons.size()) {
            throw new IllegalArgumentException("Expected " + toggleButtons.size() + " tags, got " + tags.length);
        }

        for (int i = 0, count = tags.length; i < count; i++) {
            toggleButtons.get(i).setTag(R.id.layout_linear_selector_tag_key_user, tags[i]);
        }
    }

    public Object getButtonTag(int index) {
        return toggleButtons.get(index).getTag(R.id.layout_linear_selector_tag_key_user);
    }

    public void setButtonStyler(@Nullable ButtonStyler buttonStyler) {
        this.buttonStyler = buttonStyler;
    }

    public int addOption(@NonNull String title, @Nullable Object tag) {
        Resources resources = getResources();

        ToggleButton optionButton = new ToggleButton(getContext());
        optionButton.setBackgroundResource(R.drawable.selectable_dark);
        optionButton.setTextAppearance(getContext(), R.style.AppTheme_Text_Body_Bold);
        optionButton.setTextColor(resources.getColorStateList(R.color.text_color_selector_toggle_button));
        optionButton.setTextOn(title);
        optionButton.setTextOff(title);
        optionButton.setText(title);
        optionButton.setGravity(Gravity.CENTER);
        optionButton.setTag(R.id.layout_linear_selector_tag_key_user, tag);

        if (getChildCount() > 0) {
            View divider = Styles.createVerticalDivider(getContext(), ViewGroup.LayoutParams.MATCH_PARENT);
            LayoutParams layoutParams = new LayoutParams(divider.getLayoutParams());
            int margin = resources.getDimensionPixelSize(R.dimen.gap_small);
            layoutParams.setMargins(0, margin, 0, margin);
            addView(divider, layoutParams);
        }

        int index = toggleButtons.size();
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, resources.getDimensionPixelSize(R.dimen.button_min_size), 1);
        addView(optionButton, layoutParams);
        return index;
    }

    public void removeAllOptions() {
        removeViews(0, getChildCount());
        toggleButtons.clear();
        this.selectedIndex = EMPTY_SELECTION;
    }


    public interface OnSelectionChangedListener {
        void onSelectionChanged(int newSelectionIndex);
    }

    public static interface ButtonStyler {
        void styleButton(ToggleButton button, boolean checked);
    }
}
