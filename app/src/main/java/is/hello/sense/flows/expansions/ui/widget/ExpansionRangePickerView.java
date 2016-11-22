package is.hello.sense.flows.expansions.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Styles;

/**
 * Use to pick min and max values for {@link is.hello.sense.api.model.v2.expansions.ExpansionValueRange}
 * with {@link ExpansionValuePickerView}
 */

public class ExpansionRangePickerView extends LinearLayout{

    private int selectedMaxValue;
    private int selectedMinValue;

    private int min;
    private int max;
    private String symbol;

    public ExpansionRangePickerView(final Context context) {
        this(context, null, 0);
    }

    public ExpansionRangePickerView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExpansionRangePickerView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL); // only support horizontal orientation right now
    }

    public void init(final int min,
                     final int max,
                     @NonNull final String symbol){
        this.min = min;
        this.max = max;
        this.symbol = symbol;
    }

    /**
     * @param minValue is required
     * @param maxValue is optional
     */
    public void initPickers(@NonNull final Integer minValue,
                            @Nullable final Integer maxValue){
        final boolean hasMaxPicker = maxValue != null;
        selectedMinValue = minValue;
        selectedMaxValue = hasMaxPicker ? maxValue : minValue;
        final int pickerCount = hasMaxPicker ? 2 : 1;

        post( () -> {
            if(hasMaxPicker){
                final int dividerWidth = getResources().getDimensionPixelSize(R.dimen.x3);
                final LinearLayout.LayoutParams pickerParams = new LinearLayout.LayoutParams((getMeasuredWidth() / pickerCount) - dividerWidth/2,
                                                                                             getMeasuredHeight());
                addPicker(selectedMinValue,
                          pickerParams,
                          this::updateSelectedMinValue
                         );
                addDivider(dividerWidth);
                addPicker(selectedMaxValue,
                          pickerParams,
                          this::updateSelectedMaxValue);
            } else {
                final LinearLayout.LayoutParams pickerParams = new LinearLayout.LayoutParams(getMeasuredWidth() / pickerCount,
                                                                                             getMeasuredHeight());
                addPicker(selectedMinValue,
                          pickerParams,
                          this::updateSelectedMinAndMaxValue
                         );
            }
        });
    }

    private void addPicker(final int initialValue,
                           @NonNull final LayoutParams layoutParams,
                           @NonNull final ExpansionValuePickerView.OnValueChangedListener listener){
        final ExpansionValuePickerView valuePickerView = new ExpansionValuePickerView(getContext());
        valuePickerView.initialize(min,
                                   max,
                                   0,
                                   symbol);

        valuePickerView.setOnValueChangedListener(listener);

        valuePickerView.setSelectedValue(initialValue);

        addView(valuePickerView, layoutParams);
    }

    private void addDivider(final int width){
        final TextView divider = new TextView(getContext());
        Styles.setTextAppearance(divider, R.style.AppTheme_Text_RotaryPickerItem);
        divider.setText("-");
        divider.setGravity(Gravity.CENTER);
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width,
                                                                               ViewGroup.LayoutParams.WRAP_CONTENT
                                                                               );
        params.gravity = Gravity.CENTER;
        addView(divider, params);
    }

    private void updateSelectedMinAndMaxValue(final int newSelectedValue) {
        updateSelectedMaxValue(newSelectedValue);
        updateSelectedMinValue(newSelectedValue);
    }

    private void updateSelectedMaxValue(final int newMaxValue) {
        selectedMaxValue = newMaxValue;

        //todo prevent max from being less than min
    }

    private void updateSelectedMinValue(final int newMinValue) {
        selectedMinValue = newMinValue;
        //todo if min >= max need to force other value picker to scroll
    }

    public int getSelectedMinValue() {
        return selectedMinValue;
    }

    public int getSelectedMaxValue() {
        return selectedMaxValue;
    }
}