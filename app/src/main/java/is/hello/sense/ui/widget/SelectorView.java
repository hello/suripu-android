package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Styles;

public class SelectorView extends LinearLayout implements View.OnClickListener {
    public static final int EMPTY_SELECTION = -1;

    private final List<ToggleButton> buttons = new ArrayList<>();
    private @NonNull LayoutParams buttonLayoutParams;
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

        this.buttonLayoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                                   LayoutParams.WRAP_CONTENT, 1);

        // There is no condition where the user should be allowed
        // to highlight multiple options in a selector at once.
        setMotionEventSplittingEnabled(false);
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
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        for (ToggleButton button : buttons) {
            button.setEnabled(enabled);
        }

        if (selectionAwareBackground != null) {
            selectionAwareBackground.setEnabled(enabled);
        }
    }

    @Override
    public void addView(@NonNull View child, int index, ViewGroup.LayoutParams params) {
        if (child instanceof ToggleButton) {
            ToggleButton button = (ToggleButton) child;
            int buttonIndex = buttons.size();
            button.setEnabled(isEnabled());
            button.setOnClickListener(this);
            button.setTag(R.id.view_selector_tag_key_index, buttonIndex);
            if (button.isChecked()) {
                this.selectedIndex = buttonIndex;
            }
            buttons.add(button);
        }

        super.addView(child, index, params);
    }

    @Override
    public void onClick(@NonNull View view) {
        this.selectedIndex = (Integer) view.getTag(R.id.view_selector_tag_key_index);
        synchronize();
        if (getOnSelectionChangedListener() != null) {
            getOnSelectionChangedListener().onSelectionChanged(selectedIndex);
        }
    }

    public void synchronize() {
        for (ToggleButton button : buttons) {
            int index = (Integer) button.getTag(R.id.view_selector_tag_key_index);
            boolean isSelected = (index == selectedIndex);
            button.setChecked(isSelected);
        }

        if (selectionAwareBackground != null) {
            selectionAwareBackground.setEnabled(isEnabled());
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

    public int indexOfButton(@NonNull ToggleButton button) {
        final Integer index = (Integer) button.getTag(R.id.view_selector_tag_key_index);
        if (index == null) {
            return -1;
        } else {
            return index;
        }
    }

    public void setSelectedButton(@NonNull ToggleButton button) {
        final int index = indexOfButton(button);
        if (index == -1) {
            throw new IllegalArgumentException("Button " + button + " not in selector view");
        }
        setSelectedIndex(index);
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

    public void setButtonLayoutParams(@NonNull LayoutParams buttonLayoutParams) {
        this.buttonLayoutParams = buttonLayoutParams;
    }

    //endregion


    //region Button Tags

    public void setButtonTags(Object... tags) {
        if (tags.length != buttons.size()) {
            throw new IllegalArgumentException("Expected " + buttons.size() +
                                                       " tags, got " + tags.length);
        }

        for (int i = 0, count = tags.length; i < count; i++) {
            buttons.get(i).setTag(R.id.view_selector_tag_key_user, tags[i]);
        }
    }

    public void setButtonTagAt(int index, Object tag) {
        buttons.get(index).setTag(R.id.view_selector_tag_key_user, tag);
    }

    public void setButtonTag(@NonNull ToggleButton button, Object tag) {
        if (indexOfButton(button) == -1) {
            throw new IllegalArgumentException("Button " + button + " not in selector view");
        }
        button.setTag(R.id.view_selector_tag_key_user, tag);
    }

    public Object getButtonTagAt(int index) {
        return buttons.get(index).getTag(R.id.view_selector_tag_key_user);
    }

    public Object getButtonTag(@NonNull ToggleButton button) {
        return button.getTag(R.id.view_selector_tag_key_user);
    }

    public ToggleButton getButtonForTag(Object tag) {
        for (final ToggleButton button : buttons) {
            if (Objects.equals(getButtonTag(button), tag)) {
                return button;
            }
        }

        return null;
    }

    //endregion


    //region Options

    public ToggleButton addOption(@NonNull CharSequence titleOn,
                                  @NonNull CharSequence titleOff,
                                  boolean wantsDivider) {
        final Resources resources = getResources();
        final Context context = getContext();

        // Creating the button with a style does not actually style it,
        // but on Lollipop it removes the intrusive elevation added to
        // all buttons by default.
        final ToggleButton optionButton = new ToggleButton(context, null,
                                                           R.style.AppTheme_Button_ModeSelector);
        optionButton.setBackgroundResource(R.drawable.selectable_dark_bounded);
        optionButton.setTextAppearance(context, R.style.AppTheme_Text_Body);
        optionButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelOffset(R.dimen.text_size_body_mid_sized));
        optionButton.setTextColor(resources.getColorStateList(R.color.text_color_selector_toggle_button));
        optionButton.setTextOn(titleOn);
        optionButton.setTextOff(titleOff);
        optionButton.setText(titleOff);
        optionButton.setGravity(Gravity.CENTER);
        optionButton.setMinimumHeight(resources.getDimensionPixelSize(R.dimen.button_min_size));

        if (getChildCount() > 0 && wantsDivider) {
            final View divider = Styles.createVerticalDivider(context,
                                                              ViewGroup.LayoutParams.MATCH_PARENT);
            final LayoutParams layoutParams = new LayoutParams(divider.getLayoutParams());
            final int margin = resources.getDimensionPixelSize(R.dimen.gap_medium);
            layoutParams.setMargins(0, margin, 0, margin);
            addView(divider, layoutParams);
        }

        addView(optionButton, buttonLayoutParams);
        return optionButton;
    }

    public ToggleButton addOption(@StringRes int titleRes, boolean wantsDivider) {
        final String title = getResources().getString(titleRes);
        return addOption(title, title, wantsDivider);
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
        protected boolean enabled = true;

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

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
            invalidateSelf();
        }
    }
}
