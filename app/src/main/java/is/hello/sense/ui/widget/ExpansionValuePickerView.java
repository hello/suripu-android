package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import is.hello.sense.adapter.ExpansionValuePickerAdapter;
import is.hello.sense.ui.recycler.ExpansionValuePickerItemDecoration;

/**
 * A custom RecyclerView that will use {@link ExpansionValuePickerAdapter}
 */
public class ExpansionValuePickerView extends RecyclerView {
    private final ExpansionValuePickerAdapter expansionValuePickerAdapter = new ExpansionValuePickerAdapter(getContext());

    public ExpansionValuePickerView(final Context context) {
        this(context, null);
    }

    public ExpansionValuePickerView(final Context context,
                                    final AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public ExpansionValuePickerView(final Context context,
                                    final AttributeSet attrs,
                                    final int defStyle) {
        super(context, attrs, defStyle);
        setLayoutManager(new LinearLayoutManager(context));
        setHasFixedSize(true);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setItemAnimator(null);
        addItemDecoration(new ExpansionValuePickerItemDecoration());
    }

    /**
     * Will create an adapter for this view using the parameters.
     *
     * @param min    min value to display.
     * @param max    max value to display.
     * @param symbol symbol to display next to each value.
     */
    public void initialize(final int min,
                           final int max,
                           @NonNull final String symbol) {
        this.expansionValuePickerAdapter.setRange(min, max);
        this.expansionValuePickerAdapter.setSymbol(symbol);
        setAdapter(expansionValuePickerAdapter);
    }

    /**
     * To be called by {@link ExpansionValuePickerItemDecoration} for helping the adapter track and change
     * the color of.
     *
     * @param selectedPosition index position in the adapter that should be colored.
     */
    public void setSelectedPosition(final int selectedPosition) {
        if (this.expansionValuePickerAdapter != null) {
            expansionValuePickerAdapter.setSelectedPosition(selectedPosition);
        }
    }

    /**
     * This is not the index position. This is the value the user see's
     *
     * @return the value the user see's.
     */
    public int getSelectedValue() {
        if (this.expansionValuePickerAdapter == null) {
            return RecyclerView.NO_POSITION;
        }
        return expansionValuePickerAdapter.getSelectedValue();
    }
}
