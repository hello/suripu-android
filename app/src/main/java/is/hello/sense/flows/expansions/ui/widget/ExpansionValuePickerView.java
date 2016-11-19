package is.hello.sense.flows.expansions.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;

import is.hello.sense.R;


//todo make common classes shared rather than remaking with RotaryTimePickerView
public class ExpansionValuePickerView extends LinearLayout implements ExpansionRotaryPickerView.OnSelectionListener {
    private int selectedValue;

    private final ExpansionRotaryPickerView pickerView;


    public ExpansionValuePickerView(final Context context) {
        this(context, null);
    }

    public ExpansionValuePickerView(final Context context,
                                    final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExpansionValuePickerView(final Context context,
                                    final AttributeSet attrs,
                                    final int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        final LayoutParams pickerLayoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                                                 LayoutParams.MATCH_PARENT);

        this.pickerView = new ExpansionRotaryPickerView(context);
        this.pickerView.setOnSelectionListener(this);
        this.pickerView.setItemGravity(Gravity.CENTER);
        addView(this.pickerView, pickerLayoutParams);


        if (attrs != null) {
            final TypedArray styles = context.obtainStyledAttributes(attrs, R.styleable.RotaryPickerView,
                                                                     defStyleAttr, 0);

            final Drawable itemBackground = styles.getDrawable(R.styleable.RotaryPickerView_senseItemBackground);
            this.pickerView.setItemBackground(itemBackground);

            styles.recycle();
        }
    }

    @Override
    public void onSelectionRolledOver(@NonNull final ExpansionRotaryPickerView picker,
                                      @NonNull final ExpansionRotaryPickerView.RolloverDirection direction) {

    }

    @Override
    public void onSelectionWillChange() {

    }

    @Override
    public void onSelectionChanged(final int newValue) {
        this.selectedValue = newValue;
    }

    public void initialize(final int min,
                           final int max,
                           final int initialValue,
                           final String symbol) {
        this.pickerView.setMinValue(min);
        this.pickerView.setMaxValue(max);
        this.pickerView.setSymbol(symbol);
        this.pickerView.setValue(initialValue, false);
        this.selectedValue = initialValue;
    }

    public void setSelectedValue(final int value){
        this.pickerView.setValue(value, false);
        this.selectedValue = value;
    }

    public int getSelectedValue() {
        return this.selectedValue;
    }

}
