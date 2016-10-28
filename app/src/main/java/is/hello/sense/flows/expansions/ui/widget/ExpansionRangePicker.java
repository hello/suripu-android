package is.hello.sense.flows.expansions.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import is.hello.sense.api.model.v2.expansions.ExpansionValueRange;
import is.hello.sense.ui.widget.ExpansionValuePickerView;

/**
 * Use to pick min and max values for {@link is.hello.sense.api.model.v2.expansions.ExpansionValueRange}
 * with {@link is.hello.sense.ui.widget.ExpansionValuePickerView}
 */

public class ExpansionRangePicker extends LinearLayout{

    private int selectedMaxValue;
    private int selectedMinValue;

    public ExpansionRangePicker(final Context context) {
        this(context, null, 0);
    }

    public ExpansionRangePicker(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExpansionRangePicker(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL); //todo only support horizontal orientation
    }

    public void initPickers(@NonNull final ExpansionValueRange valueRange,
                            @NonNull final String symbol,
                            @NonNull final int[] initialValues){

        post( () -> {
            final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(getMeasuredWidth() / initialValues.length,
                                                                                         getMeasuredHeight());

            for (final int initialValue : initialValues) {
                final ExpansionValuePickerView valuePickerView = new ExpansionValuePickerView(getContext());
                valuePickerView.setNestedScrollingEnabled(true);
                valuePickerView.initialize(valueRange.min, valueRange.max, symbol);
                //todo scroll to proper initial value because the default scrollToPosition function will scroll to top of position not center which is what we want.
                valuePickerView.scrollToPosition(initialValue - valueRange.min);
                addView(valuePickerView, layoutParams);
            }
        });
    }

    public int getSelectedMinValue() {
        return selectedMinValue;
    }

    public int getSelectedMaxValue() {
        return selectedMaxValue;
    }
}
