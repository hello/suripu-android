package is.hello.sense.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import is.hello.sense.R;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.util.Constants;


/**
 * todo move to correct flow package
 * <p>
 * Responsible for showing a vertical list of values.
 */
public class ExpansionValuePickerAdapter extends ArrayRecyclerAdapter<Integer, ExpansionValuePickerAdapter.BaseViewHolder> {
    private final LayoutInflater inflater;
    private final int selectedColor;
    private final int normalColor;

    private int min;
    private int difference = 0;
    private String symbol = Constants.EMPTY_STRING;
    private int selectedPosition = RecyclerView.NO_POSITION;


    public ExpansionValuePickerAdapter(@NonNull final Context context) {
        super(new ArrayList<>());
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.selectedColor = ContextCompat.getColor(this.inflater.getContext(), R.color.primary);
        this.normalColor = ContextCompat.getColor(this.inflater.getContext(), R.color.standard);
    }

    @Override
    public int getItemCount() {
        return difference;
    }

    @Override
    public Integer getItem(final int position) {
        return (position % difference) + min;
    }

    @Override
    public BaseViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        return new BaseViewHolder(inflater.inflate(R.layout.custom_item, parent, false));
    }

    @Override
    public void onBindViewHolder(final BaseViewHolder holder, final int position) {
        holder.bind(position);
    }

    /**
     * @param min min value to display.
     * @param max max value to display.
     */
    public void setRange(final int min,
                         final int max) {
        this.min = min;
        this.difference = max - min + 1;
        notifyDataSetChanged();
    }

    /**
     * @param symbol symbol to display next to each value.
     */
    public void setSymbol(@NonNull final String symbol) {
        this.symbol = symbol;
        notifyDataSetChanged();
    }

    public void setSelectedPosition(final int selectedPosition) {
        final int oldPosition = this.selectedPosition;
        this.selectedPosition = selectedPosition;
        notifyItemChanged(oldPosition);
        notifyItemChanged(this.selectedPosition);
    }

    /**
     * This is not the index position. This is the value the user see's
     *
     * @return the value the user see's.
     */
    public int getSelectedValue() {
        return getItem(this.selectedPosition);
    }

    public class BaseViewHolder extends ArrayRecyclerAdapter.ViewHolder {
        private final TextView textView;

        public BaseViewHolder(@NonNull final View itemView) {
            super(itemView);
            this.textView = (TextView) itemView.findViewById(R.id.custom_item_text);
        }

        @Override
        public void bind(final int position) {
            super.bind(position);
            if (selectedPosition == position) {
                textView.setTextColor(selectedColor);
            } else {
                textView.setTextColor(normalColor);
            }
            textView.setText(inflater.getContext()
                                     .getResources()
                                     .getString(R.string.custom_adapter_item,
                                                getItem(position), symbol));
        }
    }
}
