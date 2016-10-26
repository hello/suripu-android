package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import is.hello.sense.adapter.CustomAdapter;
import is.hello.sense.ui.recycler.CustomItemDecoration;

/**
 * Todo rename this to something fitting.
 */
public class CustomView extends RecyclerView {
    private CustomAdapter customAdapter = null;

    public CustomView(final Context context) {
        this(context, null);
    }

    public CustomView(final Context context,
                      final AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public CustomView(final Context context,
                      final AttributeSet attrs,
                      final int defStyle) {
        super(context, attrs, defStyle);
        setLayoutManager(new LinearLayoutManager(context));
        setHasFixedSize(true);
        setItemAnimator(null);
        addItemDecoration(new CustomItemDecoration());
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
        this.customAdapter = new CustomAdapter(getContext(), min, max, symbol);
        setAdapter(customAdapter);
    }

    /**
     * To be called by {@link CustomItemDecoration} for helping the adapter track and change
     * the color of.
     *
     * @param selectedPosition index position in the adapter that should be colored.
     */
    public void setSelectedPosition(final int selectedPosition) {
        if (this.customAdapter != null) {
            customAdapter.setSelectedPosition(selectedPosition);
        }
    }

    /**
     * This is not the index position. This is the value the user see's
     *
     * @return the value the user see's.
     */
    public int getSelectedValue() {
        if (this.customAdapter == null) {
            return RecyclerView.NO_POSITION;
        }
        return customAdapter.getSelectedValue();
    }
}
