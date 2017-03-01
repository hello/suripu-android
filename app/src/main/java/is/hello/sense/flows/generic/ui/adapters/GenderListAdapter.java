package is.hello.sense.flows.generic.ui.adapters;


import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;

import is.hello.sense.R;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.widget.ImageTextView;

public class GenderListAdapter extends ArrayRecyclerAdapter<String, ArrayRecyclerAdapter.ViewHolder> {
    public GenderListAdapter(@NonNull final Context context) {
        super(new ArrayList<>());
        addAll(Arrays.asList(context.getResources().getStringArray(R.array.genders)));
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent,
                                         final int viewType) {
        return new GenderViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_row_basic_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(final ArrayRecyclerAdapter.ViewHolder holder,
                                 final int position) {
        holder.bind(position);
    }

    private class GenderViewHolder extends ViewHolder {
        private final ImageTextView textView;

        private GenderViewHolder(@NonNull final View itemView) {
            super(itemView);
            this.textView = (ImageTextView) itemView.findViewById(R.id.item_row_basic_list_item_text);
        }

        @Override
        public void bind(final int position) {
            super.bind(position);
            this.textView.setText(getItem(position));
            this.textView.setImageResource(R.drawable.radio_off);
        }
    }
}
