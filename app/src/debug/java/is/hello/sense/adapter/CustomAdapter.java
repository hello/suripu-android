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


/**
 * todo rename this file and move to correct flow package
 */
public class CustomAdapter extends ArrayRecyclerAdapter<Integer, CustomAdapter.BaseViewHolder> {
    private final LayoutInflater inflater;
    private final String symbol;
    private final int min;
    private final int difference;
    private final int selectedColor;
    private final int normalColor;

    private int selectedPosition = RecyclerView.NO_POSITION;

    /**
     * @param context
     * @param min     min value to display
     * @param max     max value to display
     * @param symbol  symbol to display next to each value.
     */
    public CustomAdapter(@NonNull final Context context,
                         final int min,
                         final int max,
                         final String symbol) {
        super(new ArrayList<>());
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.min = min;
        this.difference = max - min + 1;
        this.symbol = symbol;
        this.selectedColor = ContextCompat.getColor(this.inflater.getContext(), R.color.primary);
        this.normalColor = ContextCompat.getColor(this.inflater.getContext(), R.color.standard);
        notifyDataSetChanged();
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

    public void setSelectedPosition(final int selectedPosition) {
        final int oldPosition = this.selectedPosition;
        this.selectedPosition = selectedPosition;
        notifyItemChanged(oldPosition);
        notifyItemChanged(this.selectedPosition);
    }

    /**
     * This is not the index position. This is the value the user see's
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
