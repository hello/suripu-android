package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;

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
            button.setTag(buttonIndex);
            if (button.isChecked())
                this.selectedIndex = buttonIndex;
            toggleButtons.add(button);
        }

        super.addView(child, index, params);
    }

    @Override
    public void onClick(@NonNull View view) {
        this.selectedIndex = (Integer) view.getTag();
        synchronizeButtonStates();
        if (getOnSelectionChangedListener() != null) {
            getOnSelectionChangedListener().onSelectionChanged(selectedIndex);
        }
    }

    private void synchronizeButtonStates() {
        for (ToggleButton button : toggleButtons) {
            int index = (Integer) button.getTag();
            boolean isSelected = (index == selectedIndex);
            if (buttonStyler != null) {
                buttonStyler.styleButton(button, isSelected);
            } else {
                button.setChecked(isSelected);
            }
        }
    }


    public int getSelectedIndex() {
        return selectedIndex;
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

    public void setButtonStyler(ButtonStyler buttonStyler) {
        this.buttonStyler = buttonStyler;
    }


    public interface OnSelectionChangedListener {
        void onSelectionChanged(int newSelectionIndex);
    }

    public static interface ButtonStyler {
        void styleButton(ToggleButton button, boolean checked);
    }
}
