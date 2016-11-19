package is.hello.sense.flows.expansions.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.LinearLayout;

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

    public void initPickers(@NonNull final int[] initialValues){
        selectedMinValue = initialValues[0];
        selectedMaxValue = initialValues.length > 1 ? initialValues[1] : selectedMinValue;

        post( () -> {
            final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(getMeasuredWidth() / initialValues.length,
                                                                                         getMeasuredHeight());

            for (final int initialValue : initialValues) {
                final ExpansionValuePickerView valuePickerView = new ExpansionValuePickerView(getContext());
                valuePickerView.initialize(min,
                                           max,
                                           0,
                                           symbol);

                valuePickerView.setSelectedValue(initialValue);

                addView(valuePickerView, layoutParams);
            }
        });
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