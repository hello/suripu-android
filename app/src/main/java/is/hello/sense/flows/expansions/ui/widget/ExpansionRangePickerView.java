package is.hello.sense.flows.expansions.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Styles;

/**
 * Use to pick min and max values for {@link is.hello.sense.api.model.v2.expansions.ExpansionValueRange}
 * with {@link ExpansionValuePickerView}
 */

public class ExpansionRangePickerView extends LinearLayout {

    private int selectedMaxValue;
    private int selectedMinValue;

    private int min;
    private int max;
    private String symbol;
    private ExpansionValuePickerView minPicker;
    private ExpansionValuePickerView maxPicker;
    private int rangeDifferenceThreshold = 3;

    public ExpansionRangePickerView(final Context context) {
        this(context, null, 0);
    }

    public ExpansionRangePickerView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExpansionRangePickerView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL); // only support horizontal orientation right now
        setGravity(Gravity.CENTER);
        showProgressBar();
    }

    public void init(final int min,
                     final int max,
                     @NonNull final String symbol) {
        this.min = min;
        this.max = max;
        this.symbol = symbol;
        this.rangeDifferenceThreshold = 3;
    }

    public int getSelectedMinValue() {
        return selectedMinValue;
    }

    public int getSelectedMaxValue() {
        return selectedMaxValue;
    }

    /**
     * @param minValue is required
     * @param maxValue is optional
     */
    public void initPickers(@NonNull final Integer minValue,
                            @Nullable final Integer maxValue) {
        final boolean hasMaxPicker = maxValue != null;
        selectedMinValue = minValue;
        selectedMaxValue = hasMaxPicker ? maxValue : minValue;
        final int pickerCount = hasMaxPicker ? 2 : 1;

        post(() -> {
            removeAllViews();
            if (hasMaxPicker) {
                final int dividerWidth = getResources().getDimensionPixelSize(R.dimen.x3);
                final LinearLayout.LayoutParams pickerParams = new LinearLayout.LayoutParams((getMeasuredWidth() / pickerCount) - dividerWidth / 2,
                                                                                             getMeasuredHeight());
                this.minPicker = addPicker(selectedMinValue,
                                           min,
                                           max - rangeDifferenceThreshold,
                                           pickerParams,
                                           this::updateSelectedMinValue
                                          );
                addDivider(dividerWidth);
                this.maxPicker = addPicker(selectedMaxValue,
                                           min + rangeDifferenceThreshold,
                                           max,
                                           pickerParams,
                                           this::updateSelectedMaxValue);
            } else {
                final LinearLayout.LayoutParams pickerParams = new LinearLayout.LayoutParams(getMeasuredWidth() / pickerCount,
                                                                                             getMeasuredHeight());
                addPicker(selectedMinValue,
                          min,
                          max,
                          pickerParams,
                          this::updateSelectedMinAndMaxValue
                         );
            }
        });
    }

    private ExpansionValuePickerView addPicker(final int initialValue,
                                               final int min,
                                               final int max,
                                               @NonNull final LayoutParams layoutParams,
                                               @NonNull final ExpansionValuePickerView.OnValueChangedListener listener) {
        final ExpansionValuePickerView valuePickerView = new ExpansionValuePickerView(getContext());
        valuePickerView.initialize(min,
                                   max,
                                   initialValue,
                                   symbol);

        valuePickerView.setOnValueChangedListener(listener);
        addView(valuePickerView, layoutParams);
        return valuePickerView;
    }

    private void addDivider(final int width) {
        final TextView divider = new TextView(getContext());
        Styles.setTextAppearance(divider, R.style.AppTheme_Text_RotaryPickerItem);
        divider.setText("-");
        divider.setGravity(Gravity.CENTER);
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width,
                                                                               ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        addView(divider, params);
    }

    private void updateSelectedMinAndMaxValue(final int newSelectedValue) {
        updateSelectedMaxValue(newSelectedValue);
        updateSelectedMinValue(newSelectedValue);
    }

    private void updateSelectedMaxValue(final int newMaxValue) {
        selectedMaxValue = newMaxValue;
        updatePickerToEnforceDifference(minPicker,
                                        selectedMaxValue - rangeDifferenceThreshold);

    }

    private void updateSelectedMinValue(final int newMinValue) {
        selectedMinValue = newMinValue;
        updatePickerToEnforceDifference(maxPicker,
                                        selectedMinValue + rangeDifferenceThreshold);

    }

    private void updatePickerToEnforceDifference(@Nullable final ExpansionValuePickerView picker,
                                                 final int validValue) {
        if (picker != null && selectedMaxValue - selectedMinValue < rangeDifferenceThreshold) {
            picker.setSelectedValue(validValue,
                                    true);
        }
    }

    public void showProgressBar() {
        removeAllViews();
        final ProgressBar progressBar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleLarge);
        final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        progressBar.setLayoutParams(lp);
        addView(progressBar);
    }

}